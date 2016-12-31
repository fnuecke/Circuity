package li.cil.circuity.common.ecs.component;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.bus.controller.AbstractBusController;
import li.cil.lib.api.ecs.component.LateTickable;
import li.cil.lib.api.ecs.component.Redstone;
import li.cil.lib.api.ecs.component.event.ActivationListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.synchronization.value.SynchronizedBoolean;
import li.cil.lib.synchronization.value.SynchronizedEnum;
import li.cil.lib.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;

@Serializable
public class BusControllerBlock extends BusNeighborAware implements ITickable, LateTickable, ActivationListener {
    @Serialize
    private final BlockBusControllerImpl controller = new BlockBusControllerImpl();

    private final SynchronizedEnum<AbstractBusController.State> state = new SynchronizedEnum<>(AbstractBusController.State.class);
    private final SynchronizedBoolean isOnline = new SynchronizedBoolean();

    private Redstone redstone;

    // --------------------------------------------------------------------- //

    public BusControllerBlock(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    public boolean hasErrors() {
        return state.get().isError;
    }

    public boolean isOnline() {
        return isOnline.get();
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
        controller.dispose();
    }

    // --------------------------------------------------------------------- //
    // AbstractComponentBusDevice

    @Override
    public BusDevice getBusElement() {
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
    public boolean handleActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        switch (state.get()) {
            case ERROR_MULTIPLE_BUS_CONTROLLERS:
            case ERROR_SUBSYSTEM:
            case ERROR_CONNECTION_FAILED: {
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
            case ERROR_SUBSYSTEM:
                key = Constants.I18N.BUS_ERROR_ADDRESSES_OVERLAP;
                break;
            case ERROR_CONNECTION_FAILED:
                key = Constants.I18N.BUS_ERROR_CONNECTION_FAILED;
                break;
            default:
                return;
        }

        PlayerUtil.addLocalChatMessage(player, new TextComponentTranslation(key));
    }

    // --------------------------------------------------------------------- //

    public final class BlockBusControllerImpl extends AbstractBusController {
        public long getComponentId() {
            return BusControllerBlock.this.getId();
        }

        // --------------------------------------------------------------------- //
        // BusController

        @Override
        public void markChanged() {
            BusControllerBlock.this.markChanged();
        }

        // --------------------------------------------------------------------- //
        // AbstractBusController

        @Override
        protected World getBusWorld() {
            return BusControllerBlock.this.getWorld();
        }

        // --------------------------------------------------------------------- //
        // BusConnector

        @Override
        public boolean getConnected(final Collection<BusElement> devices) {
            return BusControllerBlock.this.getConnected(devices);
        }
    }
}
