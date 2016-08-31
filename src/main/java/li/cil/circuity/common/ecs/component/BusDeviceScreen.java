package li.cil.circuity.common.ecs.component;

import li.cil.circuity.ModCircuity;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.AbstractAddressable;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.AddressHint;
import li.cil.circuity.api.bus.device.BusChangeListener;
import li.cil.circuity.api.bus.device.DeviceInfo;
import li.cil.circuity.api.bus.device.DeviceType;
import li.cil.circuity.api.bus.device.ScreenRenderer;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedUUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.UUID;

public class BusDeviceScreen extends AbstractComponentBusDevice implements ActivationListener {
    @Serialize
    private final ScreenImpl device = new ScreenImpl();
    @Serialize
    private final SynchronizedUUID rendererId = new SynchronizedUUID();

    public static final int SCREEN_RES_PIXELS_W = 320;
    public static final int SCREEN_RES_PIXELS_H = 200;

    private final ScreenGuiImpl screenGui = new ScreenGuiImpl();

    // --------------------------------------------------------------------- //

    public BusDeviceScreen(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    @Nullable
    public ScreenRenderer getScreenRenderer() {
        final UUID id = rendererId.get();
        if (id != null) {
            final Object renderer = SillyBeeAPI.globalObjects.get(getWorld(), id);
            if (renderer instanceof ScreenRenderer) {
                return (ScreenRenderer) renderer;
            } else if (renderer != null) {
                ModCircuity.getLogger().warn("Got an incompatible type retrieving ScreenRenderer. UUID collision?");
            }
        }
        return null;
    }

    // --------------------------------------------------------------------- //
    // AbstractComponent

    @Override
    public void onDestroy() {
        super.onDestroy();

        final UUID id = rendererId.get();
        if (id != null) {
            SillyBeeAPI.globalObjects.remove(getWorld(), id);
        }
    }

    // ActivationListener

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if(player.isSneaking()) {
            return false;
        }

        final World world = getWorld();
        if (world.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(screenGui);
            //PlayerUtil.addLocalChatMessage(player, new TextComponentString("There should be a GUI here."));
        }
        return true;
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getDevice() {
        return device;
    }

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SCREEN, Constants.DeviceInfo.SCREEN_NAME);

    public final class ScreenGuiImpl extends GuiScreen {
        @Override
        public void initGui() {
            super.initGui();
        }

        @Override
        public void onGuiClosed() {
            super.onGuiClosed();
        }

        @Override
        public void drawScreen(int mx, int my, float unk1) {
            super.drawScreen(mx, my, unk1);

            // SETTINGS
            final int windowGapSize = 5;
            final int screenBorderSize = 10;
            final int innerBorderSize = 5;

            // Get GUI dimensions and centre
            int sw = this.width;
            int sh = this.height;
            int scx = sw/2;
            int scy = sh/2;

            // Get inner size
            int innerW = sw-windowGapSize-screenBorderSize-innerBorderSize;
            int innerH = sh-windowGapSize-screenBorderSize-innerBorderSize;

            // Shrink to fit aspect ratio
            if(innerW*SCREEN_RES_PIXELS_H > innerH*SCREEN_RES_PIXELS_W) {
                innerW = (innerH*SCREEN_RES_PIXELS_W)/SCREEN_RES_PIXELS_H;
            } else {
                innerH = (innerW*SCREEN_RES_PIXELS_H)/SCREEN_RES_PIXELS_W;
            }

            // Get screen rect size
            int innerBorderW = innerW + innerBorderSize;
            int innerBorderH = innerH + innerBorderSize;

            // Get outer size
            int outerW = innerBorderW + screenBorderSize;
            int outerH = innerBorderH + screenBorderSize;

            // Set up GL state
            GlStateManager.pushAttrib();
            GlStateManager.disableTexture2D();
            GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);

            // Start drawing frame
            GlStateManager.glBegin(GL11.GL_QUADS);

            // Outer rectangle
            GlStateManager.color(0.8f, 0.7f, 0.6f);
            GlStateManager.glVertex3f( scx-outerW/2f, scy-outerH/2f, 0f);
            GlStateManager.glVertex3f( scx-outerW/2f, scy+outerH/2f, 0f);
            GlStateManager.glVertex3f( scx+outerW/2f, scy+outerH/2f, 0f);
            GlStateManager.glVertex3f( scx+outerW/2f, scy-outerH/2f, 0f);

            // Inner rectangle (actual screen)
            GlStateManager.color(0.0f, 0.0f, 0.0f);
            GlStateManager.glVertex3f( scx-innerBorderW/2f, scy-innerBorderH/2f, 0f);
            GlStateManager.glVertex3f( scx-innerBorderW/2f, scy+innerBorderH/2f, 0f);
            GlStateManager.glVertex3f( scx+innerBorderW/2f, scy+innerBorderH/2f, 0f);
            GlStateManager.glVertex3f( scx+innerBorderW/2f, scy-innerBorderH/2f, 0f);

            // Finish drawing frame
            GlStateManager.glEnd();

            // Clean up GL state
            GlStateManager.enableTexture2D();
            GlStateManager.popAttrib();

            // Draw screen
            ScreenRenderer renderer = BusDeviceScreen.this.getScreenRenderer();
            if(renderer != null) {
                GlStateManager.translate( (scx-innerW/2f),  (scy-innerH/2f), 0f);
                renderer.render(innerW, innerH);
                GlStateManager.translate(-(scx-innerW/2f), -(scy-innerH/2f), 0f);
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }


    @Serializable
    public final class ScreenImpl extends AbstractAddressable implements AddressHint, BusChangeListener {
        // --------------------------------------------------------------------- //
        // AbstractAddressable

        @Override
        protected AddressBlock validateAddress(final AddressBlock memory) {
            return memory.take(Constants.SCREEN_ADDRESS, 1);
        }

        // --------------------------------------------------------------------- //
        // Addressable

        @Nullable
        @Override
        public DeviceInfo getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Override
        public int read(final int address) {
            return 0;
        }

        @Override
        public void write(final int address, final int value) {
        }

        // --------------------------------------------------------------------- //
        // AddressHint

        @Override
        public int getSortHint() {
            return Constants.SCREEN_ADDRESS;
        }

        // --------------------------------------------------------------------- //
        // BusChangeListener

        @Override
        public void handleBusChanged() {
            // TODO Build list of candidates, allow user to select current one.
            for (final BusDevice device : controller.getDevices()) {
                if (device instanceof ScreenRenderer) {
                    final ScreenRenderer renderer = (ScreenRenderer) device;
                    BusDeviceScreen.this.rendererId.set(renderer.getPersistentId());
                    return;
                }
            }
        }
    }
}
