package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.event.ContainerDestructionListener;
import li.cil.lib.api.ecs.component.event.InventoryChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.items.ItemHandlerListWrapper;
import li.cil.lib.items.ItemHandlerProxy;
import li.cil.lib.synchronization.value.SynchronizedArray;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Serializable
public class InventoryImmutable extends AbstractComponent implements ItemHandlerProxy, ContainerDestructionListener, ICapabilityProvider {
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

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (filter == null || filter.canInsertItem(this, slot, stack))
                return super.insertItem(slot, stack, simulate);
            return stack;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return stackLimit >= 0 ? stackLimit : super.getSlotLimit(slot);
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
        final int oldSize = stacks.size();

        stacks.setSize(size);

        // When growing, avoid null entries.
        for (int slot = oldSize; slot < size; ++slot) {
            stacks.set(slot, ItemStack.EMPTY);
        }

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
    // ContainerDestructionListener

    @Override
    public void handleContainerDestruction() {
        final Optional<Location> location = getComponent(Location.class);
        location.ifPresent(this::dropInventoryItems);
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

    // --------------------------------------------------------------------- //

    private void fireInventoryChanged(final int slot) {
        getComponents(InventoryChangeListener.class).forEach(l -> l.handleInventoryChange(this, slot));
    }

    private void dropInventoryItems(final Location location) {
        final World world = location.getWorld();
        final Vec3d pos = location.getPositionVector();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.xCoord, pos.yCoord, pos.zCoord, stack);
            }
        }
    }
}
