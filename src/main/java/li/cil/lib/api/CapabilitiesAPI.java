package li.cil.lib.api;

import li.cil.lib.api.capabilities.WrapperProvider;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Access to capability wrappers.
 */
public interface CapabilitiesAPI {
    /**
     * Register a new capability wrapper for the specified capability.
     *
     * @param capability      the capability to register the wrapper for.
     * @param wrapperProvider the wrapper to register.
     * @param <T>             the generic type of the capability.
     */
    <T> void register(final Capability<T> capability, final WrapperProvider<T> wrapperProvider);

    /**
     * Get a wrapped version of a list of capability instances.
     * <p>
     * If no {@link WrapperProvider} is registered for the capability, the first
     * instance in the list is returned.
     *
     * @param capability the capability to get a wrapper for.
     * @param instances  the instances to wrap.
     * @param <T>        the generic type of the capability.
     * @return a wrapper for the passed instances if possible; the first instance otherwise.
     */
    <T> T getWrapper(final Capability<T> capability, final Iterable<T> instances);
}
