package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.client.renderer.overlay.OverlayData;
import li.cil.circuity.client.renderer.overlay.OverlayRenderer;
import li.cil.circuity.common.ecs.component.BusControllerBlock;
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

            final long componentId = device.controllerId.get();
            final Component component = SillyBeeAPI.manager.getManager(tileEntity.getWorld()).getComponent(componentId);
            if (component instanceof BusControllerBlock) {
                final BusControllerBlock host = (BusControllerBlock) component;
                if (host.isOnline()) {
                    return Textures.RANDOM_ACCESS_MEMORY_ONLINE.get(OverlayRenderer.getPulseAlpha(device.hashCode()));
                }
            }
        }

        return null;
    }
}
