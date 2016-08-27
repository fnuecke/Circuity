package li.cil.circuity.server.processor.z80;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.ecs.component.AbstractComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

final class TestLocation extends AbstractComponent implements Location {
    private World world;

    public TestLocation(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    @Override
    public World getWorld() {
        return world;
    }

    public TestLocation setWorld(final World world) {
        this.world = world;
        return this;
    }

    @Override
    public Vec3d getPositionVector() {
        return Vec3d.ZERO;
    }

    @Override
    public BlockPos getPosition() {
        return BlockPos.ORIGIN;
    }
}
