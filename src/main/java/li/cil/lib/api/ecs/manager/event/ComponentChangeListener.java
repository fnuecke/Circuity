package li.cil.lib.api.ecs.manager.event;

import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.synchronization.SynchronizationManager;

/**
 * Interface for component change listeners that can be registered with an
 * {@link EntityComponentManager}. The only internal use for this is to
 * notify the {@link SynchronizationManager}s of changes to the entity
 * component system they are responsible for.
 */
public interface ComponentChangeListener {
    void onComponentAdded(final Component component);

    void onComponentRemoved(final Component component);
}
