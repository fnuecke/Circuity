package li.cil.circuity.client.renderer.tileentity;

import li.cil.circuity.common.tileentity.TileEntityScreen;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TileEntityScreenRenderer extends TileEntitySpecialRenderer<TileEntityScreen> {
    @Override
    public void renderTileEntityAt(final TileEntityScreen te, final double x, final double y, final double z, final float partialTicks, final int destroyStage) {
        super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage);
    }
}
