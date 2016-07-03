package li.cil.lib.api.ecs.manager.event;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.synchronization.SynchronizationManager;

/**
 * Interface for component change listeners that can be registered with an
 * {@link EntityComponentManager}. The only internal use for this is to
 * notify the {@link SynchronizationManager}s of changes to the entity
 * component system they are responsible for.
 */
public interface EntityChangeListener {
    void onEntityAdded(final EntityComponentManager manager, final long entity);

    void onEntityRemoved(final EntityComponentManager manager, final long entity);
}
