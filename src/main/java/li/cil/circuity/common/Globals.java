package li.cil.circuity.common;

import li.cil.circuity.ModCircuity;
import li.cil.circuity.common.init.Blocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class Globals {
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(ModCircuity.MOD_NAME) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(Item.getItemFromBlock(Blocks.busController));
        }
    };

    private Globals() {
    }
}
