package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.common.ecs.component.BusControllerBlock;
import li.cil.circuity.common.tileentity.TileEntityBusController;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileEntityBusControllerRenderer extends AbstractOverlayRenderer<TileEntityBusController> {
    @Nullable
    @Override
    protected Overlay getOverlay(final TileEntityBusController tileEntity) {
        final Optional<BusControllerBlock> maybeController = tileEntity.getComponent(BusControllerBlock.class);
        if (maybeController.isPresent()) {
            final BusControllerBlock controller = maybeController.get();

            if (controller.hasErrors()) {
                return Textures.BUS_CONTROLLER_ERROR;
            } else if (controller.isOnline()) {
                return Textures.BUS_CONTROLLER_ONLINE;
            }
        }

        return null;
    }
}
