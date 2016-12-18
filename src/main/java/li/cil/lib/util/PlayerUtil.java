package li.cil.lib.util;

import li.cil.circuity.ModCircuity;
import li.cil.circuity.client.gui.GuiType;
import li.cil.lib.api.ecs.component.Component;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class PlayerUtil {
    @SideOnly(Side.CLIENT)
    public static void addLocalChatMessage(final EntityPlayer player, final ITextComponent message) {
        if (player instanceof EntityPlayerSP) {
            player.sendMessage(message);
        }
    }

    public static void openGui(final EntityPlayer player, final GuiType guiType, final int x, final int y, final int z) {
        final World world = player.getEntityWorld();
        player.openGui(ModCircuity.getInstance(), guiType.ordinal(), world, x, y, z);
    }

    public static void openGui(final EntityPlayer player, final GuiType guiType, final long data) {
        final World world = player.getEntityWorld();
        final int x = (int) (data >>> 32);
        final int y = (int) data;
        final int z = 0;
        player.openGui(ModCircuity.getInstance(), guiType.ordinal(), world, x, y, z);
    }

    public static void openGui(final EntityPlayer player, final GuiType guiType, final Component component) {
        final long componentId = component.getId();
        openGui(player, guiType, componentId);
    }

    // --------------------------------------------------------------------- //

    private PlayerUtil() {
    }
}
