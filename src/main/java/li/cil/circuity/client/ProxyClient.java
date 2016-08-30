package li.cil.circuity.client;

import li.cil.circuity.client.renderer.tileentity.TileEntityScreenRenderer;
import li.cil.circuity.common.ProxyCommon;
import li.cil.circuity.common.tileentity.TileEntityScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@SuppressWarnings("unused")
public final class ProxyClient extends ProxyCommon {
    @Override
    public void preInit(final FMLPreInitializationEvent event) {
        super.preInit(event);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityScreen.class, new TileEntityScreenRenderer());
    }

    @Override
    public void handleRegisterItem(final Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
