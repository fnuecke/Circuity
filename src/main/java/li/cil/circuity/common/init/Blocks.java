package li.cil.circuity.common.init;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import li.cil.circuity.common.tileentity.TileEntityBusCable;
import li.cil.circuity.common.tileentity.TileEntityBusController;
import li.cil.lib.block.BlockEntityContainer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class Blocks {
    public static Block busController;
    public static Block busCable;

    public static void init() {
        busController = GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> new TileEntityBusController()).
                setCreativeTab(Globals.CREATIVE_TAB).
                setRegistryName(Constants.BUS_CONTROLLER_NAME));
        GameRegistry.registerTileEntity(TileEntityBusController.class, Constants.BUS_CONTROLLER_NAME);

        busCable = GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> new TileEntityBusCable()).
                setCreativeTab(Globals.CREATIVE_TAB).
                setRegistryName(Constants.BUS_CABLE_NAME));
        GameRegistry.registerTileEntity(TileEntityBusCable.class, Constants.BUS_CABLE_NAME);
    }

    private Blocks() {
    }
}
