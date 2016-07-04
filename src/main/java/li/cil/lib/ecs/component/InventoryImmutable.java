package li.cil.lib.ecs.component;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedArray;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.items.ItemHandlerListWrapper;
import li.cil.lib.items.ItemHandlerProxy;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public class InventoryImmutable extends AbstractComponent implements ItemHandlerProxy, ICapabilityProvider {
    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY;

    // --------------------------------------------------------------------- //

    @Serialize
    protected final SynchronizedArray<ItemStack> stacks = new SynchronizedArray<>(ItemStack.class);

    protected final ItemHandlerListWrapper inventory = new ItemHandlerListWrapper() {
        @Override
        public List<ItemStack> getList() {
            return stacks;
        }

        @Override
        public void setStackInSlot(final int slot, @Nullable final ItemStack stack) {
            super.setStackInSlot(slot, stack);
            markChanged();
        }
    };

    // --------------------------------------------------------------------- //

    public InventoryImmutable(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    public void setSize(final int size) {
        stacks.setSize(size);
    }

    // --------------------------------------------------------------------- //
    // ItemHandlerProxy

    @Override
    public IItemHandler getItemHandler() {
        return inventory;
    }

    // --------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == ITEM_HANDLER_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == ITEM_HANDLER_CAPABILITY) {
            return ITEM_HANDLER_CAPABILITY.cast(this);
        }
        return null;
    }
}
