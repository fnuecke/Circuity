package li.cil.lib.util;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
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

    // --------------------------------------------------------------------- //

    private PlayerUtil() {
    }
}
