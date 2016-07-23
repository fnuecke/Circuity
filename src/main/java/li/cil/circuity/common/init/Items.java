package li.cil.circuity.common.init;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.Globals;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class Items {
    public static Item busController;
    public static Item busCable;

    public static void init() {
        busController = GameRegistry.register(new ItemBlock(Blocks.busController).
                setCreativeTab(Globals.CREATIVE_TAB).
                setRegistryName(Constants.BUS_CONTROLLER_NAME));

        busCable = GameRegistry.register(new ItemBlock(Blocks.busCable).
                setCreativeTab(Globals.CREATIVE_TAB).
                setRegistryName(Constants.BUS_CABLE_NAME));
    }

    private Items() {
    }
}
