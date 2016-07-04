package li.cil.lib.api.capabilities;

import net.minecraftforge.common.capabilities.Capability;

/**
 * Wrapper factory contract.
 * <p>
 * These may be registered to allow automatic wrapping of multiple capability
 * implementations in a single block. This is useful for block that act based
 * on a user-defined configuration, e.g. based on certain items in the block's
 * inventory, where multiple sub-components may provide the same capability.
 * <p>
 * For example, when multiple item handlers are present, they will be wrapped
 * using the standard {@link net.minecraftforge.items.wrapper.CombinedInvWrapper}.
 * <p>
 * Register implementations of this interface via {@link li.cil.lib.api.CapabilitiesAPI#register(Capability, WrapperProvider)}.
 *
 * @param <T> the generic type of the wrapped capability.
 */
public interface WrapperProvider<T> {
    /**
     * Generate a wrapper object interfacing all of the provided instances of
     * the capability type this wrapper provider handles.
     * <p>
     * There will be at least two elements in the passed list.
     *
     * @param instances the list of capability instances to wrap.
     * @return a wrapper for the passed instances.
     */
    T createWrapper(final Iterable<T> instances);
}
