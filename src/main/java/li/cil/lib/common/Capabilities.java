package li.cil.lib.common;

import li.cil.lib.api.CapabilitiesAPI;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.capabilities.WrapperProvider;
import net.minecraftforge.common.capabilities.Capability;

import java.util.HashMap;
import java.util.Map;

public enum Capabilities implements CapabilitiesAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final Map<Capability, WrapperProvider> wrapperProviders = new HashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.capabilities = INSTANCE;
    }

    // --------------------------------------------------------------------- //

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getWrapper(final Capability<T> capability, final Iterable<T> instances) {
        final WrapperProvider provider = wrapperProviders.get(capability);
        if (provider != null) {
            return (T) provider.createWrapper(instances);
        }
        return instances.iterator().next();
    }

    @Override
    public <T> void register(final Capability<T> capability, final WrapperProvider<T> wrapperProvider) {
        wrapperProviders.put(capability, wrapperProvider);
    }
}
