package li.cil.lib.ecs.component;

import li.cil.circuity.common.capabilities.NoSuchCapabilityException;
import li.cil.lib.api.ecs.component.event.InventoryChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.items.ItemHandlerListWrapper;
import li.cil.lib.items.ItemHandlerProxy;
import li.cil.lib.synchronization.value.SynchronizedArray;
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

    @FunctionalInterface
    public interface ItemFilter {
        boolean canInsertItem(final IItemHandler inventory, final int slot, final ItemStack stack);
    }

    // --------------------------------------------------------------------- //

    @Serialize
    protected final SynchronizedArray<ItemStack> stacks = new SynchronizedArray<>(ItemStack.class);

    protected final ItemHandlerListWrapper inventory = new ItemHandlerListWrapper() {
        @Override
        public List<ItemStack> getList() {
            return stacks;
        }

        @Override
        protected int getStackLimit(final int slot, final ItemStack stack) {
            return stackLimit >= 0 ? stackLimit : super.getStackLimit(slot, stack);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            markChanged();
            InventoryImmutable.this.fireInventoryChanged(slot);
        }

        @Nullable
        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (filter != null && !filter.canInsertItem(this, slot, stack))
                return stack;
            return super.insertItem(slot, stack, simulate);
        }
    };

    private int stackLimit = -1;
    private ItemFilter filter;

    // --------------------------------------------------------------------- //

    public InventoryImmutable(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //

    public InventoryImmutable setSize(final int size) {
        stacks.setSize(size);
        return this;
    }

    public InventoryImmutable setStackLimit(final int limit) {
        stackLimit = limit;
        return this;
    }

    public InventoryImmutable setFilter(@Nullable final ItemFilter filter) {
        this.filter = filter;
        return this;
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

    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == ITEM_HANDLER_CAPABILITY) {
            return ITEM_HANDLER_CAPABILITY.cast(this);
        }
        throw new NoSuchCapabilityException();
    }

    // --------------------------------------------------------------------- //

    private void fireInventoryChanged(final int slot) {
        getComponents(InventoryChangeListener.class).forEach(l -> l.handleInventoryChange(this, slot));
    }
}
