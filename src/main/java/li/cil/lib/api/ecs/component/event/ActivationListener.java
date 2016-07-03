package li.cil.lib.api.ecs.component.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

import javax.annotation.Nullable;

/**
 * When a component implements this interface, it will be notified when a player
 * interacts with the component's entity by right clicking it.
 */
public interface ActivationListener {
    boolean onActivated(final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ);
}
