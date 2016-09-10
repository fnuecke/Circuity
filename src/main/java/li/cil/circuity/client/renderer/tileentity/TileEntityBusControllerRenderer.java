package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.client.renderer.overlay.OverlayData;
import li.cil.circuity.common.ecs.component.BusControllerBlock;
import li.cil.circuity.common.tileentity.TileEntityBusController;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileEntityBusControllerRenderer extends AbstractOverlayRenderer<TileEntityBusController> {
    @Nullable
    @Override
    protected OverlayData getOverlay(final TileEntityBusController tileEntity) {
        final Optional<BusControllerBlock> maybeDevice = tileEntity.getComponent(BusControllerBlock.class);
        if (maybeDevice.isPresent()) {
            final BusControllerBlock device = maybeDevice.get();

            if (device.hasErrors()) {
                return Textures.BUS_CONTROLLER_ERROR.get();
            } else if (device.isOnline()) {
                return Textures.BUS_CONTROLLER_ONLINE.get();
            }
        }

        return null;
    }
}
