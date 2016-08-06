package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;
import net.minecraft.entity.Entity;

/**
 * When a component implements this interface, it will be notified when an entity
 * collides with the component's entity.
 */
public interface EntityCollisionListener extends Component {
    boolean handleEntityCollided(final Entity entity);
}
