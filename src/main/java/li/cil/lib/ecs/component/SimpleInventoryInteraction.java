package li.cil.lib.ecs.component;

import li.cil.lib.ModSillyBee;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Optional;

public class SimpleInventoryInteraction extends AbstractComponent implements ActivationListener {
    public SimpleInventoryInteraction(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final Optional<InventoryMutable> maybeInventory = getComponent(InventoryMutable.class);
        if (!maybeInventory.isPresent()) {
            ModSillyBee.getLogger().warn("Using {} on an entity that has no {}.", SimpleInventoryInteraction.class, InventoryMutable.class);
            return false;
        }

        final InventoryMutable inventory = maybeInventory.get();
        final ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty()) {
            // Extract.
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                final ItemStack extracted = inventory.extractItem(slot, inventory.getSlotLimit(slot), false);
                if (!extracted.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, extracted.copy());
                    return true;
                }
            }
        } else {
            // Inject.
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                final ItemStack remainder = inventory.insertItem(slot, stack.copy(), false);
                if (remainder.isEmpty() || remainder.getCount() < stack.getCount()) {
                    stack.setCount(remainder.getCount());
                    return true;
                }
            }
        }

        return false;
    }
}
