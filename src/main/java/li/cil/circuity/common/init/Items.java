package li.cil.circuity.common.init;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import li.cil.circuity.common.item.ItemEEPROM;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.List;

public final class Items {
    public static List<Item> all = new ArrayList<>();

    public static Item busCable;
    public static Item busController;
    public static Item eeprom;
    public static Item eepromReader;
    public static Item processorI8080;
    public static Item randomAccessMemory;
    public static Item redstoneController;

    public static void init() {
        busCable = register(Blocks.busCable);
        busController = register(Blocks.busController);
        eeprom = register(new ItemEEPROM(), Constants.EEPROM_NAME);
        eepromReader = register(Blocks.eepromReader);
        processorI8080 = register(Blocks.processorI8080);
        randomAccessMemory = register(Blocks.randomAccessMemory);
        redstoneController = register(Blocks.redstoneController);
    }

    private static Item register(final Item item, final String name) {
        all.add(item);

        return GameRegistry.register(item.
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(name).
                setRegistryName(name));
    }

    private static Item register(final Block block) {
        return register(new ItemBlock(block), block.getRegistryName().getResourcePath());
    }

    private Items() {
    }
}
