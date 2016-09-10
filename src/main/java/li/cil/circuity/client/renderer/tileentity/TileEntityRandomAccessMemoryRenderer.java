package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.component.BusDeviceHost;
import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.client.renderer.overlay.OverlayData;
import li.cil.circuity.client.renderer.overlay.OverlayRenderer;
import li.cil.circuity.common.ecs.component.BusDeviceRandomAccessMemory;
import li.cil.circuity.common.tileentity.TileEntityRandomAccessMemory;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileEntityRandomAccessMemoryRenderer extends AbstractOverlayRenderer<TileEntityRandomAccessMemory> {
    @Nullable
    @Override
    protected OverlayData getOverlay(final TileEntityRandomAccessMemory tileEntity) {
        final Optional<BusDeviceRandomAccessMemory> maybeDevice = tileEntity.getComponent(BusDeviceRandomAccessMemory.class);
        if (maybeDevice.isPresent()) {
            final BusDeviceRandomAccessMemory device = maybeDevice.get();

            // TODO This saves us from having to sync the online state for each RAM block, but given how ugly this is, that might be preferable after all...
            final long componentId = device.controllerId.get();
            final Component component = SillyBeeAPI.manager.getManager(tileEntity.getWorld()).getComponent(componentId);
            if (component instanceof BusDeviceHost) {
                final BusDeviceHost host = (BusDeviceHost) component;
                final BusDevice maybeController = host.getBusDevice();
                if (maybeController instanceof BusController) {
                    final BusController controller = (BusController) maybeController;
                    if (controller.isOnline()) {
                        return Textures.RANDOM_ACCESS_MEMORY_ONLINE.get(OverlayRenderer.getPulseAlpha(device.hashCode()));
                    }
                }
            }
        }

        return null;
    }
}
