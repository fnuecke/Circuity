package li.cil.lib.api.ecs.component.event;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * When a component implements this interface, it will be notified when a
 * block adjacent to the entity changes. This is only supported for tile
 * entity based entities.
 */
public interface NeighborChangeListener {
    void handleNeighborChange(@Nullable final BlockPos neighborPos);
}
