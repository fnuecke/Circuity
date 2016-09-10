package li.cil.circuity.client;

import li.cil.circuity.ModCircuity;
import li.cil.circuity.client.gui.GuiHandlerClient;
import li.cil.circuity.client.renderer.Textures;
import li.cil.circuity.client.renderer.tileentity.TileEntityBusControllerRenderer;
import li.cil.circuity.client.renderer.tileentity.TileEntityRandomAccessMemoryRenderer;
import li.cil.circuity.client.renderer.tileentity.TileEntityScreenRenderer;
import li.cil.circuity.common.ProxyCommon;
import li.cil.circuity.common.tileentity.TileEntityBusController;
import li.cil.circuity.common.tileentity.TileEntityRandomAccessMemory;
import li.cil.circuity.common.tileentity.TileEntityScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@SuppressWarnings("unused")
public final class ProxyClient extends ProxyCommon {
    @Override
    public void preInit(final FMLPreInitializationEvent event) {
        super.preInit(event);

        Textures.init();

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBusController.class, new TileEntityBusControllerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRandomAccessMemory.class, new TileEntityRandomAccessMemoryRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityScreen.class, new TileEntityScreenRenderer());
    }

    @Override
    public void init(final FMLInitializationEvent event) {
        super.init(event);

        NetworkRegistry.INSTANCE.registerGuiHandler(ModCircuity.getInstance(), GuiHandlerClient.INSTANCE);
    }

    @Override
    public void handleRegisterItem(final Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
