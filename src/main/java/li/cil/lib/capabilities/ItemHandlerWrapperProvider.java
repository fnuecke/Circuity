package li.cil.lib.capabilities;

import com.google.common.collect.Iterables;
import li.cil.lib.api.capabilities.WrapperProvider;
import net.minecraftforge.items.IItemHandler;

public enum ItemHandlerWrapperProvider implements WrapperProvider<IItemHandler> {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Override
    public IItemHandler createWrapper(final Iterable<IItemHandler> instances) {
        return new CombinedItemHandlerWrapper(Iterables.toArray(instances, IItemHandler.class));
    }
}
