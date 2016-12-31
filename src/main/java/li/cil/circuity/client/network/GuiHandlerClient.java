package li.cil.circuity.client.network;

import li.cil.circuity.client.gui.GuiBlockScreen;
import li.cil.circuity.client.gui.GuiType;
import li.cil.circuity.common.ecs.component.BusDeviceScreen;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public enum GuiHandlerClient implements IGuiHandler {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Nullable
    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        // In case a UI was opened for a component, its ID is encoded in the x and y arguments.
        final long longData = (((long) x) << 32) | y;

        switch (GuiType.VALUES[id]) {
            case SCREEN: {
                final long componentId = longData;
                final Component component = SillyBeeAPI.manager.getManager(world).getComponent(componentId);
                if (component instanceof BusDeviceScreen) {
                    final BusDeviceScreen screen = (BusDeviceScreen) component;
                    return new GuiBlockScreen(screen);
                }
                break;
            }
        }

        return null;
    }
}
