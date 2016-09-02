package li.cil.circuity.client.renderer;

import li.cil.circuity.api.CircuityAPI;
import li.cil.circuity.client.renderer.tileentity.Overlay;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public enum Textures {
    INSTANCE;

    // --------------------------------------------------------------------- //

    /**
     * Collected list of all additional textures that should be loaded into the
     * block texture atlas. For looping over them in the stitch event.
     */
    private static final List<ResourceLocation> ALL_LOCATIONS = new ArrayList<>();

    private static final String OVERLAY_PATH = "blocks/overlay/";

    // --------------------------------------------------------------------- //

    public static final Overlay BUS_CONTROLLER_ERROR = new Overlay(
            add(OVERLAY_PATH + "bus_controller_top_error"),
            add(OVERLAY_PATH + "bus_controller_side_error"),
            add(OVERLAY_PATH + "bus_controller_bottom_error")
    );

    public static final Overlay BUS_CONTROLLER_ONLINE = new Overlay(
            add(OVERLAY_PATH + "bus_controller_top_online"),
            add(OVERLAY_PATH + "bus_controller_side_online"),
            add(OVERLAY_PATH + "bus_controller_bottom_online")
    );

    public static final Overlay PROCESSOR_Z80_ONLINE = new Overlay(
            add(OVERLAY_PATH + "processor_z80_top_online"),
            add(OVERLAY_PATH + "processor_z80_side_online"),
            null
    );

    public static final Overlay RANDOM_ACCESS_MEMORY_ONLINE = new Overlay(
            add(OVERLAY_PATH + "random_access_memory_top_online"),
            add(OVERLAY_PATH + "random_access_memory_side_online"),
            add(OVERLAY_PATH + "random_access_memory_bottom_online")
    );

    // --------------------------------------------------------------------- //

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onTextureStitchPre(final TextureStitchEvent.Pre event) {
        ALL_LOCATIONS.forEach(event.getMap()::registerSprite);
    }

    // --------------------------------------------------------------------- //

    private static ResourceLocation add(final String path) {
        final ResourceLocation location = new ResourceLocation(CircuityAPI.MOD_ID, path);
        ALL_LOCATIONS.add(location);
        return location;
    }
}
