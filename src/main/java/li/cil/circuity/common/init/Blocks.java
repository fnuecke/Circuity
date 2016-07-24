package li.cil.circuity.common.init;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import li.cil.circuity.common.tileentity.TileEntityBusCable;
import li.cil.circuity.common.tileentity.TileEntityBusController;
import li.cil.circuity.common.tileentity.TileEntityRandomAccessMemory;
import li.cil.circuity.common.tileentity.TileEntityRedstoneController;
import li.cil.lib.block.BlockEntityContainer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class Blocks {
    public static Block busController;
    public static Block busCable;
    public static Block randomAccessMemory;
    public static Block redstoneController;

    public static void init() {
        busController = GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> new TileEntityBusController()).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.BUS_CONTROLLER_NAME).
                setRegistryName(Constants.BUS_CONTROLLER_NAME));
        GameRegistry.registerTileEntity(TileEntityBusController.class, Constants.BUS_CONTROLLER_NAME);

        busCable = GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> new TileEntityBusCable()).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.BUS_CABLE_NAME).
                setRegistryName(Constants.BUS_CABLE_NAME));
        GameRegistry.registerTileEntity(TileEntityBusCable.class, Constants.BUS_CABLE_NAME);

        randomAccessMemory = GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> new TileEntityRandomAccessMemory()).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.RANDOM_ACCESS_MEMORY_NAME).
                setRegistryName(Constants.RANDOM_ACCESS_MEMORY_NAME));
        GameRegistry.registerTileEntity(TileEntityRandomAccessMemory.class, Constants.RANDOM_ACCESS_MEMORY_NAME);

        redstoneController = GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> new TileEntityRedstoneController()).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.REDSTONE_CONTROLLER_NAME).
                setRegistryName(Constants.REDSTONE_CONTROLLER_NAME));
        GameRegistry.registerTileEntity(TileEntityRedstoneController.class, Constants.REDSTONE_CONTROLLER_NAME);
    }

    private Blocks() {
    }
}
