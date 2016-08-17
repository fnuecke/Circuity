package li.cil.circuity.common.bus;

import com.google.common.base.Throwables;
import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusSegment;
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
 * <tr><td>0</td><td>Number of mapped devices. Read-only.</td></tr>
 * <tr><td>1</td><td>Selected device. Read-write.</td></tr>
 * <tr><td>2</td><td>Type identified of the device. Read-only.</td></tr>
 * <tr><td>3</td><td>High address selected device is mapped to. Read-only.</td></tr>
 * <tr><td>4</td><td>Low address selected device is mapped to. Read-only.</td></tr>
 * </table>
 */
public abstract class AbstractBusController extends AbstractAddressable implements BusController, AddressHint, BusStateAware {
    /**
     * The number of addressable words via this buses address space.
     */
    public static final int ADDRESS_COUNT = 0xFFFFF;

    /**
     * The interval in which to re-scan the bus in case multiple controllers
     * were detected or the scan hit the end of the loaded world, in seconds.
     */
    private static final int RESCAN_INTERVAL = 5;

    /**
     * Address block representing the full address space of the bus.
     */
    private static final AddressBlock FULL_ADDRESS_BLOCK = new AddressBlock(0, ADDRESS_COUNT, 8);

    /**
     * General device information about this bus controller.
     */
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.BUS_CONTROLLER);

    // --------------------------------------------------------------------- //

    /**
     * Used for synchronized access to the bus, in particular for read/writes
     * and rescanning/changing addresses/interrupts of devices.
     */
    private final Object busLock = new Object();

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
     * Mapping of addresses to devices, the brain-dead way.
     * <p>
     * Direct mapping of address location to device at that location. This is
     * obviously not feasible for buses with a higher address bus width, but
     * for our purposes the used memory is acceptable.
     */
    private final Addressable[] addresses = new Addressable[ADDRESS_COUNT];

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
    private final Comparator<Addressable> addressComparator = Comparator.comparingInt(addressable -> addressBlocks.get(addressable).getOffset());

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
     * Whether the last scan failed.
     */
    private boolean hasErrors;

    /**
     * Set if we currently have a worker thread running.
     */
    private Future currentUpdate;

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

    // --------------------------------------------------------------------- //
    // AbstractAddressable

    @Override
    protected AddressBlock validateAddress(final AddressBlock memory) {
        return memory.take(Constants.BUS_CONTROLLER_ADDRESS, (1 + 1 + 2) * 8);
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
        synchronized (busLock) {
            switch (address) {
                case 0: // Number of addressable devices.
                    return addressables.size();
                case 1: // Select device.
                    return selected;
                case 2: { // Type identifier of selected addressable device.
                    if (selected < 0 || selected >= addressables.size()) {
                        return 0;
                    } else {
                        final Addressable device = addressables.get(selected);
                        final DeviceInfo info = device.getDeviceInfo();
                        return info != null ? info.type.id : 0;
                    }
                }
                case 3: { // High address of selected addressable device.
                    if (selected < 0 || selected >= addressables.size()) {
                        return 0;
                    } else {
                        final Addressable device = addressables.get(selected);
                        final AddressBlock memory = addressBlocks.get(device);
                        return (memory.getOffset() >> 8) & 0xFF;
                    }
                }
                case 4: { // Low address of selected addressable device.
                    if (selected < 0 || selected >= addressables.size()) {
                        return 0;
                    } else {
                        final Addressable device = addressables.get(selected);
                        final AddressBlock memory = addressBlocks.get(device);
                        return memory.getOffset() & 0xFF;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public void write(final int address, final int value) {
        switch (address) {
            case 1:
                selected = value;
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
    public void scheduleScan() {
        final World world = getBusWorld();
        if (world.isRemote) return;
        synchronized (busLock) {
            if (scheduledScan == null) {
                scheduledScan = SillyBeeAPI.scheduler.schedule(world, this::scanSynchronized);
            }
        }
    }

    @Override
    public void mapAndWrite(final int address, final int value) {
        synchronized (busLock) {
            final Addressable device = addresses[address];
            if (device != null) {
                final AddressBlock memory = addressBlocks.get(device);
                final int mappedAddress = address - memory.getOffset();
                device.write(mappedAddress, value);
            } else {
                segfault();
            }
        }
    }

    @Override
    public int mapAndRead(final int address) {
        synchronized (busLock) {
            final Addressable device = addresses[address];
            if (device != null) {
                final AddressBlock memory = addressBlocks.get(device);
                final int mappedAddress = address - memory.getOffset();
                return device.read(mappedAddress);
            } else {
                segfault();
                return 0;
            }
        }
    }

    @Override
    public void interrupt(final int interruptId, final int data) {
        final int interruptSinkId = interruptMap[interruptId];
        final InterruptSink sink = interruptSinks.get(interruptSinkId);
        if (sink != null) {
            sink.interrupt(interruptId, data);
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * Starts an update cycle.
     * <p>
     * This will trigger a worker thread which will then update all
     * {@link li.cil.circuity.api.bus.device.AsyncTickable} bus devices, if
     * and only if the bus controller is in a legal state (i.e. no two bus
     * controllers connected to the same bus).
     * <p>
     * This method is <em>not</em> thread safe. It is expected to be called
     * from the server thread only.
     */
    public void startUpdate() {
        if (currentUpdate == null) {
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
     * Set the buses power state.
     * <p>
     * Notifies attached state aware devices if the state changes.
     *
     * @param value the new power state.
     */
    public void setOnline(final boolean value) {
        synchronized (busLock) {
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
    }

    /**
     * Check whether the bus controller is currently in an error state.
     * <p>
     * The controller is in an error state when the last scan failed. This can
     * happen if there are multiple controllers on the same bus, or when part
     * of the bus cannot generate its list of adjacent devices (e.g. due to
     * the adjacent block being in an unloaded chunk).
     *
     * @return <code>true</code> when in an error state; <code>false</code> otherwise.
     */
    public boolean hasErrors() {
        return hasErrors;
    }

    /**
     * Clears the bus controller's state, removing all devices (and setting
     * their bus controller to <code>null</code>).
     */
    public void clear() {
        synchronized (busLock) {
            if (!doAnyAddressesOverlap()) {
                Arrays.fill(addresses, null);
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

    public void setDeviceAddress(final int index, final AddressBlock address) {
        synchronized (busLock) {
            final boolean didAnyAddressesOverlap = doAnyAddressesOverlap();

            addressBlocks.put(addressables.get(index), address);

            final boolean doAnyAddressesOverlap = doAnyAddressesOverlap();
            if (!didAnyAddressesOverlap && doAnyAddressesOverlap) {
                Arrays.fill(addresses, null);
                for (final Addressable addressable : addressables) {
                    addressable.setMemory(null);
                }
            } else if (didAnyAddressesOverlap && !doAnyAddressesOverlap) {
                for (final Addressable addressable : addressables) {
                    final AddressBlock memory = addressBlocks.get(addressable);
                    setAddressMap(memory, addressable);
                    addressable.setMemory(memory);
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

    protected abstract World getBusWorld();

    // --------------------------------------------------------------------- //

    private void updateDevicesAsync() {
        synchronized (busLock) {
            tickables.forEach(AsyncTickable::updateAsync);
        }
    }

    private void segfault() {
        // TODO Interrupt?
    }

    // Avoids one level of indentation in scan.
    private void scanSynchronized() {
        synchronized (busLock) {
            scheduledScan = null;
            hasErrors = false;
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
                    scanErrored();
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

                    if (device instanceof Addressable) {
                        final Addressable addressable = (Addressable) device;

                        int index = Collections.binarySearch(addressables, addressable, addressComparator);
                        while (addressables.get(index) != addressable) index++;
                        addressables.remove(index);

                        final AddressBlock memory = addressBlocks.remove(addressable);

                        if (!didAnyAddressesOverlap) {
                            setAddressMap(memory, null);
                            addressable.setMemory(null);
                        }
                    }

                    if (device instanceof InterruptSource) {
                        final InterruptSource source = (InterruptSource) device;

                        final int[] ids = source.getEmittedInterrupts(InterruptList.empty());
                        for (final int id : ids) {
                            if (id < 0 || id >= interruptSources.size() || interruptSources.get(id) != device) {
                                ModCircuity.getLogger().warn("InterruptSource claims to own an interrupt ID that is invalid or not owned by it. This indicates an incorrect implementation in '%s'.", device.getClass().getName());
                            } else {
                                interruptSourceIds.clear(id);
                                interruptMap[id] = -1;
                                interruptSources.set(id, null);
                            }
                        }
                    }

                    if (device instanceof InterruptSink) {
                        final InterruptSink sink = (InterruptSink) device;

                        final int[] ids = sink.getAcceptedInterrupts(InterruptList.empty());
                        for (final int id : ids) {
                            if (id < 0 || id >= interruptSinks.size() || interruptSinks.get(id) != device) {
                                ModCircuity.getLogger().warn("InterruptSink claims to own an interrupt ID that is invalid or not owned by it. This indicates an incorrect implementation in '%s'.", device.getClass().getName());
                            } else {
                                interruptSinkIds.clear(id);
                                for (int i = 0; i < interruptMap.length; i++) {
                                    if (interruptMap[i] == id) {
                                        interruptMap[i] = -1;
                                    }
                                }
                                interruptSinks.set(id, null);
                            }
                        }
                    }

                    device.setBusController(null);
                }
            }
        }

        // Multiple controllers on one bus are a no-go. If we detect we're
        // connected to another controller, shut down everything and start
        // rescanning periodically.
        final boolean hasMultipleControllers = newDevices.stream().anyMatch(device -> device instanceof BusController && device != this);
        if (hasMultipleControllers) {
            scanErrored();
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
                final AddressBlock memory = getFreeAddress(addressable);

                addressBlocks.put(addressable, memory);

                final int index = Collections.binarySearch(addressables, addressable, addressComparator);
                addressables.add(index >= 0 ? index : ~index, addressable);

                if (!didAnyAddressesOverlap) {
                    setAddressMap(memory, addressable);
                }
            }

            if (device instanceof InterruptSource) {
                final InterruptSource source = (InterruptSource) device;

                final int[] ids = source.getEmittedInterrupts(computeInterruptList(interruptSourceIds));
                for (final int id : ids) {
                    if (id < 0 || id >= interruptSources.size() || interruptSourceIds.get(id)) {
                        ModCircuity.getLogger().warn("InterruptSource wants to use an interrupt ID that is invalid or already in use. This indicates an incorrect implementation in '%s'.", device.getClass().getName());
                    }
                    interruptSourceIds.set(id);
                }
            }

            if (device instanceof InterruptSink) {
                final InterruptSink sink = (InterruptSink) device;

                final int[] ids = sink.getAcceptedInterrupts(computeInterruptList(interruptSinkIds));
                for (final int id : ids) {
                    if (id < 0 || id >= interruptSinks.size() || interruptSinkIds.get(id)) {
                        ModCircuity.getLogger().warn("InterruptSink wants to use an interrupt ID that is invalid or already in use. This indicates an incorrect implementation in '%s'.", device.getClass().getName());
                    } else {
                        interruptSinkIds.set(id);
                        while (interruptSinks.size() <= id)
                            interruptSinks.add(null);
                        interruptSinks.set(id, null);
                    }
                }
            }
        }

        // Ensure capacity of interrupt map is sufficiently large if new sources were added.
        if (interruptSourceIds.length() > interruptMap.length) {
            final int[] newInterruptMap = Arrays.copyOf(interruptMap, interruptSourceIds.length());
            Arrays.fill(newInterruptMap, interruptMap.length, newInterruptMap.length, -1);
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
                    if (didAnyAddressesOverlap) {
                        setAddressMap(memory, addressable);
                    }
                    addressable.setMemory(memory);
                }
            }
        } else if (!didAnyAddressesOverlap) {
            Arrays.fill(addresses, null);
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
            addressBlocks.clear();

            // Look for devices with sort hints, add them in the order given.
            addressables.stream().
                    filter(a -> a instanceof AddressHint).
                    sorted(AbstractBusController::compareAddressHints).
                    forEach(a -> addressBlocks.put(a, getFreeAddress(a)));

            // Then add the remaining devices.
            for (final Addressable addressable : addressables) {
                if (addressable instanceof AddressHint) continue;
                addressBlocks.put(addressable, getFreeAddress(addressable));
            }

            // If we could resolve the overlap, notify all addressable devices
            // of their new addresses.
            if (!doAnyAddressesOverlap()) {
                for (final Addressable addressable : addressables) {
                    final AddressBlock memory = addressBlocks.get(addressable);
                    setAddressMap(memory, addressable);
                    addressable.setMemory(memory);
                }
            }
        }
    }

    private void scanErrored() {
        clear();
        scheduledScan = SillyBeeAPI.scheduler.scheduleIn(getBusWorld(), RESCAN_INTERVAL * 20, this::scanSynchronized);
        hasErrors = true;
    }

    private boolean doAnyAddressesOverlap() {
        for (int i = 1; i < addressables.size(); i++) {
            final Addressable addressable1 = addressables.get(i - 1);
            final Addressable addressable2 = addressables.get(i);

            final AddressBlock memory1 = addressBlocks.get(addressable1);
            final AddressBlock memory2 = addressBlocks.get(addressable2);

            final int end1 = memory1.getOffset() + memory1.getLength();
            if (end1 > ADDRESS_COUNT) {
                return true;
            }

            if (end1 > memory2.getOffset()) {
                return true;
            }
        }
        return false;
    }

    private void setAddressMap(final AddressBlock memory, @Nullable final Addressable addressable) {
        for (int address = memory.getOffset(), end = memory.getOffset() + memory.getLength(); address < end; address++) {
            addresses[address] = addressable;
        }
    }

    private AddressBlock getFreeAddress(final Addressable newAddressable) {
        // Addressable devices are ordered by address, find gaps, specifically
        // find a gap that's large enough to fit the specified device.
        int address = 0;
        for (final Addressable addressable : addressables) {
            final AddressBlock memory = addressBlocks.get(addressable);

            // A device may have no assigned memory assigned, yet, if this is
            // called while remapping all devices on the bus.
            if (memory == null) continue;

            final int available = memory.getOffset() - address;
            if (available > 0) {
                final AddressBlock candidate = new AddressBlock(address, available, FULL_ADDRESS_BLOCK.getWordSize());
                final AddressBlock requested = newAddressable.getMemory(candidate);
                if (requested.getOffset() >= candidate.getOffset() && requested.getOffset() + requested.getLength() <= candidate.getOffset() + candidate.getLength()) {
                    return requested;
                }
            }

            // In case of already overlapping device addresses, avoid going
            // back in address space as that might give us false positives.
            address = Math.max(address, memory.getOffset() + memory.getLength());
        }

        // Either we failed to find a gap, or we're at the empty space after the
        // last currently mapped addressable device. Use that space, even if it
        // means overlap.
        return newAddressable.getMemory(new AddressBlock(address, FULL_ADDRESS_BLOCK.getLength() - address, FULL_ADDRESS_BLOCK.getWordSize()));
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
}
