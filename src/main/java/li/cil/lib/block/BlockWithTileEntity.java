package li.cil.lib.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class BlockWithTileEntity extends Block {
    private BiFunction<World, IBlockState, ? extends TileEntity> tileEntityFactory;

    // --------------------------------------------------------------------- //

    public BlockWithTileEntity(final Material material, final MapColor mapColor) {
        super(material, mapColor);
    }

    public BlockWithTileEntity(final Material material) {
        super(material);
    }

    // --------------------------------------------------------------------- //

    public BlockWithTileEntity setTileEntity(@Nullable final BiFunction<World, IBlockState, ? extends TileEntity> factory) {
        this.tileEntityFactory = factory;
        return this;
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
}
