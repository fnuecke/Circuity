package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.client.renderer.overlay.OverlayData;
import li.cil.circuity.client.renderer.overlay.OverlayRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public abstract class AbstractOverlayRenderer<T extends TileEntity> extends TileEntitySpecialRenderer<T> {
    @Override
    public void renderTileEntityAt(final T tileEntity, final double x, final double y, final double z, final float partialTicks, final int destroyStage) {
        final TextureManager manager = this.rendererDispatcher.renderEngine;
        if (manager == null) {
            return;
        }

        final OverlayData overlay = getOverlay(tileEntity);
        if (overlay == null || overlay.alpha < 0.01f) {
            return;
        }

        manager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        final TextureMap atlas = (TextureMap) manager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        setLightmapDisabled(true);
        RenderHelper.disableStandardItemLighting();
        OverlayRenderer.renderOverlay(atlas, overlay);
        RenderHelper.enableStandardItemLighting();
        setLightmapDisabled(false);
        GlStateManager.popMatrix();
    }

    @Nullable
    protected abstract OverlayData getOverlay(final T tileEntity);
}
