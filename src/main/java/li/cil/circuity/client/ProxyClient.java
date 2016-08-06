package li.cil.circuity.client;

import li.cil.circuity.common.ProxyCommon;
import li.cil.circuity.common.init.Items;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public final class ProxyClient extends ProxyCommon {
    @Override
    public void preInit(final FMLPreInitializationEvent event) {
        super.preInit(event);

        for (final Item item : Items.all) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
