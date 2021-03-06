package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

/**
 * When a component implements this interface, it will be notified when a player
 * interacts with the component's entity by right clicking it.
 */
public interface ActivationListener extends Component {
    boolean handleActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ);
}
