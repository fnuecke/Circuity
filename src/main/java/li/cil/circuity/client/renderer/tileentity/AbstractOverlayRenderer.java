package li.cil.circuity.client.renderer.tileentity;

import net.minecraft.client.renderer.GlStateManager;
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

        final Overlay overlay = getOverlay(tileEntity);
        if (overlay == null) {
            return;
        }

        manager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        final TextureMap atlas = (TextureMap) manager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        this.func_190053_a(true);
        OverlayRenderer.renderOverlay(atlas, overlay);
        this.func_190053_a(false);
        GlStateManager.popMatrix();
    }

    @Nullable
    protected abstract Overlay getOverlay(final T tileEntity);
}
