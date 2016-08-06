package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;
import net.minecraft.entity.player.EntityPlayer;

/**
 * When a component implements this interface, it will be notified when a player
 * interacts with the component's entity by clicking it.
 */
public interface ClickListener extends Component {
    boolean handleClicked(final EntityPlayer player);
}
