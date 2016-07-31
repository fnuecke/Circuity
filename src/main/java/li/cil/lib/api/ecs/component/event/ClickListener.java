package li.cil.lib.api.ecs.component.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * When a component implements this interface, it will be notified when a player
 * interacts with the component's entity by clicking it.
 */
public interface ClickListener {
    boolean handleClicked(final EntityPlayer player);
}
