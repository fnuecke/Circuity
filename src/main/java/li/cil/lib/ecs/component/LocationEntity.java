package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.synchronization.value.SynchronizedObject;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class LocationEntity extends AbstractComponent implements Location {
    private final SynchronizedObject<Entity> parent = new SynchronizedObject<>(Entity.class);

    // --------------------------------------------------------------------- //

    public LocationEntity(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    public LocationEntity setParent(final Entity parent) {
        this.parent.set(parent);
        return this;
    }

    // --------------------------------------------------------------------- //

    public Entity getParent() {
        final Entity entity = parent.get();
        assert entity != null : "Trying to use location component before setting owner.";
        return entity;
    }

    // --------------------------------------------------------------------- //
    // Location

    @Override
    public World getWorld() {
        return getParent().getEntityWorld();
    }

    @Override
    public BlockPos getPosition() {
        return getParent().getPosition();
    }

    @Override
    public Vec3d getPositionVector() {
        return getParent().getPositionVector();
    }
}
