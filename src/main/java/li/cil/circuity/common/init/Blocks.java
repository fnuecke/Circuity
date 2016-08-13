package li.cil.circuity.common.init;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import li.cil.circuity.common.tileentity.TileEntityBusCable;
import li.cil.circuity.common.tileentity.TileEntityBusController;
import li.cil.circuity.common.tileentity.TileEntityEEPROMReader;
import li.cil.circuity.common.tileentity.TileEntityProcessorZ80;
import li.cil.circuity.common.tileentity.TileEntityRandomAccessMemory;
import li.cil.circuity.common.tileentity.TileEntityRedstoneController;
import li.cil.circuity.common.tileentity.TileEntitySerialConsole;
import li.cil.lib.block.BlockEntityContainer;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class Blocks {
    public static Block busController;
    public static Block busCable;
    public static Block eepromReader;
    public static Block processorZ80;
    public static Block randomAccessMemory;
    public static Block redstoneController;
    public static Block serialConsole;

    public static void init() {
        busCable = register(Constants.BUS_CABLE_NAME, TileEntityBusCable.class);
        busController = register(Constants.BUS_CONTROLLER_NAME, TileEntityBusController.class);
        eepromReader = register(Constants.EEPROM_READER_NAME, TileEntityEEPROMReader.class);
        processorZ80 = register(Constants.PROCESSOR_Z80_NAME, TileEntityProcessorZ80.class);
        randomAccessMemory = register(Constants.RANDOM_ACCESS_MEMORY_NAME, TileEntityRandomAccessMemory.class);
        redstoneController = register(Constants.REDSTONE_CONTROLLER_NAME, TileEntityRedstoneController.class);
        serialConsole = register(Constants.SERIAL_CONSOLE_NAME, TileEntitySerialConsole.class);
    }

    private static Block register(final String name, final Class<? extends TileEntity> tileEntityClass) {
        GameRegistry.registerTileEntity(tileEntityClass, name);

        return GameRegistry.register(new BlockEntityContainer(Material.IRON).
                setTileEntity((world, state) -> ReflectionUtil.newInstance(tileEntityClass)).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(name).
                setRegistryName(name));
    }

    private Blocks() {
    }
}
