package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.common.ecs.component.BusDeviceScreen;
import li.cil.circuity.common.tileentity.TileEntityScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

import java.util.Optional;

public class TileEntityScreenRenderer extends TileEntitySpecialRenderer<TileEntityScreen> {
    @Override
    public void renderTileEntityAt(final TileEntityScreen tileEntity, final double x, final double y, final double z, final float partialTicks, final int destroyStage) {
        final Optional<BusDeviceScreen> maybeScreen = tileEntity.getComponent(BusDeviceScreen.class);
        if (maybeScreen.isPresent()) {
            final BusDeviceScreen screen = maybeScreen.get();
            final ScreenRenderer renderer = screen.getScreenRenderer();
            if (renderer != null) {
                GlStateManager.pushMatrix();
                this.func_190053_a(true);
                GlStateManager.disableCull();

                GlStateManager.translate(x, y, z);
                GlStateManager.translate(0, 2, 0.5f);
                GlStateManager.scale(1, -1, 1);
                GlStateManager.scale(1 / 256f, 1 / 256f, 1 / 256f);

                renderer.render(256, 256);

                super.renderTileEntityAt(tileEntity, x, y, z, partialTicks, destroyStage);

                GlStateManager.enableCull();
                this.func_190053_a(false);
                GlStateManager.popMatrix();
            }
        }
    }
}
