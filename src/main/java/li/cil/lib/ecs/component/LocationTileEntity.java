package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.synchronization.value.SynchronizedObject;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class LocationTileEntity extends AbstractComponent implements Location {
    private final SynchronizedObject<TileEntity> parent = new SynchronizedObject<>(TileEntity.class);

    // --------------------------------------------------------------------- //

    public LocationTileEntity(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    public LocationTileEntity setParent(final TileEntity parent) {
        this.parent.set(parent);
        return this;
    }

    // --------------------------------------------------------------------- //

    public TileEntity getParent() {
        final TileEntity tileEntity = parent.get();
        assert tileEntity != null : "Trying to use transform component before setting owner.";
        return tileEntity;
    }

    // --------------------------------------------------------------------- //
    // Location

    @Override
    public World getWorld() {
        return getParent().getWorld();
    }

    @Override
    public BlockPos getPosition() {
        return getParent().getPos();
    }

    @Override
    public Vec3d getPositionVector() {
        final BlockPos pos = getPosition();
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
