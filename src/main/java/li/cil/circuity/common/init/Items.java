package li.cil.circuity.common.init;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class Items {
    public static Item busController;
    public static Item busCable;
    public static Item randomAccessMemory;

    public static void init() {
        busController = GameRegistry.register(new ItemBlock(Blocks.busController).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.BUS_CONTROLLER_NAME).
                setRegistryName(Constants.BUS_CONTROLLER_NAME));

        busCable = GameRegistry.register(new ItemBlock(Blocks.busCable).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.BUS_CABLE_NAME).
                setRegistryName(Constants.BUS_CABLE_NAME));

        randomAccessMemory = GameRegistry.register(new ItemBlock(Blocks.randomAccessMemory).
                setCreativeTab(Globals.CREATIVE_TAB).
                setUnlocalizedName(Constants.RANDOM_ACCESS_MEMORY_NAME).
                setRegistryName(Constants.RANDOM_ACCESS_MEMORY_NAME));
    }

    private Items() {
    }
}
