package li.cil.lib.util;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class PlayerUtil {
    @SideOnly(Side.CLIENT)
    public static void addLocalChatMessage(final EntityPlayer player, final ITextComponent message) {
        if (player instanceof EntityPlayerSP) {
            player.sendMessage(message);
        }
    }

    public static ItemStack getHeldItemOfType(final EntityPlayer player, final Item filter) {
        return getHeldItemOfType(player, new ItemStack(filter));
    }

    public static ItemStack getHeldItemOfType(final EntityPlayer player, final ItemStack filter) {
        return getHeldItemOfType(player, filter, ItemStack::isItemEqual);
    }

    public static ItemStack getHeldItemOfType(final EntityPlayer player, final ItemStack filter, final StackComparator comparator) {
        final ItemStack mainStack = player.getHeldItemMainhand();
        if (comparator.compare(mainStack, filter)) {
            return mainStack;
        }

        final ItemStack offStack = player.getHeldItemOffhand();
        if (comparator.compare(offStack, filter)) {
            return offStack;
        }

        return ItemStack.EMPTY;
    }

    // --------------------------------------------------------------------- //

    @FunctionalInterface
    public interface StackComparator {
        boolean compare(final ItemStack stackA, final ItemStack stackB);
    }

    // --------------------------------------------------------------------- //

    private PlayerUtil() {
    }
}
