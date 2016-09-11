package li.cil.circuity.common.bus.controller;

import com.google.common.base.Throwables;
import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusConnector;
import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.controller.DeviceMapper;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.controller.Subsystem;
import li.cil.circuity.api.bus.controller.detail.ElementManager;
import li.cil.circuity.api.bus.controller.detail.SerialInterfaceProvider;
import li.cil.circuity.api.bus.device.AbstractBusDevice;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.AsyncTickable;
import li.cil.circuity.api.bus.device.BusChangeListener;
import li.cil.circuity.api.bus.device.BusStateListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.util.SerialPortManager;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.bus.util.BusThreadPool;
import li.cil.circuity.common.bus.util.SerialPortManagerProxy;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.scheduler.ScheduledCallback;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 * neighboring bus connectors changed.
 * </li>
 * <li>
 * call {@link #dispose()} when they get disposed/removed from the world.
 * </li>
 * </ul>
 */
public abstract class AbstractBusController extends AbstractBusDevice implements BusController, Addressable, AddressHint, BusStateListener, SerialPortManagerProxy {
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
         * State entered when the scan could not be completed due to a connector
         * failing to return its adjacent devices. Typically this will be due
         * to an adjacent block being in an unloaded chunk, but it may be used
         * to emulate failing hardware in the future.
         */
        ERROR_CONNECTION_FAILED(true);

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
     * Manages the serial interface of the bus controller.
     */
    private final SerialPortManager serialPortManager = new SerialPortManager();

    /**
     * Sub systems in use by this bus controller.
     */
    @Serialize
    private final Map<Class, Subsystem> subsystems = new LinkedHashMap<>();

    /**
     * Set of all currently known bus elements.
     */
    private final HashSet<BusElement> elements = new HashSet<>();

    /**
     * The list of state aware bus devices, i.e. device that are notified when
     * the bus is powered on / off.
     */
    private final List<BusStateListener> stateListeners = new ArrayList<>();

    /**
     * The list of change aware bus devices, i.e. device that are notified when
     * the bus changes/devices are added/removed.
     */
    private final List<BusChangeListener> changeListeners = new ArrayList<>();

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

    // --------------------------------------------------------------------- //

    public AbstractBusController() {
        serialPortManager.setPreferredAddressOffset(Constants.BUS_CONTROLLER_ADDRESS);
        serialPortManager.addSerialPort(this::readAPIVersion, null, null);

        // TODO Populate based on some registry in which addons can register additional subsystems?
        final DeviceMapperImpl selector = new DeviceMapperImpl();
        subsystems.put(DeviceMapper.class, selector);
        subsystems.put(AddressMapper.class, new AddressMapperImpl(this));
        subsystems.put(InterruptMapper.class, new InterruptMapperImpl(this));

        for (final Subsystem subsystem : subsystems.values()) {
            if (subsystem instanceof SerialInterfaceProvider) {
                final SerialInterfaceProvider provider = (SerialInterfaceProvider) subsystem;
                provider.initializeSerialInterface(serialPortManager, selector);
            }
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
            for (final BusStateListener listener : stateListeners) {
                try {
                    listener.handleBusOnline();
                } catch (final Throwable t) {
                    ModCircuity.getLogger().error("BusStateListener threw in handleBusOnline.", t);
                }
            }
        } else {
            for (final BusStateListener listener : stateListeners) {
                try {
                    listener.handleBusOffline();
                } catch (final Throwable t) {
                    ModCircuity.getLogger().error("BusStateListener threw in handleBusOffline.", t);
                }
            }
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
            for (final BusElement element : elements) {
                try {
                    element.setBusController(null);
                } catch (final Throwable t) {
                    ModCircuity.getLogger().error("BusElement threw in setBusController.", t);
                }
            }
            if (scheduledScan != null) {
                SillyBeeAPI.scheduler.cancel(getBusWorld(), scheduledScan);
                scheduledScan = null;
            }
        }
    }

    // --------------------------------------------------------------------- //

    protected abstract World getBusWorld();

    // --------------------------------------------------------------------- //
    // BusElement

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
    // BusDevice

    @Nullable
    @Override
    public DeviceInfo getDeviceInfo() {
        return DEVICE_INFO;
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
        for (final Subsystem subsystem : subsystems.values()) {
            subsystem.reset();
        }
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
    public Iterable<BusElement> getElements() {
        return elements;
    }

    // --------------------------------------------------------------------- //
    // SerialPortManagerProxy

    @Override
    public SerialPortManager getSerialPortManager() {
        return serialPortManager;
    }

    // --------------------------------------------------------------------- //

    private int readAPIVersion(final long address) {
        return API_VERSION;
    }

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

        final Set<BusElement> newElements = new HashSet<>();

        {
            final List<BusElement> adjacentElements = new ArrayList<>();
            final Set<BusConnector> closed = new HashSet<>();
            final Queue<BusConnector> open = new ArrayDeque<>();

            // Avoid null entries in iterables returned by getConnected() to screw
            // things up. Not that anyone should ever do that, but I don't trust
            // people not to screw this up, so we're playing it safe.
            closed.add(null);

            // Start at the bus controller. This is why the BusController
            // interface extends the BusConnector interface; homogenizes things.
            open.add(this);

            // Explore the graph implicitly defined by bus connectors' getConnected()
            // return values (which are, essentially, the edges in the graph) in
            // a breadth-first fashion, adding already explored connectors to the
            // closed set to avoid infinite loops due to cycles in the graph.
            while (!open.isEmpty()) {
                final BusConnector connector = open.poll();
                if (!closed.add(connector)) continue;
                if (!connector.getConnected(adjacentElements)) {
                    scanErrored(State.ERROR_CONNECTION_FAILED);
                    return;
                }
                for (final BusElement element : adjacentElements) {
                    newElements.add(element);
                    if (element instanceof BusConnector) {
                        open.add((BusConnector) element);
                    }
                }
                adjacentElements.clear();
            }

            // Similarly as with the above, avoid null entries in getConnected()
            // to screw things up. Still not trusting people. Who'd've thunk.
            newElements.remove(null);
        }

        // ----------------------------------------------------------------- //
        // Handle removed and added devices -------------------------------- //
        // ----------------------------------------------------------------- //

        {
            // Find devices that have been removed, update internal data
            // structures accordingly and notify them. While doing so, convert
            // the set of found devices into the set of added devices.
            final Iterator<BusElement> it = elements.iterator();
            while (it.hasNext()) {
                final BusElement element = it.next();

                // If the device is in the list of found devices, it is still
                // known and nothing changes for the device. We remove it so
                // that this set of devices only contains the added devices
                // when we're done. If it isn't in the list, then, well, it
                // is gone, and we have to update our internal data.
                if (!newElements.remove(element)) {
                    try {
                        element.setBusController(null);
                    } catch (final Throwable t) {
                        ModCircuity.getLogger().error("Bus element threw in setBusController.", t);
                    }

                    it.remove();

                    if (element instanceof BusStateListener) {
                        stateListeners.remove(element);
                    }

                    if (element instanceof BusChangeListener) {
                        changeListeners.remove(element);
                    }

                    if (element instanceof AsyncTickable) {
                        tickables.remove(element);
                    }

                    for (final Subsystem subsystem : subsystems.values()) {
                        if (subsystem instanceof ElementManager) {
                            final ElementManager manager = (ElementManager) subsystem;
                            manager.remove(element);
                        }
                    }
                }
            }
        }

        // The above leaves us with the list of added devices, update internal
        // data structures accordingly and notify them.
        for (final BusElement element : newElements) {
            try {
                element.setBusController(this);
            } catch (final Throwable t) {
                ModCircuity.getLogger().error("Bus element threw in setBusController.", t);
                continue;
            }

            elements.add(element);

            if (element instanceof BusStateListener) {
                stateListeners.add((BusStateListener) element);
            }

            if (element instanceof BusChangeListener) {
                changeListeners.add((BusChangeListener) element);
            }

            if (element instanceof AsyncTickable) {
                tickables.add((AsyncTickable) element);
            }

            for (final Subsystem subsystem : subsystems.values()) {
                if (subsystem instanceof ElementManager) {
                    final ElementManager manager = (ElementManager) subsystem;
                    manager.add(element);
                }
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
            final boolean hasMultipleControllers = newElements.stream().anyMatch(device -> device instanceof BusController && device != this);
            if (hasMultipleControllers) {
                scanErrored(State.ERROR_MULTIPLE_BUS_CONTROLLERS);
                return;
            }
        }

        state = State.READY;

        for (final BusChangeListener listener : changeListeners) {
            try {
                listener.handleBusChanged();
            } catch (final Throwable t) {
                ModCircuity.getLogger().error("BusChangeListener threw in handleBusChanged.", t);
            }
        }
    }

    private void scanErrored(final State error) {
        scheduledScan = SillyBeeAPI.scheduler.scheduleIn(getBusWorld(), RESCAN_INTERVAL * Constants.TICKS_PER_SECOND, this::scanSynchronized);
        state = error;
    }
}
