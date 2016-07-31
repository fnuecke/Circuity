package li.cil.lib.api.ecs.manager;

import li.cil.lib.Manager;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.event.ComponentChangeListener;
import li.cil.lib.api.ecs.manager.event.EntityChangeListener;
import li.cil.lib.api.synchronization.SynchronizationManagerClient;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A manager keeping track of a list of entities and their components.
 * <p>
 * Typically there's one manager per world. To obtain an instance of a manager,
 * use the methods available in the {@link Manager} class.
 * <p>
 * Note that entities and components may only be created and removed on the
 * server side. To create an entity client side manually (this is automatically
 * done when using one of the built-in entity containers), subscribe to the
 * entity using {@link SynchronizationManagerClient#subscribe(EntityComponentManager, long)}.
 * This will then automatically create all components and synchronize their
 * state to the client. Note that this happens asynchronously, i.e the components
 * will not exist right after that method returns, as this data has to be
 * retrieved from the server. To unsubscribe, simply destroy the entity.
 * Alternatively use {@link SynchronizationManagerClient#unsubscribe(EntityComponentManager, long)},
 * which in turn simply destroys the entity, and only exists for symmetry.
 * <p>
 * In general, it is recommended to use the built-in entity containers and
 * let them take care of initializing synchronization of their entities. In the
 * case of nested entities, however, you will always need to initiate and stop
 * synchronization manually.
 *
 * @see Manager
 */
@SuppressWarnings("unused")
public interface EntityComponentManager {
    /**
     * Create a new entity.
     *
     * @return the ID of the created entity.
     * @throws UnsupportedOperationException when invoked on the client side.
     */
    long addEntity() throws UnsupportedOperationException;

    /**
     * Get whether an entity with the specified ID exists.
     *
     * @param entity the entity to check.
     * @return <code>true</code> if the entity exists; <code>false</code> otherwise.
     */
    boolean hasEntity(final long entity);

    /**
     * Destroys an entity and all of its components.
     * <p>
     * When called on the client side, this will automatically unsubscribe
     * the client from synchronization messages for the entity.
     *
     * @param entity the entity to destroy.
     * @return <code>true</code> if the entity existed; <code>false</code> otherwise.
     */
    boolean removeEntity(final long entity);

    // --------------------------------------------------------------------- //

    /**
     * Add a component to an entity.
     * <p>
     * Creates a new instance of the specified component type. The type must
     * provide a constructor of the correct format, as described in the documentation
     * of the {@link Component} interface.
     * <p>
     * Note that this may return an invalid component in the specific case that
     * the component destroys itself in its {@link Component#onCreate()}
     * callback. A component may choose to do so when certain preconditions for
     * its existence are not met. Typically this will not be the case though.
     *
     * @param entity the entity to add the component to.
     * @param clazz  the type of the component to create.
     * @param <T>    the generic type of the component to create.
     * @return the newly created component.
     * @throws UnsupportedOperationException when invoked on the client side.
     */
    <T extends Component> T addComponent(final long entity, final Class<T> clazz) throws UnsupportedOperationException;

    /**
     * Get whether a component with the specified ID exists.
     *
     * @param component the component to check.
     * @return <code>true</code> if the component exists; <code>false</code> otherwise.
     */
    boolean hasComponent(final long component);

    /**
     * Get whether the specified component exists in this manager.
     *
     * @param component the component to check.
     * @return <code>true</code> if the component exists; <code>false</code> otherwise.
     */
    boolean hasComponent(final Component component);

    /**
     * Destroy a component.
     * <p>
     * This will remove the component from its entity and call the component's
     * {@link Component#onDestroy()} callback.
     *
     * @param component the component to destroy.
     * @return <code>true</code> if the component was still valid; <code>false</code> otherwise.
     * @throws UnsupportedOperationException when invoked on the client side.
     */
    boolean removeComponent(final Component component) throws UnsupportedOperationException;

    // --------------------------------------------------------------------- //

    /**
     * Get a component by its ID.
     *
     * @param id the ID of the component to get.
     * @return the component, or <code>null</code> if no such component exists.
     */
    @Nullable
    Component getComponent(final long id);

    /**
     * Get a list of <em>all</em> components (on all entities) of the specified type.
     * <p>
     * The specified type may be any superclass of the components to retrieve,
     * including interfaces.
     *
     * @param clazz the type of the components to get.
     * @param <T>   the generic type of the components to get.
     * @return the list of components of the specified type.
     */
    <T> Iterable<T> getComponents(final Class<T> clazz);

    /**
     * Get a component of an entity by its type.
     * <p>
     * The specified type may be any superclass of the component to retrieve,
     * including interfaces.
     *
     * @param entity the entity to get the component of.
     * @param clazz  the type of the component to get.
     * @param <T>    the generic type of the component to get.
     * @return the first component matching the specified type, or none.
     */
    <T> Optional<T> getComponent(final long entity, final Class<T> clazz);

    /**
     * Get a list of components of the specified type on the specified entity.
     *
     * @param entity the entity to get the components of.
     * @param clazz  the type of the components to get.
     * @param <T>    the generic type of the components to get.
     * @return the list of components of the specified type on the entity.
     */
    <T> Stream<T> getComponents(final long entity, final Class<T> clazz);

    /**
     * Get a list of all components on the specified entity.
     *
     * @param entity the entity to get the components of.
     * @return the list of components on the specified entity.
     */
    Iterable<Component> getComponents(final long entity);

    // --------------------------------------------------------------------- //

    /**
     * Register an entity event listener to get notified of added and removed entities.
     *
     * @param listener the listener to add.
     */
    void addEntityChangeListener(final EntityChangeListener listener);

    /**
     * Remove an entity event listener registered via {@link #addEntityChangeListener(EntityChangeListener)}.
     *
     * @param listener the listener to remove.
     * @return <code>true</code> if the listener was registered; <code>false</code> otherwise.
     */
    boolean removeEntityChangeListener(final EntityChangeListener listener);

    /**
     * Register a component event listener to get notified of added and removed components.
     *
     * @param listener the listener to add.
     */
    void addComponentChangeListener(final ComponentChangeListener listener);

    /**
     * Remove a component event listener registered via {@link #addComponentChangeListener(ComponentChangeListener)}.
     *
     * @param listener the listener to remove.
     * @return <code>true</code> if the listener was registered; <code>false</code> otherwise.
     */
    boolean removeComponentChangeListener(final ComponentChangeListener listener);
}
