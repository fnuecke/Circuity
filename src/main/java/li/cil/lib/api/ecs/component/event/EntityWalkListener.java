package li.cil.lib.api.ecs.component.event;

import net.minecraft.entity.Entity;

/**
 * When a component implements this interface, it will be notified when an entity
 * walks over the component's entity.
 */
public interface EntityWalkListener {
    boolean handleEntityWalk(final Entity entity);
}
