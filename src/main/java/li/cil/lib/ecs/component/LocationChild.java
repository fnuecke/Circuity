package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.synchronization.value.SynchronizedLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class LocationChild extends AbstractComponent implements Location {
    private final SynchronizedLong parent = new SynchronizedLong();

    // --------------------------------------------------------------------- //

    public LocationChild(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    public LocationChild setParent(final Location transform) {
        this.parent.set(transform.getId());
        return this;
    }

    // --------------------------------------------------------------------- //

    public Location getParent() {
        final long parentId = parent.get();
        assert parentId != 0 : "Trying to use transform component before setting owner.";
        final Location transform = (Location) getManager().getComponent(parentId);
        assert transform != null : "Trying to use transform component before setting owner.";
        return transform;
    }

    // --------------------------------------------------------------------- //
    // Location

    @Override
    public World getWorld() {
        return getParent().getWorld();
    }

    @Override
    public Vec3d getPositionVector() {
        return getParent().getPositionVector();
    }

    @Override
    public BlockPos getPosition() {
        return getParent().getPosition();
    }
}
