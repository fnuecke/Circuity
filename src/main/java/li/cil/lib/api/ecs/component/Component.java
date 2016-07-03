package li.cil.lib.api.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;

/**
 * Basic contract for a component.
 * <p>
 * Components should never be instantiated directly. Instead, create them by
 * adding them to an entity using {@link EntityComponentManager#addComponent(long, Class)}.
 * <p>
 * For this to work, components must provide a constructor taking three arguments,
 * {@link EntityComponentManager}, <code>long</code> (entity id), <code>long</code>
 * (component id), and must store these values and return them from the respective
 * getters. In general it is recommended to build components by extending the
 * {@link li.cil.lib.ecs.component.AbstractComponent} class.
 *
 * @see EntityComponentManager
 */
public interface Component {
    /**
     * The manager of this component.
     * <p>
     * This is the manager that was used to create the component.
     *
     * @return the manager of the component.
     */
    EntityComponentManager getManager();

    /**
     * The entity this component is attached to.
     * <p>
     * This is the entity that the component was created for via a call to
     * {@link EntityComponentManager#addComponent(long, Class)}.
     *
     * @return the entity of the component.
     */
    long getEntity();

    /**
     * The ID of the component.
     * <p>
     * This is a unique identifier (in the scope of the component's manager).
     * There can be no two components in the same manager with the same ID.
     * The ID is automatically assigned during the creation of the component.
     * <p>
     * The two representations of a component (server side and client side)
     * are guaranteed to have the same ID.
     *
     * @return the ID of the component.
     */
    long getId();

    /**
     * Called from the component's manager after the component has been added.
     * <p>
     * This is called after the manager has completed adjusting its internal
     * state for the new component, but before any listeners for component
     * changes are notified that the component was added.
     * <p>
     * It is valid to destroy the component in this callback. In that case the
     * component will be cleanly removed from the manager again, and the
     * {@link EntityComponentManager#addComponent(long, Class)} call will
     * return <code>null</code>.
     */
    void onCreate();

    /**
     * Called just before the component's manager removes the component.
     * <p>
     * This means that at the time this is called the component is still fully
     * registered with the manager, allowing full access to other components
     * on the component's entity, for example.
     * <p>
     * This is called from {@link EntityComponentManager#removeComponent(Component)},
     * as well as indirectly from {@link EntityComponentManager#removeEntity(long)}.
     */
    void onDestroy();
}
