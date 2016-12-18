package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.client.renderer.overlay.OverlayData;
import li.cil.circuity.common.ecs.component.BusCable;
import li.cil.circuity.common.ecs.component.BusControllerBlock;
import li.cil.circuity.common.tileentity.TileEntityBusCable;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileEntityBusCableRenderer extends AbstractOverlayRenderer<TileEntityBusCable> {
    @Nullable
    @Override
    protected OverlayData getOverlay(final TileEntityBusCable tileEntity) {
        final Optional<BusCable> maybeDevice = tileEntity.getComponent(BusCable.class);
        if (maybeDevice.isPresent()) {
            final BusCable device = maybeDevice.get();

            final long componentId = device.controllerId.get();
            final Component component = SillyBeeAPI.manager.getManager(tileEntity.getWorld()).getComponent(componentId);
            if (component instanceof BusControllerBlock) {
                final BusControllerBlock host = (BusControllerBlock) component;
                if (host.isOnline() && !host.hasErrors()) {
                    return Textures.BUS_CABLE_ONLINE.get();
                }
            }
        }

        return null;
    }
}
