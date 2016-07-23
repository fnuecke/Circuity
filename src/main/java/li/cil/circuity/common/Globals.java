package li.cil.circuity.common;

import li.cil.circuity.api.CircuityAPI;
import li.cil.circuity.common.init.Blocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public final class Globals {
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(CircuityAPI.MOD_NAME) {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(Blocks.busController);
        }
    };

    private Globals() {
    }
}
