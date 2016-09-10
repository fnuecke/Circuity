package li.cil.circuity.common.bus;

import com.google.common.base.Throwables;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusSegment;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.controller.Subsystem;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusChangeListener;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
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
 * call {@link #dispose()} when they get disposed/removed from the world.
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
public abstract class AbstractBusController extends AbstractBusDevice implements BusController, Addressable, AddressHint, BusStateListener {
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
     * General device information about this bus controller.
     */
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.BUS_CONTROLLER, Constants.DeviceInfo.BUS_CONTROLLER_NAME);

    /**
     * Comparator used to keep the {@link #devices} list sorted.
     */
    private static final Comparator<BusDevice> DEVICE_COMPARATOR = Comparator.comparing(BusDevice::getPersistentId);

    // --------------------------------------------------------------------- //

    /**
     * Possible states the bus controller may be in after a scan.
     */
    public enum State {
        /**
         * All is well, controller is operating normally.
         */
        READY(false),

        /**
         * A scan is currently pending.
         */
        SCANNING(false),

        /**
         * State entered when multiple bus controllers were present.
         */
        ERROR_MULTIPLE_BUS_CONTROLLERS(true),

        /**
         * State entered when a subsystem is in an invalid state.
         */
        ERROR_SUBSYSTEM(true),

        /**
         * State entered when the scan could not be completed due to a segment
         * failing to return its adjacent devices. Typically this will be due
         * to an adjacent block being in an unloaded chunk, but it may be used
         * to emulate failing hardware in the future.
         */
        ERROR_SEGMENT_FAILED(true);

        public final boolean isError;

        State(final boolean isError) {
            this.isError = isError;
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * Used for synchronized access to the bus, in particular for read/writes
     * and rescanning/changing addresses/interrupts of devices.
     */
    private final Object lock = new Object();

    /**
     * Sub systems in use by this bus controller.
     */
    @Serialize
    private final Map<Class, Subsystem> subsystems = new HashMap<>();

    /**
     * List of all currently known bus devices on the bus.
     * <p>
     * This is updated after a scan triggered by {@link #scheduleScan()} has
     * completed. It will then contain the list of all connected devices.
     * <p>
     * Sorted by device UUID, allows binary search and stable access from
     * serial interface by index.
     */
    private final List<BusDevice> devices = new ArrayList<>();

    /**
     * Direct access to bus devices based on their persistent ID.
     */
    private final Map<UUID, BusDevice> deviceById = new HashMap<>();

    /**
     * The list of state aware bus devices, i.e. device that are notified when
     * the bus is powered on / off.
     */
    private final List<BusStateListener> stateAwares = new ArrayList<>();

    /**
     * The list of bus devices that also implement {@link ITickable}.
     * <p>
     * These will be updated by the bus controller's worker thread whenever the
     * bus controller is updated (which really should be every tick).
     */
    private final List<AsyncTickable> tickables = new ArrayList<>();

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

    // --------------------------------------------------------------------- //

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

    public AbstractBusController() {
        // TODO Populate based on some registry in which addons can register additional subsystems?
        subsystems.put(AddressMapper.class, new AddressMapperImpl(this));
        subsystems.put(InterruptMapper.class, new InterruptMapperImpl(this));
    }

    // --------------------------------------------------------------------- //
    // BusDevice

    @Nullable
    @Override
    public DeviceInfo getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Nullable
    @Override
    public BusController getBusController() {
        return this;
    }

    @Override
    public void setBusController(@Nullable final BusController controller) {
        assert controller == this : "Multiple controllers on one bus.";
    }

    // --------------------------------------------------------------------- //
    // Addressable

    @Override
    public AddressBlock getPreferredAddressBlock(final AddressBlock memory) {
        return memory.take(Constants.BUS_CONTROLLER_ADDRESS, 7);
    }

    // TODO Allow subsystems to dynamically extend the serial protocol. Will also need some way to dynamically document it then, tho.
    @Override
    public int read(final int address) {
        switch (address) {
            case 0: // Version of this serial API.
                return API_VERSION;
            case 1: // Number of addressable devices.
                return devices.size();
            case 2: // Selected device.
                return selected;
            case 3: { // Type identifier of selected addressable device.
                if (selected >= 0 && selected < devices.size()) {
                    final BusDevice device = devices.get(selected);
                    final DeviceInfo info = device.getDeviceInfo();
                    return info != null ? info.type.id : 0xFFFFFFFF;
                }
                break;
            }
            case 4: { // Read a byte of the address the device is mapped to.
                if (selected >= 0 && selected < devices.size()) {
                    final BusDevice device = devices.get(selected);
                    if (device instanceof Addressable) {
                        final Addressable addressable = (Addressable) device;
                        final AddressMapper mapper = getSubsystem(AddressMapper.class);
                        final AddressBlock memory = mapper.getAddressBlock(addressable);
                        return (int) ((memory.getOffset() >>> (addressShift++ * mapper.getWordSize())) & mapper.getWordMask());
                    }
                }
                break;
            }
            case 5: { // Read a byte of the size of the device.
                if (selected >= 0 && selected < devices.size()) {
                    final BusDevice device = devices.get(selected);
                    if (device instanceof Addressable) {
                        final Addressable addressable = (Addressable) device;
                        final AddressMapper mapper = getSubsystem(AddressMapper.class);
                        final AddressBlock memory = mapper.getAddressBlock(addressable);
                        return (int) ((memory.getLength() >>> (sizeShift++ * mapper.getWordSize())) & mapper.getWordMask());
                    }
                }
                break;
            }
            case 6: { // Read a single character of the name of the selected device.
                if (selected >= 0 && selected < devices.size()) {
                    final BusDevice device = devices.get(selected);
                    final DeviceInfo info = device.getDeviceInfo();
                    final String name = info != null ? info.name : null;
                    return name != null && nameIndex < name.length() ? name.charAt(nameIndex++) : 0;
                }
                break;
            }
        }
        return 0xFFFFFFFF;
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Subsystem> T getSubsystem(final Class<T> subsystem) {
        return (T) subsystems.get(subsystem);
    }

    @Override
    public Iterable<BusDevice> getDevices() {
        return devices;
    }

    @Nullable
    @Override
    public BusDevice getDevice(final UUID persistentId) {
        return deviceById.get(persistentId);
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
            stateAwares.forEach(BusStateListener::handleBusOnline);
        } else {
            stateAwares.forEach(BusStateListener::handleBusOffline);
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
    public void dispose() {
        // Needs to be synchronized as it may be called when owner is disposed,
        // which may happen during a tick, i.e. while async update is running.
        synchronized (lock) {
            try {
                for (final BusDevice device : devices) {
                    device.setBusController(null);
                }
            } finally {
                subsystems.values().forEach(Subsystem::dispose);
                devices.clear();
                deviceById.clear();
                stateAwares.clear();
                tickables.clear();

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
            for (final AsyncTickable tickable : tickables) {
                // A rescan might have snuck in or the owner may have been disposed
                // between this was scheduled and before the worker thread started.
                if (!isOnline()) return;
                tickable.updateAsync();
            }
        }
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

                    deviceById.remove(device.getPersistentId());

                    if (device instanceof BusStateListener) {
                        stateAwares.remove(device);
                    }

                    if (device instanceof AsyncTickable) {
                        tickables.remove(device);
                    }

                    for (final Subsystem subsystem : subsystems.values()) {
                        subsystem.remove(device);
                    }

                    device.setBusController(null);
                }
            }
        }

        // The above leaves us with the list of added devices, update internal
        // data structures accordingly and notify them.
        for (final BusDevice device : newDevices) {
            final int index = Collections.binarySearch(devices, device, DEVICE_COMPARATOR);
            assert index < 0 : "Device has been added twice.";
            devices.add(~index, device);

            device.setBusController(this);

            deviceById.put(device.getPersistentId(), device);

            if (device instanceof BusStateListener) {
                stateAwares.add((BusStateListener) device);
            }

            if (device instanceof AsyncTickable) {
                tickables.add((AsyncTickable) device);
            }

            for (final Subsystem subsystem : subsystems.values()) {
                subsystem.add(device);
            }
        }

        // ----------------------------------------------------------------- //
        // Handle state changes due to added/removed devices --------------- //
        // ----------------------------------------------------------------- //

        // Make sure to run this always when also calling add/remove on the
        // subsystems, and to always call it on all subsystems. This allows
        // post-processing in validate.
        {
            // TODO Collect a list of error messages?
            boolean allSubsystemsValid = true;
            for (final Subsystem subsystem : subsystems.values()) {
                // Important: & not &&, to make sure validate() is always called.
                allSubsystemsValid &= subsystem.validate();
            }
            if (!allSubsystemsValid) {
                scanErrored(State.ERROR_SUBSYSTEM);
                return;
            }
        }

        // ----------------------------------------------------------------- //
        // Check for multiple controllers ---------------------------------- //
        // ----------------------------------------------------------------- //

        {
            // Multiple controllers on one bus are a no-go. If we detect we're
            // connected to another controller, shut down everything and start
            // rescanning periodically.
            final boolean hasMultipleControllers = newDevices.stream().anyMatch(device -> device instanceof BusController && device != this);
            if (hasMultipleControllers) {
                scanErrored(State.ERROR_MULTIPLE_BUS_CONTROLLERS);
                return;
            }
        }

        state = State.READY;

        for (final BusDevice device : devices) {
            if (device instanceof BusChangeListener) {
                final BusChangeListener listener = (BusChangeListener) device;
                listener.handleBusChanged();
            }
        }
    }

    private void scanErrored(final State error) {
        scheduledScan = SillyBeeAPI.scheduler.scheduleIn(getBusWorld(), RESCAN_INTERVAL * Constants.TICKS_PER_SECOND, this::scanSynchronized);
        state = error;
    }
}
