package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.items.ItemHandlerModifiableProxy;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;

public class InventoryMutable extends InventoryImmutable implements ItemHandlerModifiableProxy, ICapabilityProvider {
    @CapabilityInject(IItemHandlerModifiable.class)
    public static Capability<IItemHandlerModifiable> ITEM_HANDLER_MODIFIABLE_CAPABILITY;

    // --------------------------------------------------------------------- //

    public InventoryMutable(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // ItemHandlerModifiableProxy

    @Override
    public IItemHandlerModifiable getItemHandler() {
        return inventory;
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == ITEM_HANDLER_MODIFIABLE_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == ITEM_HANDLER_MODIFIABLE_CAPABILITY) {
            return ITEM_HANDLER_MODIFIABLE_CAPABILITY.cast(this);
        }
        return null;
    }
}
