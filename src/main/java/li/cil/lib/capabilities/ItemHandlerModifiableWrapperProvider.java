package li.cil.lib.capabilities;

import com.google.common.collect.Iterables;
import li.cil.lib.api.capabilities.WrapperProvider;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public enum ItemHandlerModifiableWrapperProvider implements WrapperProvider<IItemHandlerModifiable> {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Override
    public IItemHandlerModifiable createWrapper(final Iterable<IItemHandlerModifiable> instances) {
        return new CombinedInvWrapper(Iterables.toArray(instances, IItemHandlerModifiable.class));
    }
}
