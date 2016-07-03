package li.cil.lib.api.synchronization;

import li.cil.lib.api.ecs.manager.EntityComponentManager;

import javax.annotation.Nullable;

/**
 * Client specific synchronization logic.
 */
public interface SynchronizationManagerClient extends SynchronizationManager {
    /**
     * Subscribe to synchronization updates for the specified entity.
     * <p>
     * This will create the entity with the specified ID if it doesn't exist
     * yet, and request the entity's list of components from the server, as
     * well as letting the server know to inform this client about value
     * changes in any of the entity's components.
     *
     * @param manager the manager containing the entity to subscribe to.
     * @param entity  the ID of the entity to subscribe to.
     */
    void subscribe(final EntityComponentManager manager, final long entity);

    /**
     * Unsubscribe from synchronization updates for the specified entity.
     * <p>
     * This will immediately destroy the entity on the client, as well as all
     * of its components, and inform the server to stop sending value change
     * information for any of the entity's components.
     * <p>
     * This is typically called when an entity container is unloaded, e.g. a
     * tile entity's chunk is unloaded.
     * <p>
     * Note that this is no different to simply destroying the entity via
     * the manager directly (i.e. calling {@link EntityComponentManager#removeEntity(long)}).
     * This method merely exists for symmetry with {@link #subscribe(EntityComponentManager, long)}.
     *
     * @param manager the manager containing the entity to unsubscribe from.
     * @param entity  the ID of the entity to unsubscribe from.
     */
    void unsubscribe(final EntityComponentManager manager, final long entity);

    // --------------------------------------------------------------------- //

    /**
     * Resolve a type ID used in synchronization to its actual type.
     * <p>
     * This can be useful when implementing custom {@link SynchronizedValue}s.
     * To avoid sending full types name across the network, the synchronization
     * library allows aliasing types to numeric identifiers. This list is kept
     * up-to-date on all clients automatically. When serializing, the numeric
     * ID can be obtained via {@link SynchronizationManagerServer#getTypeIdByType(Class)}
     * or {@link SynchronizationManagerServer#getTypeIdByValue(Object)}.
     * <p>
     * This returns <code>null</code> if on the server side the result of
     * querying the two aforementioned methods with <code>null</code> is sent
     * to the client (i.e. no type/unknown type).
     *
     * @param id the type ID to resolve to an actual type.
     * @return the class type represented by the specified identifier.
     */
    @Nullable
    Class getTypeByTypeId(final int id);
}
