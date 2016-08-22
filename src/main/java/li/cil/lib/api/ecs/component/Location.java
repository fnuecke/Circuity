package li.cil.lib.api.ecs.component;

import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Component providing information on the location of an entity.
 * <p>
 * Every entity is expected to have a single location component. For existing
 * implementations, see {@link li.cil.lib.ecs.component.LocationTileEntity} for
 * tile entity based locations (i.e. for entities bound to a tile entity), and
 * {@link li.cil.lib.ecs.component.LocationEntity} for entity based locations
 * (i.e. for entities bound to a Minecraft entity).
 * <p>
 * There is also {@link li.cil.lib.ecs.component.LocationChild} for nested
 * entities (e.g. for components based on the configuration of the parent
 * entity, such as machine behavior configured though the presence of certain
 * items in a parent inventory).
 *
 * @see li.cil.lib.ecs.component.LocationTileEntity
 * @see li.cil.lib.ecs.component.LocationEntity
 * @see li.cil.lib.ecs.component.LocationChild
 */
public interface Location extends Component {
    /**
     * The world the component's entity resides in.
     * <p>
     * This should be the same as what {@link li.cil.lib.Manager#getWorld(EntityComponentManager, boolean)}
     * returns for the component's manager.
     *
     * @return the world the component's entity resides in.
     */
    World getWorld();

    /**
     * The position of the component's entity in the world.
     * <p>
     * For Minecraft entities, this will be the exact position. For tile
     * entities this will be the center of their block space.
     *
     * @return the position of the component's entity in the world.
     */
    Vec3d getPositionVector();

    /**
     * The position of the component's entity in the world.
     * <p>
     * For tile entities, this will be the exact position. For entities, this
     * will the block space the entity is currently in.
     *
     * @return the position of the component's entity in the world.
     */
    BlockPos getPosition();
}
