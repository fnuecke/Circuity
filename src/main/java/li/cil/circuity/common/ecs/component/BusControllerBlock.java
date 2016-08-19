package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.bus.AbstractBusController;
import li.cil.lib.api.ecs.component.LateTickable;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedBoolean;
import li.cil.lib.synchronization.value.SynchronizedEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

@Serializable
public final class BusControllerBlock extends BusNeighborAware implements ITickable, LateTickable, ActivationListener {
    @Serialize
    private final BlockBusControllerImpl controller = new BlockBusControllerImpl();

    private final SynchronizedEnum<AbstractBusController.State> state = new SynchronizedEnum<>(AbstractBusController.State.class);
    private final SynchronizedBoolean isOnline = new SynchronizedBoolean();

    private Redstone redstone;

    // --------------------------------------------------------------------- //

    public BusControllerBlock(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public void onCreate() {
        super.onCreate();

        redstone = getComponent(Redstone.class).orElseThrow(IllegalStateException::new);
        controller.scheduleScan();
    }

    // No need to call super.onDestroy() here, because our controller is by
    // necessity our own, so we really don't need to schedule a scan anymore.
    @Override
    public void onDestroy() {
        super.onDestroy();

        redstone = null;
        controller.clear();
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    protected BusDevice getDevice() {
        return controller;
    }

    // --------------------------------------------------------------------- //
    // BusNeighborAware

    @Nullable
    @Override
    protected BusController getController() {
        return controller;
    }

    // --------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        final World world = getWorld();
        if (!world.isRemote) {
            final boolean online = redstone.getInput(null) > 0;
            controller.setOnline(online);
            if (online) {
                controller.startUpdate();
            }

            isOnline.set(online);
            state.set(controller.getState());
        } else {
            switch (state.get()) {
                case READY:
                    if (isOnline.get()) {
                        spawnParticle(EnumParticleTypes.REDSTONE);
                    }
                    break;
                case ERROR_MULTIPLE_BUS_CONTROLLERS:
                case ERROR_ADDRESSES_OVERLAP:
                case ERROR_SEGMENT_FAILED:
                    spawnParticle(EnumParticleTypes.FLAME);
                    break;
            }
        }
    }

    // --------------------------------------------------------------------- //
    // LateTickable

    @Override
    public void lateUpdate() {
        controller.finishUpdate();
    }

    // --------------------------------------------------------------------- //
    // ActivationListener

    @Override
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, @Nullable final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        switch (state.get()) {
            case ERROR_MULTIPLE_BUS_CONTROLLERS:
            case ERROR_ADDRESSES_OVERLAP:
            case ERROR_SEGMENT_FAILED: {
                final World world = getWorld();
                if (world.isRemote) {
                    printErrorMessage(player, state.get());
                }
                return true;
            }
        }

        return false;
    }

    // --------------------------------------------------------------------- //

    private static void printErrorMessage(final EntityPlayer player, final AbstractBusController.State state) {
        final String key;
        switch (state) {
            case ERROR_MULTIPLE_BUS_CONTROLLERS:
                key = Constants.I18N.BUS_ERROR_MULTIPLE_CONTROLLERS;
                break;
            case ERROR_ADDRESSES_OVERLAP:
                key = Constants.I18N.BUS_ERROR_ADDRESSES_OVERLAP;
                break;
            case ERROR_SEGMENT_FAILED:
                key = Constants.I18N.BUS_ERROR_SEGMENT_FAILED;
                break;
            default:
                return;
        }

        if (player == Minecraft.getMinecraft().thePlayer) {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation(key));
        }
    }

    private void spawnParticle(final EnumParticleTypes particleType) {
        final Optional<Location> location = getComponent(Location.class);
        location.ifPresent(l -> {
            final Vec3d pos = l.getPositionVector();
            l.getWorld().spawnParticle(particleType, pos.xCoord, pos.yCoord + 0.5f, pos.zCoord, 0, 0.01f, 0);
        });
    }

    // --------------------------------------------------------------------- //

    private final class BlockBusControllerImpl extends AbstractBusController {
        @Override
        protected World getBusWorld() {
            return BusControllerBlock.this.getWorld();
        }

        @Override
        public boolean getDevices(final Collection<BusDevice> devices) {
            return BusControllerBlock.this.getDevices(devices);
        }
    }
}
