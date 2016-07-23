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

    public LocationChild setParent(final Location location) {
        this.parent.set(location.getId());
        return this;
    }

    // --------------------------------------------------------------------- //

    public Location getParent() {
        final long parentId = parent.get();
        assert parentId != 0 : "Trying to use location component before setting owner.";
        final Location location = (Location) getManager().getComponent(parentId);
        assert location != null : "Trying to use location component before setting owner.";
        return location;
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
