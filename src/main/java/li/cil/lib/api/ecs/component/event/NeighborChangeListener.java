package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * When a component implements this interface, it will be notified when a
 * block adjacent to the entity changes. This is only supported for tile
 * entity based entities.
 */
public interface NeighborChangeListener extends Component {
    void handleNeighborChange(@Nullable final BlockPos neighborPos);
}
