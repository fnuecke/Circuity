package li.cil.lib.api;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Access to entity component managers.
 */
public interface ManagerAPI {
    /**
     * Get a manager for the specified world.
     * <p>
     * If no manager exists for the specified world it will be created.
     *
     * @param world the world to get the manager for.
     * @return the manager for the specified world.
     */
    EntityComponentManager getManager(final World world);

    /**
     * Get the world a manager belongs to.
     * <p>
     * This will typically not return <code>null</code>, but may do so for dead
     * managers, i.e. managers for which the world no longer exists, but that
     * are still referenced from somewhere else in some way.
     *
     * @param manager the manager to get the world for.
     * @return the world the manager belongs to.
     */
    @Nullable
    World getWorld(final EntityComponentManager manager);
}
