package li.cil.circuity.common.init;

import li.cil.circuity.ModCircuity;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import li.cil.circuity.common.item.ItemEEPROM;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class Items {
    public static Item busCable;
    public static Item busController;
    public static Item eeprom;
    public static Item eepromReader;
    public static Item processorZ80;
    public static Item randomAccessMemory;
    public static Item redstoneController;
    public static Item serialConsole;

    public static void init() {
        busCable = register(Blocks.busCable);
        busController = register(Blocks.busController);
        eeprom = register(new ItemEEPROM(), Constants.EEPROM_NAME);
        eepromReader = register(Blocks.eepromReader);
        processorZ80 = register(Blocks.processorZ80);
        randomAccessMemory = register(Blocks.randomAccessMemory);
        redstoneController = register(Blocks.redstoneController);
        serialConsole = register(Blocks.serialConsole);
    }

    private static Item register(final Block block) {
        return register(new ItemBlock(block), block.getRegistryName().getResourcePath());
    }

    private static Item register(final Item item, final String name) {
        item.setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(name).
                setRegistryName(name);
        GameRegistry.register(item);
        ModCircuity.getProxy().handleRegisterItem(item);
        return item;
    }

    private Items() {
    }
}
