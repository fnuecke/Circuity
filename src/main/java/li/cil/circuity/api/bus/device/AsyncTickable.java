package li.cil.circuity.api.bus.device;

/**
 * Like {@link net.minecraft.util.ITickable}, but called from worker threads.
 * <p>
 * This may be implemented on {@link li.cil.circuity.api.bus.BusDevice}s which
 * wish to be updated by the bus controller they're connected to. Not using
 * {@link net.minecraft.util.ITickable} for this to avoid confusion (since
 * tickable is usually assumed to be called from the server thread).
 */
public interface AsyncTickable {
    void updateAsync();
}
