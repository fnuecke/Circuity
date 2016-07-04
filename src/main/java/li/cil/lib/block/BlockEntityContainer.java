package li.cil.lib.block;

import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.component.event.ClickListener;
import li.cil.lib.api.ecs.component.event.EntityCollisionListener;
import li.cil.lib.api.ecs.component.event.EntityWalkListener;
import li.cil.lib.api.ecs.component.event.NeighborChangeListener;
import li.cil.lib.ecs.entity.EntityContainer;
import li.cil.lib.tileentity.TileEntityEntityContainer;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class BlockEntityContainer extends Block {
    private BiFunction<World, IBlockState, ? extends TileEntityEntityContainer> tileEntityFactory;

    // --------------------------------------------------------------------- //

    public BlockEntityContainer(final Material material, final MapColor mapColor) {
        super(material, mapColor);
    }

    public BlockEntityContainer(final Material material) {
        super(material);
    }

    // --------------------------------------------------------------------- //

    public BlockEntityContainer setTileEntity(@Nullable final BiFunction<World, IBlockState, ? extends TileEntityEntityContainer> factory) {
        this.tileEntityFactory = factory;
        return this;
    }

    // --------------------------------------------------------------------- //
    // Facilitated component access

    public static <T> Optional<T> getComponent(final IBlockAccess world, final BlockPos pos, final Class<T> clazz) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof EntityContainer) {
            final EntityContainer container = (EntityContainer) tileEntity;
            return container.getComponent(clazz);
        }
        return Optional.empty();
    }

    public static <T> Stream<T> getComponents(final IBlockAccess world, final BlockPos pos, final Class<T> clazz) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof EntityContainer) {
            final EntityContainer container = (EntityContainer) tileEntity;
            return container.getComponents(clazz);
        }
        return Stream.empty();
    }

    // --------------------------------------------------------------------- //
    // TileEntity creation

    @Override
    public boolean hasTileEntity(final IBlockState state) {
        return tileEntityFactory != null;
    }

    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        if (tileEntityFactory != null) {
            return tileEntityFactory.apply(world, state);
        }
        return super.createTileEntity(world, state);
    }

    // --------------------------------------------------------------------- //
    // Miscellaneous properties

    @Override
    public boolean canBeReplacedByLeaves(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        return false;
    }

    @Override
    public boolean isBeaconBase(final IBlockAccess world, final BlockPos pos, final BlockPos beacon) {
        return super.isBeaconBase(world, pos, beacon); // TODO
    }

    // --------------------------------------------------------------------- //
    // Redstone logic

    @Override
    public boolean canConnectRedstone(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        final Optional<Redstone> redstone = getComponent(world, pos, Redstone.class);
        return redstone.isPresent();
    }

    @Override
    public boolean canProvidePower(final IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        final Optional<Redstone> redstone = getComponent(world, pos, Redstone.class);
        return redstone.flatMap(rs -> Optional.of(rs.getOutput(side))).orElse(super.getWeakPower(state, world, pos, side));
    }

    // --------------------------------------------------------------------- //
    // Events

    @Override
    public void onNeighborChange(final IBlockAccess world, final BlockPos pos, final BlockPos neighbor) {
        final Stream<NeighborChangeListener> listeners = getComponents(world, pos, NeighborChangeListener.class);
        listeners.forEach(listener -> listener.onNeighborChange(neighbor));
        super.onNeighborChange(world, pos, neighbor);
    }

    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state, final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final Stream<ActivationListener> listeners = getComponents(world, pos, ActivationListener.class);
        return listeners.anyMatch(listener -> listener.onActivated(player, hand, heldItem, side, hitX, hitY, hitZ)) ||
                super.onBlockActivated(world, pos, state, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public void onEntityWalk(final World world, final BlockPos pos, final Entity entity) {
        final Stream<EntityWalkListener> listeners = getComponents(world, pos, EntityWalkListener.class);
        listeners.forEach(listener -> listener.onEntityWalk(entity));
        super.onEntityWalk(world, pos, entity);
    }

    @Override
    public void onBlockClicked(final World world, final BlockPos pos, final EntityPlayer player) {
        final Stream<ClickListener> listeners = getComponents(world, pos, ClickListener.class);
        listeners.forEach(listener -> listener.onClicked(player));
        super.onBlockClicked(world, pos, player);
    }

    @Override
    public void onEntityCollidedWithBlock(final World world, final BlockPos pos, final IBlockState state, final Entity entity) {
        final Stream<EntityCollisionListener> listeners = getComponents(world, pos, EntityCollisionListener.class);
        listeners.forEach(listener -> listener.onEntityCollided(entity));
        super.onEntityCollidedWithBlock(world, pos, state, entity);
    }

    // --------------------------------------------------------------------- //
    // Rotation

    @Override
    public boolean rotateBlock(final World world, final BlockPos pos, final EnumFacing axis) {
        return super.rotateBlock(world, pos, axis); // TODO
    }

    @Override
    public EnumFacing[] getValidRotations(final World world, final BlockPos pos) {
        return super.getValidRotations(world, pos); // TODO
    }

    // --------------------------------------------------------------------- //
    // Coloring

    @Override
    public boolean recolorBlock(final World world, final BlockPos pos, final EnumFacing side, final EnumDyeColor color) {
        return super.recolorBlock(world, pos, side, color); // TODO
    }

    // --------------------------------------------------------------------- //
    // Tooltip

    @Override
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced); // TODO
    }
}
