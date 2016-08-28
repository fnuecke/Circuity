package li.cil.circuity.common.bus;

import com.google.common.base.Throwables;
import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusSegment;
import li.cil.circuity.api.bus.ConfigurableBusController;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusStateAware;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.InterruptList;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.util.RangeMap;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Base implementation for a bus controller.
 * <p>
 * This implements a bus with an address bus width of 16 and a data bus width of 8.
 * <p>
 * Subclasses are required to provide the following functionality:
 * <ul>
 * <li>
 * implement {@link #getBusWorld()}, which is used when scheduling scans.
 * </li>
 * <li>
 * call {@link #scheduleScan()} when added to the world and when the list of
 * neighboring bus segments changed.
 * </li>
 * <li>
 * call {@link #clear()} when they get disposed/removed from the world.
 * </li>
 * </ul>
 * <p>
 * Ports:
 * <table>
 * <tr><td>0</td><td>API version of the selected device<sup>1</sup>. Read-only.</td></tr>
 * <tr><td>1</td><td>Number of mapped devices. Read-only.</td></tr>
 * <tr><td>2</td><td>Selected device. Read-write.</td></tr>
 * <tr><td>3</td><td>Type identifier of the device. Read-only.</td></tr>
 * <tr><td>4</td><td>Read a single word of the address the selected device is mapped to (low to high). Writing resets the shift.</td></tr>
 * <tr><td>5</td><td>Read a single word of the size of the selected device (low to high). Writing resets the shift.</td></tr>
 * <tr><td>6</td><td>Read a single character of the name of the selected device. Call until it returns zero to read the full name. Writing resets the offset.</td></tr>
 * </table>
 * <p>
 * <sup>1)</sup> This is guaranteed to be the first port. The initially selected device is guaranteed to be the bus controller itself. This way software may query the bus controller's API version before having to actually use the API.
 */
public abstract class AbstractBusController extends AbstractAddressable implements ConfigurableBusController, AddressHint, BusStateAware {
    /**
     * The number of addressable words via this buses address space.
     */
    public static final long ADDRESS_COUNT = 0x100000L;

    /**
     * The interval in which to re-scan the bus in case multiple controllers
     * were detected or the scan hit the end of the loaded world, in seconds.
     */
    private static final int RESCAN_INTERVAL = 5;

    /**
     * Version of the controller's serial interface API version.
     */
    private static final int API_VERSION = 1;

    /**
     * Address block representing the full address space of the bus.
     */
    private static final AddressBlock FULL_ADDRESS_BLOCK = new AddressBlock(0, ADDRESS_COUNT);

    /**
     * General device information about this bus controller.
     */
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.BUS_CONTROLLER, Constants.DeviceInfo.BUS_CONTROLLER_NAME);

    // --------------------------------------------------------------------- //

    /**
     * Possible states the bus controller may be in after a scan.
     */
    public enum State {
        /**
         * All is well, controller is operating normally.
         */
        READY,

        /**
         * A scan is currently pending.
         */
        SCANNING,

        /**
         * State entered when multiple bus controllers were present.
         */
        ERROR_MULTIPLE_BUS_CONTROLLERS,

        /**
         * State entered when some address blocks overlapped.
         */
        ERROR_ADDRESSES_OVERLAP,

        /**
         * State entered when the scan could not be completed due to a segment
         * failing to return its adjacent devices. Typically this will be due
         * to an adjacent block being in an unloaded chunk, but it may be used
         * to emulate failing hardware in the future.
         */
        ERROR_SEGMENT_FAILED
    }

    // --------------------------------------------------------------------- //

    /**
     * Used for synchronized access to the bus, in particular for read/writes
     * and rescanning/changing addresses/interrupts of devices.
     */
    private final Object lock = new Object();

    /**
     * List of all currently known bus devices on the bus.
     * <p>
     * This is updated after a scan triggered by {@link #scheduleScan()} has
     * completed. It will then contain the list of all connected devices.
     */
    private final Set<BusDevice> devices = new HashSet<>();

    /**
     * The list of state aware bus devices, i.e. device that are notified when
     * the bus is powered on / off.
     */
    private final List<BusStateAware> stateAwares = new ArrayList<>();

    /**
     * The list of bus devices that also implement {@link ITickable}.
     * <p>
     * These will be updated by the bus controller's worker thread whenever the
     * bus controller is updated (which really should be every tick).
     */
    private final List<AsyncTickable> tickables = new ArrayList<>();

    /**
     * List of all <em>addressable</em> devices.
     * <p>
     * This is a subset of {@link #devices}, for performance and stable per-
     * index lookup in the serial API.
     */
    private final List<Addressable> addressables = new ArrayList<>();

    /**
     * Mapping of addresses to devices.
     */
    private final RangeMap<Addressable> addresses = new RangeMap<>(ADDRESS_COUNT);

    /**
     * Mapping of addressable devices to the address block they're assigned to.
     * <p>
     * This will always contain all connected addressable devices, but may
     * contain overlapping address blocks. In that case, the addressing is in
     * an invalid state, and no device will be bound to any address. Only when
     * all conflicts are resolved (typically by user interaction) will the
     * address lookup table be filled.
     */
    private final Map<Addressable, AddressBlock> addressBlocks = new HashMap<>();

    /**
     * Comparator used to keep the {@link #addressables} list sorted.
     * <p>
     * This is done to ensure a deterministic order across save and load, and
     * to make it easier to check for intersecting address blocks, as well as
     * to find gaps for newly assigned address blocks. Also has the nice
     * side-effect of a less terrible worst-case remove time.
     */
    private final Comparator<Addressable> addressComparator = Comparator.comparingLong(addressable -> addressBlocks.get(addressable).getOffset());

    /**
     * The list of occupied interrupt source IDs. These IDs get assigned to
     * {@link InterruptSource}s as they are connected to the bus.
     */
    private final BitSet interruptSourceIds = new BitSet();

    /**
     * The list of occupied interrupt sink IDs. These IDs get assigned to
     * {@link InterruptSink}s as they are connected to the bus.
     */
    private final BitSet interruptSinkIds = new BitSet();

    /**
     * The mapping of interrupt source ID to actual interrupt sink. As there
     * may be gaps in the IDs, this may contain <code>null</code> entries.
     */
    private final List<InterruptSource> interruptSources = new ArrayList<>();

    /**
     * The mapping of interrupt sink ID to actual interrupt sink. As there
     * may be gaps in the IDs, this may contain <code>null</code> entries.
     */
    private final List<InterruptSink> interruptSinks = new ArrayList<>();

    /**
     * The mapping of source to sink interrupt IDs. A value of <code>-1</code>
     * means that there is no mapping for that source interrupt ID. This array
     * will be grown as necessary.
     */
    private int[] interruptMap = new int[0];

    /**
     * Whether we have a scan scheduled already (avoid multiple scans).
     */
    private ScheduledCallback scheduledScan;

    /**
     * The current controller state, based on the last scan.
     */
    private State state = State.SCANNING;

    /**
     * Set if we currently have a worker thread running.
     */
    private Future currentUpdate;

    /**
     * The currently set word size of the bus.
     * <p>
     * This is the width of the data bus.
     */
    @Serialize
    private byte wordSize = 8;

    /**
     * Whether the bus is currently powered. Stored to notify newly connected
     * devices.
     */
    @Serialize
    private boolean isOnline;

    /**
     * Currently selected device for reading its address via serial interface.
     */
    @Serialize
    private int selected;

    /**
     * Currently index in the name of the selected device for serial interface.
     */
    @Serialize
    private int nameIndex;

    /**
     * Current index/shift of the address of the selected device for serial interface.
     */
    @Serialize
    private int addressShift;

    /**
     * Current index/shift of the size of the selected device for serial interface.
     */
    @Serialize
    private int sizeShift;

    // --------------------------------------------------------------------- //
    // AbstractAddressable

    @Override
    protected AddressBlock validateAddress(final AddressBlock memory) {
        return memory.take(Constants.BUS_CONTROLLER_ADDRESS, 7);
    }

    // --------------------------------------------------------------------- //
    // Addressable

    @Nullable
    @Override
    public DeviceInfo getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public int read(final int address) {
        switch (address) {
            case 0: // Version of this serial API.
                return API_VERSION;
            case 1: // Number of addressable devices.
                return addressables.size();
            case 2: // Selected device.
                return selected;
            case 3: { // Type identifier of selected addressable device.
                if (selected < 0 || selected >= addressables.size()) {
                    return 0;
                } else {
                    final Addressable device = addressables.get(selected);
                    final DeviceInfo info = device.getDeviceInfo();
                    return info != null ? info.type.id : 0;
                }
            }
            case 4: { // Read a byte of the address the device is mapped to.
                if (selected < 0 || selected >= addressables.size()) {
                    return 0;
                } else {
                    final Addressable device = addressables.get(selected);
                    final AddressBlock memory = addressBlocks.get(device);
                    return (int) ((memory.getOffset() >>> (addressShift++ * getWordSize())) & getWordMask());
                }
            }
            case 5: { // Read a byte of the size of the device.
                if (selected < 0 || selected >= addressables.size()) {
                    return 0;
                } else {
                    final Addressable device = addressables.get(selected);
                    final AddressBlock memory = addressBlocks.get(device);
                    return (int) ((memory.getLength() >>> (sizeShift++ * getWordSize())) & getWordMask());
                }
            }
            case 6: { // Read a single character of the name of the selected device.
                if (selected < 0 || selected >= addressables.size()) {
                    return 0;
                } else {
                    final Addressable device = addressables.get(selected);
                    final DeviceInfo info = device.getDeviceInfo();
                    final String name = info != null ? info.name : null;
                    return name != null && nameIndex < name.length() ? (name.charAt(nameIndex++) & getWordMask()) : 0;
                }
            }
        }
        return 0;
    }

    @Override
    public void write(final int address, final int value) {
        switch (address) {
            case 2: // Select device.
                selected = value;
                addressShift = 0;
                sizeShift = 0;
                nameIndex = 0;
                break;
            case 4: // Reset address shift.
                addressShift = 0;
                break;
            case 5: // Reset size shift.
                sizeShift = 0;
                break;
            case 6: // Reset device name pointer.
                nameIndex = 0;
                break;
        }
    }

    // --------------------------------------------------------------------- //
    // AddressHint

    @Override
    public int getSortHint() {
        return Constants.BUS_CONTROLLER_ADDRESS;
    }

    // --------------------------------------------------------------------- //
    // BusStateAware

    @Override
    public void handleBusOnline() {
    }

    @Override
    public void handleBusOffline() {
        selected = 0;
    }

    // --------------------------------------------------------------------- //
    // BusController

    @Override
    public boolean isOnline() {
        return isOnline && state == State.READY;
    }

    @Override
    public void scheduleScan() {
        final World world = getBusWorld();
        if (world.isRemote) return;
        synchronized (lock) {
            if (scheduledScan == null) {
                scheduledScan = SillyBeeAPI.scheduler.schedule(world, this::scanSynchronized);
                state = State.SCANNING;
            }
        }
    }

    @Override
    public int getWordSize() {
        return wordSize;
    }

    @Override
    public int getWordMask() {
        return 0xFFFFFFFF >>> (32 - wordSize);
    }

    @Nullable
    @Override
    public AddressBlock getAddress(final Addressable device) {
        return addressBlocks.get(device);
    }

    @Override
    public void mapAndWrite(final long address, final int value) {
        final Addressable device = addresses.get(address);
        if (device != null) {
            final AddressBlock memory = addressBlocks.get(device);
            final int mappedAddress = (int) (address - memory.getOffset());
            device.write(mappedAddress, value);
        } else {
            segfault();
        }
    }

    @Override
    public int mapAndRead(final long address) {
        final Addressable device = addresses.get(address);
        if (device != null) {
            final AddressBlock memory = addressBlocks.get(device);
            final int mappedAddress = (int) (address - memory.getOffset());
            return device.read(mappedAddress);
        } else {
            segfault();
            return 0;
        }
    }

    @Override
    public PrimitiveIterator.OfInt getInterruptSourceIds(final InterruptSource device) {
        return new InterruptIterator<>(interruptSources, device);
    }

    @Override
    public PrimitiveIterator.OfInt getInterruptSinkIds(final InterruptSink device) {
        return new InterruptIterator<>(interruptSinks, device);
    }

    @Override
    public void interrupt(final int interruptId, final int data) {
        synchronized (lock) {
            if (!isOnline()) return;
            final int interruptSinkId = interruptMap[interruptId];
            if (interruptSinkId >= 0) {
                final InterruptSink sink = interruptSinks.get(interruptSinkId);
                assert sink != null : "BusController is in an invalid state: mapping to InterruptSink ID with missing InterruptSink instance.";
                sink.interrupt(interruptSinkId, data);
            }
        }
    }

    // --------------------------------------------------------------------- //
    // ConfigurableBusController

    @Override
    public void setDeviceAddress(final Addressable device, final AddressBlock address) {
        synchronized (lock) {
//            addressBlocks.put(device, address);
//            scheduleScan();
        }
    }

    @Override
    public void setInterruptMapping(final int sourceId, final int sinkId) {
        synchronized (lock) {
            interruptMap[sourceId] = sinkId;
        }
    }


    // --------------------------------------------------------------------- //

    /**
     * Get the controller's current state.
     *
     * @return the current state.
     */
    public State getState() {
        return state;
    }

    /**
     * Set the data bus width.
     * <p>
     * Note that changing this value completely resets the bus controller.
     * <p>
     * This method is thread safe.
     *
     * @param value the new data bus width.
     */
    public void setWordSize(final byte value) {
        synchronized (lock) {
            this.wordSize = value;
            clear();
            scheduleScan();
        }
    }

    /**
     * Set the buses power state.
     * <p>
     * Notifies attached state aware devices if the state changes.
     * <p>
     * This method is <em>not</em> thread safe.
     *
     * @param value the new power state. It is expected to be called
     *              from the server thread only, before/unless {@link #startUpdate()}
     *              is called (i.e. while no worker thread processes the bus tick).
     */
    public void setOnline(final boolean value) {
        if (value == isOnline) {
            return;
        }

        isOnline = value;
        if (isOnline) {
            stateAwares.forEach(BusStateAware::handleBusOnline);
        } else {
            stateAwares.forEach(BusStateAware::handleBusOffline);
        }
    }

    /**
     * Starts an update cycle.
     * <p>
     * This will trigger a worker thread which will then update all
     * {@link li.cil.circuity.api.bus.device.AsyncTickable} bus devices, if
     * and only if the bus controller is in a legal state (i.e. no two bus
     * controllers connected to the same bus).
     * <p>
     * This will do nothing if the bus is currently in an errored state and
     * unless it is in an online state.
     * <p>
     * This method is <em>not</em> thread safe. It is expected to be called
     * from the server thread only.
     *
     * @see State#getState()
     * @see State#setOnline(boolean)
     * @see State#isOnline()
     */
    public void startUpdate() {
        if (currentUpdate == null && isOnline()) {
            currentUpdate = BusThreadPool.INSTANCE.submit(this::updateDevicesAsync);
        }
    }

    /**
     * Finish an update cycle.
     * <p>
     * Waits for the worker thread to complete updating all devices on the bus.
     * This way, even though updates are running in parallel, they are still
     * kept in sync with the server update loop, which is particularly useful
     * to avoid non-deterministic behavior when saving.
     * <p>
     * This method is <em>not</em> thread safe. It is expected to be called
     * from the server thread only.
     */
    public void finishUpdate() {
        if (currentUpdate != null) {
            try {
                currentUpdate.get();
            } catch (InterruptedException | ExecutionException e) {
                Throwables.propagate(e);
            } finally {
                currentUpdate = null;
            }
        }
    }

    /**
     * Clears the bus controller's state, removing all devices (and setting
     * their bus controller to <code>null</code>).
     * <p>
     * This method is thread safe.
     */
    public void clear() {
        // Needs to be synchronized as it may be called when owner is disposed,
        // which may happen during a tick, i.e. while async update is running.
        synchronized (lock) {
            try {
                if (!doAnyAddressesOverlap()) {
                    addresses.clear();
                    for (final Addressable addressable : addressables) {
                        addressable.setMemory(null);
                    }
                }

                for (final InterruptSink sink : interruptSinks) {
                    if (sink != null) {
                        sink.setAcceptedInterrupts(null);
                    }
                }

                for (final InterruptSource source : interruptSources) {
                    if (source != null) {
                        source.setEmittedInterrupts(null);
                    }
                }

                for (final BusDevice device : devices) {
                    device.setBusController(null);
                }
            } finally {
                devices.clear();
                stateAwares.clear();
                tickables.clear();
                addressables.clear();
                addressBlocks.clear();
                interruptSourceIds.clear();
                interruptSinkIds.clear();
                interruptSources.clear();
                interruptSinks.clear();

                Arrays.fill(interruptMap, -1);

                if (scheduledScan != null) {
                    SillyBeeAPI.scheduler.cancel(getBusWorld(), scheduledScan);
                    scheduledScan = null;
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

    protected abstract World getBusWorld();

    // --------------------------------------------------------------------- //

    private void updateDevicesAsync() {
        synchronized (lock) {
            // A rescan might have snuck in or the owner may have been disposed
            // between this was scheduled and before the worker thread started.
            if (!isOnline()) return;
            tickables.forEach(AsyncTickable::updateAsync);
        }
    }

    private void segfault() {
        // TODO Interrupt?
    }

    // Avoids one level of indentation in scan.
    private void scanSynchronized() {
        synchronized (lock) {
            scheduledScan = null;
            scan();
        }
    }

    private void scan() {
        // ----------------------------------------------------------------- //
        // Build new list of devices --------------------------------------- //
        // ----------------------------------------------------------------- //

        final Set<BusDevice> newDevices = new HashSet<>();

        {
            final List<BusDevice> adjacentDevices = new ArrayList<>();
            final Set<BusSegment> closed = new HashSet<>();
            final Queue<BusSegment> open = new ArrayDeque<>();

            // Avoid null entries in iterables returned by getDevices() to screw
            // things up. Not that anyone should ever do that, but I don't trust
            // people not to screw this up, so we're playing it safe.
            closed.add(null);

            // Start at the bus controller. This is why the BusController
            // interface extends the BusSegment interface; homogenizes things.
            open.add(this);

            // Explore the graph implicitly defined by bus segments' getDevices()
            // return values (which are, essentially, the edges in the graph) in
            // a breadth-first fashion, adding already explored segments to the
            // closed set to avoid infinite loops due to cycles in the graph.
            while (!open.isEmpty()) {
                final BusSegment segment = open.poll();
                if (!closed.add(segment)) continue;
                if (!segment.getDevices(adjacentDevices)) {
                    scanErrored(State.ERROR_SEGMENT_FAILED);
                    return;
                }
                for (final BusDevice device : adjacentDevices) {
                    newDevices.add(device);
                    if (device instanceof BusSegment) {
                        open.add((BusSegment) device);
                    }
                }
                adjacentDevices.clear();
            }

            // Similarly as with the above, avoid null entries in getDevices()
            // to screw things up. Still not trusting people. Who'd've thunk.
            newDevices.remove(null);
        }

        // ----------------------------------------------------------------- //
        // Handle removed and added devices -------------------------------- //
        // ----------------------------------------------------------------- //

        // Remember if we had a valid mapping before adding the new devices.
        final boolean didAnyAddressesOverlap = doAnyAddressesOverlap();

        {
            // For tracking the list of removed sinks so we only need to go
            // through the interrupt map once at the end.
            final BitSet removedSinkIds = new BitSet();

            // Find devices that have been removed, update internal data
            // structures accordingly and notify them. While doing so, convert
            // the set of found devices into the set of added devices.
            final Iterator<BusDevice> it = devices.iterator();
            while (it.hasNext()) {
                final BusDevice device = it.next();

                // If the device is in the list of found devices, it is still
                // known and nothing changes for the device. We remove it so
                // that this set of devices only contains the added devices
                // when we're done. If it isn't in the list, then, well, it
                // is gone, and we have to update our internal data.
                if (!newDevices.remove(device)) {
                    it.remove();

                    if (device instanceof BusStateAware) {
                        stateAwares.remove(device);
                    }

                    if (device instanceof AsyncTickable) {
                        tickables.remove(device);
                    }

                    if (device instanceof Addressable) {
                        final Addressable addressable = (Addressable) device;

                        // Binary search can find any one of a list of equal
                        // address offsets, so refine the search if necessary
                        // by searching left and right starting from the seed
                        // index we get from the binary search.
                        int index = Collections.binarySearch(addressables, addressable, addressComparator);
                        if (addressables.get(index) != addressable) {
                            boolean searchingLeft = true, searchingRight = true;
                            for (int i = 1; searchingLeft || searchingRight; i++) {
                                if (searchingLeft) {
                                    final int j = index - i;
                                    if (j >= 0) {
                                        if (addressables.get(j) == addressable) {
                                            index = j;
                                            break;
                                        }
                                    } else {
                                        searchingLeft = false;
                                    }
                                }

                                if (searchingRight) {
                                    final int j = index + i;
                                    if (j < addressables.size()) {
                                        if (addressables.get(j) == addressable) {
                                            index = j;
                                            break;
                                        }
                                    } else {
                                        searchingRight = false;
                                    }
                                }
                            }
                        }
                        addressables.remove(index);

                        final AddressBlock memory = addressBlocks.remove(addressable);

                        if (!didAnyAddressesOverlap) {
                            setAddressMap(memory, null);
                            addressable.setMemory(null);
                        }
                    }

                    if (device instanceof InterruptSource) {
                        final InterruptSource source = (InterruptSource) device;

                        final PrimitiveIterator.OfInt sourceIds = getInterruptSourceIds(source);
                        while (sourceIds.hasNext()) {
                            final int id = sourceIds.nextInt();
                            interruptMap[id] = -1;
                            interruptSources.set(id, null);
                            interruptSourceIds.clear(id);
                        }

                        source.setEmittedInterrupts(null);
                    }

                    if (device instanceof InterruptSink) {
                        final InterruptSink sink = (InterruptSink) device;

                        final PrimitiveIterator.OfInt sinkIds = getInterruptSinkIds(sink);
                        while (sinkIds.hasNext()) {
                            final int id = sinkIds.nextInt();
                            removedSinkIds.set(id);
                            interruptSinks.set(id, null);
                            interruptSinkIds.clear(id);
                        }

                        sink.setAcceptedInterrupts(null);
                    }

                    device.setBusController(null);
                }
            }

            // Scan through the interrupt map once to remove all references to
            // interrupt sink ids of sinks that have been removed.
            for (int i = 0; i < interruptMap.length; i++) {
                final int sinkId = interruptMap[i];
                if (sinkId >= 0 && removedSinkIds.get(sinkId)) {
                    interruptMap[i] = -1;
                }
            }
        }

        // Multiple controllers on one bus are a no-go. If we detect we're
        // connected to another controller, shut down everything and start
        // rescanning periodically.
        final boolean hasMultipleControllers = newDevices.stream().anyMatch(device -> device instanceof BusController && device != this);
        if (hasMultipleControllers) {
            scanErrored(State.ERROR_MULTIPLE_BUS_CONTROLLERS);
            return;
        }

        // The above leaves us with the list of added devices, update internal
        // data structures accordingly and notify them.
        for (final BusDevice device : newDevices) {
            devices.add(device);

            device.setBusController(this);

            if (device instanceof BusStateAware) {
                stateAwares.add((BusStateAware) device);
            }

            if (device instanceof AsyncTickable) {
                tickables.add((AsyncTickable) device);
            }

            if (device instanceof Addressable) {
                final Addressable addressable = (Addressable) device;
                setAddress(addressable, getFreeAddress(addressable));
            }

            if (device instanceof InterruptSource) {
                final InterruptSource source = (InterruptSource) device;

                final int[] ids = source.getEmittedInterrupts(computeInterruptList(interruptSourceIds));
                if (validateInterruptIds(ids, interruptSourceIds, interruptSources, source)) {
                    applyInterruptIds(ids, interruptSourceIds, interruptSources, source);
                    source.setEmittedInterrupts(ids);
                } else {
                    ModCircuity.getLogger().warn("InterruptSource wants to use an interrupt ID that is invalid or already in use or provided duplicate IDs. This indicates an incorrect implementation in '{}'.", device.getClass().getName());
                }
            }

            if (device instanceof InterruptSink) {
                final InterruptSink sink = (InterruptSink) device;

                final int[] ids = sink.getAcceptedInterrupts(computeInterruptList(interruptSinkIds));
                if (validateInterruptIds(ids, interruptSinkIds, interruptSinks, sink)) {
                    applyInterruptIds(ids, interruptSinkIds, interruptSinks, sink);
                    sink.setAcceptedInterrupts(ids);
                } else {
                    ModCircuity.getLogger().warn("InterruptSink wants to use an interrupt ID that is invalid or already in use or provided duplicate IDs. This indicates an incorrect implementation in '{}'.", device.getClass().getName());
                }
            }
        }

        // Ensure capacity of interrupt map is sufficiently large if new sources were added.
        if (interruptSourceIds.length() > interruptMap.length) {
            final int oldLength = interruptMap.length;
            interruptMap = Arrays.copyOf(interruptMap, interruptSourceIds.length());
            Arrays.fill(interruptMap, oldLength, interruptMap.length, -1);
        }

        // ----------------------------------------------------------------- //
        // Handle state changes due to added/removed devices --------------- //
        // ----------------------------------------------------------------- //

        final boolean doAnyAddressesOverlap = doAnyAddressesOverlap();
        if (!doAnyAddressesOverlap) {
            // If we were in an invalid state before we need to activate all
            // addressable devices known, which now includes the new devices.
            // Otherwise we just need to activate the new devices.
            final Iterable<BusDevice> activatedDevices = didAnyAddressesOverlap ? devices : newDevices;
            for (final BusDevice device : activatedDevices) {
                if (device instanceof Addressable) {
                    final Addressable addressable = (Addressable) device;
                    final AddressBlock memory = addressBlocks.get(addressable);
                    setAddressMap(memory, addressable);
                    addressable.setMemory(memory);
                }
            }
        } else if (!didAnyAddressesOverlap) {
            addresses.clear();
            for (final Addressable addressable : addressables) {
                if (!newDevices.contains(addressable)) {
                    addressable.setMemory(null);
                }
            }
        }

        // Try remapping *all* devices if we have overlaps, trying to resolve
        // the overlap (can solve the overlap if a device was removed, opening
        // a slot for a previously overlapping device).
        if (doAnyAddressesOverlap) {
            // Clear to allow assigning everything to anything.
            final List<Addressable> unaddressed = new ArrayList<>(addressables);
            addressables.clear();
            addressBlocks.clear();

            // Look for devices with sort hints, add them in the order given.
            unaddressed.stream().
                    filter(a -> a instanceof AddressHint).
                    sorted(AbstractBusController::compareAddressHints).
                    forEach(addressable -> setAddress(addressable, getFreeAddress(addressable)));

            // Then add the remaining devices.
            for (final Addressable addressable : unaddressed) {
                if (addressable instanceof AddressHint) continue;
                setAddress(addressable, getFreeAddress(addressable));
            }

            // If we could resolve the overlap, notify all addressable devices
            // of their new addresses.
            if (!doAnyAddressesOverlap()) {
                for (final Addressable addressable : addressables) {
                    final AddressBlock memory = addressBlocks.get(addressable);
                    setAddressMap(memory, addressable);
                    addressable.setMemory(memory);
                }
            } else {
                scanErrored(State.ERROR_ADDRESSES_OVERLAP);
                return;
            }
        }

        state = State.READY;
    }

    private void scanErrored(final State state) {
        scheduledScan = SillyBeeAPI.scheduler.scheduleIn(getBusWorld(), RESCAN_INTERVAL * Constants.TICKS_PER_SECOND, this::scanSynchronized);
        this.state = state;
    }

    private boolean doAnyAddressesOverlap() {
        for (int i = 1; i < addressables.size(); i++) {
            final Addressable addressable1 = addressables.get(i - 1);
            final Addressable addressable2 = addressables.get(i);

            final AddressBlock memory1 = addressBlocks.get(addressable1);
            final AddressBlock memory2 = addressBlocks.get(addressable2);

            final long end1 = memory1.getOffset() - ADDRESS_COUNT + memory1.getLength();
            if (end1 > 0) {
                return true;
            }

            if (end1 > memory2.getOffset() - ADDRESS_COUNT) {
                return true;
            }
        }
        return false;
    }

    private void setAddress(final Addressable addressable, final AddressBlock memory) {
        addressBlocks.put(addressable, memory);

        final int index = Collections.binarySearch(addressables, addressable, addressComparator);
        addressables.add(index >= 0 ? index : ~index, addressable);
    }

    private void setAddressMap(final AddressBlock memory, @Nullable final Addressable addressable) {
        addresses.remove(memory.getOffset());
        if (addressable != null) {
            addresses.add(addressable, memory.getOffset(), memory.getLength());
        }
    }

    private AddressBlock getFreeAddress(final Addressable newAddressable) {
        // Addressable devices are ordered by address, find gaps, specifically
        // find a gap that's large enough to fit the specified device.
        long address = 0;
        for (final Addressable addressable : addressables) {
            final AddressBlock memory = addressBlocks.get(addressable);

            // A device may have no assigned memory assigned, yet, if this is
            // called while remapping all devices on the bus.
            if (memory == null) continue;

            final int available = (int) (Math.min(memory.getOffset() - address, Integer.MAX_VALUE));
            if (available > 0) {
                final AddressBlock candidate = new AddressBlock(address, available);
                final AddressBlock requested = newAddressable.getMemory(candidate);
                if (requested.getOffset() >= candidate.getOffset() && requested.getOffset() + requested.getLength() <= candidate.getOffset() + candidate.getLength()) {
                    return requested;
                }
            }

            // Don't allocate outside of our addressable range.
            if (memory.getOffset() + memory.getLength() >= FULL_ADDRESS_BLOCK.getLength()) {
                break;
            }

            // In case of already overlapping device addresses, avoid going
            // back in address space as that might give us false positives.
            address = Math.max(address, memory.getOffset() + memory.getLength());
        }

        // Either we failed to find a gap, or we're at the empty space after the
        // last currently mapped addressable device. Use that space, even if it
        // means overlap.
        final int available = (int) (Math.min(FULL_ADDRESS_BLOCK.getLength() - address, Integer.MAX_VALUE));
        return newAddressable.getMemory(new AddressBlock(address, available));
    }

    private static int compareAddressHints(final Addressable a1, final Addressable a2) {
        final AddressHint ah1 = (AddressHint) a1;
        final AddressHint ah2 = (AddressHint) a2;
        return ah1.getSortHint() - ah2.getSortHint();
    }

    private static InterruptList computeInterruptList(final BitSet ids) {
        final BitSet notIds = new BitSet(ids.length() + 1);
        notIds.or(ids);
        notIds.flip(0, notIds.length() + 1);
        final int[] unusedIds = new int[notIds.cardinality()];
        for (int id = notIds.nextSetBit(0), idx = 0; id >= 0; id = notIds.nextSetBit(id + 1), idx++) {
            unusedIds[idx] = id;
            if (id == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        return new InterruptList(unusedIds);
    }

    private static <T> int indexOf(final List<T> list, final T item, final int offset) {
        final int index = list.subList(offset, list.size()).indexOf(item);
        return index >= 0 ? index + offset : index;
    }

    private static <T> boolean validateInterruptIds(final int[] ids, final BitSet set, final List<T> list, final T instance) {
        final BitSet visited = new BitSet();
        for (final int id : ids) {
            if (id < 0 || (set.get(id) && list.get(id) != instance)) {
                return false;
            }
            if (visited.get(id)) {
                return false;
            }
            visited.set(id);
        }
        return true;
    }

    private static <T> void applyInterruptIds(final int[] ids, final BitSet set, final List<T> list, final T instance) {
        for (final int id : ids) {
            set.set(id);
            while (list.size() <= id)
                list.add(null);
            list.set(id, instance);
        }
    }

    private static final class InterruptIterator<T> implements PrimitiveIterator.OfInt {
        private final List<T> list;
        private final T value;
        private int next;

        private InterruptIterator(final List<T> list, final T value) {
            this.list = list;
            this.value = value;
            next = list.indexOf(value);
        }

        @Override
        public boolean hasNext() {
            return next >= 0;
        }

        @Override
        public int nextInt() {
            if (!hasNext()) throw new NoSuchElementException();
            final int value = next;
            next = indexOf(list, this.value, value + 1);
            return value;
        }
    }
}
