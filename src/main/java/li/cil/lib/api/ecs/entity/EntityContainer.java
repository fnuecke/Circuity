package li.cil.lib.api.ecs.entity;

import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import java.util.Optional;

/**
 * Provides access to the underlying entity associated with this container
 * object, which is typically a {@link TileEntity} or {@link Entity}.
 */
public interface EntityContainer {
    /**
     * The manager holding for the underlying entity.
     *
     * @return the manager of the entity.
     */
    EntityComponentManager getManager();

    /**
     * The entity associated with this object.
     *
     * @return the id of the entity.
     */
    long getEntity();

    /**
     * Add a component to the entity.
     *
     * @param clazz the type of the component to add.
     * @param <T>   the generic type of the component to add.
     * @return the added component.
     */
    <T extends Component> T addComponent(final Class<T> clazz);

    /**
     * Get a list of all components currently attached to the entity.
     *
     * @return the list of components of the entity.
     */
    Iterable<Component> getComponents();

    /**
     * Get a list of components currently attached to the entity having the
     * specified type or are a sub-type of the specified type.
     *
     * @param clazz the type of the components to get.
     * @param <T>   the generic type of the components to get.
     * @return the list of components of that type of the entity.
     */
    <T> Iterable<T> getComponents(final Class<T> clazz);

    /**
     * Get the first component of the specified type attached to the entity.
     *
     * @param clazz the type of the component to get.
     * @param <T>   the generic type of the component to get.
     * @return the first component of that type of the entity, if any.
     */
    <T> Optional<T> getComponent(final Class<T> clazz);
}
