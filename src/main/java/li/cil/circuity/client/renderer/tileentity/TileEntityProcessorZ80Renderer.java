package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.client.renderer.overlay.OverlayData;
import li.cil.circuity.client.renderer.overlay.OverlayRenderer;
import li.cil.circuity.common.ecs.component.BusControllerBlock;
import li.cil.circuity.common.ecs.component.BusDeviceZ80Processor;
import li.cil.circuity.common.tileentity.TileEntityProcessorZ80;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;

import javax.annotation.Nullable;
import java.util.Optional;

public class TileEntityProcessorZ80Renderer extends AbstractOverlayRenderer<TileEntityProcessorZ80> {
    @Nullable
    @Override
    protected OverlayData getOverlay(final TileEntityProcessorZ80 tileEntity) {
        final Optional<BusDeviceZ80Processor> maybeDevice = tileEntity.getComponent(BusDeviceZ80Processor.class);
        if (maybeDevice.isPresent()) {
            final BusDeviceZ80Processor device = maybeDevice.get();

            // TODO This saves us from having to sync the online state for each RAM block, but given how ugly this is, that might be preferable after all...
            final long componentId = device.controllerId.get();
            final Component component = SillyBeeAPI.manager.getManager(tileEntity.getWorld()).getComponent(componentId);
            if (component instanceof BusControllerBlock) {
                final BusControllerBlock host = (BusControllerBlock) component;
                if (host.isOnline()) {
                    return Textures.PROCESSOR_Z80_ONLINE.get(OverlayRenderer.getPulseAlpha(device.hashCode()));
                }
            }
        }

        return null;
    }
}
