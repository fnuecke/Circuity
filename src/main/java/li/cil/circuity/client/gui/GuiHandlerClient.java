package li.cil.circuity.client.gui;

import li.cil.circuity.common.ecs.component.BusDeviceScreen;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public enum GuiHandlerClient implements IGuiHandler {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        return null;
    }

    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        // In case a UI was opened for a component, its ID is encoded in the x and y arguments.
        final long componentId = (((long) x) << 32) | y;

        switch (GuiType.VALUES[id]) {
            case SCREEN: {
                final Component component = SillyBeeAPI.manager.getManager(world).getComponent(componentId);
                if (component instanceof BusDeviceScreen) {
                    final BusDeviceScreen screen = (BusDeviceScreen) component;
                    return new GuiScreenImpl(screen);
                }
                break;
            }
        }

        return null;
    }
}
