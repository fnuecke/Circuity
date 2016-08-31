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
import li.cil.circuity.client.gui.GuiType;
import li.cil.circuity.common.Constants;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedUUID;
import li.cil.lib.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

import javax.annotation.Nullable;
import java.util.UUID;

public class BusDeviceScreen extends AbstractComponentBusDevice implements ActivationListener {
    @Serialize
    private final ScreenImpl device = new ScreenImpl();
    @Serialize
    private final SynchronizedUUID rendererId = new SynchronizedUUID();

    public static final int SCREEN_RES_PIXELS_W = 320;
    public static final int SCREEN_RES_PIXELS_H = 200;

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

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        PlayerUtil.openGui(player, GuiType.SCREEN, this);

        return true;
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getDevice() {
        return device;
    }

    public static final DeviceInfo DEVICE_INFO = new DeviceInfo(DeviceType.SCREEN, Constants.DeviceInfo.SCREEN_NAME);


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
