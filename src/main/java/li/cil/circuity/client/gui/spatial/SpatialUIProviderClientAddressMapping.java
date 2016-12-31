package li.cil.circuity.client.gui.spatial;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.circuity.common.item.ItemConfigurator;
import li.cil.circuity.server.gui.spatial.SpatialUIProviderServerAddressMapping;
import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.gui.spatial.SpatialUIClient;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.spatial.SpatialUIProviderClient;
import li.cil.lib.api.gui.widget.Canvas;
import li.cil.lib.client.gui.layout.VerticalLayout;
import li.cil.lib.client.gui.widget.Button;
import li.cil.lib.client.gui.widget.Window;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public enum SpatialUIProviderClientAddressMapping implements SpatialUIProviderClient {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Override
    public boolean canProvideFor(final EntityPlayer player, final ICapabilityProvider value, @Nullable final EnumFacing side) {
        if (!ItemConfigurator.isMode(player, ItemConfigurator.Mode.ADDRESS_MAPPING)) {
            return false;
        }

        if (!value.hasCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, side)) {
            return false;
        }

        final BusElement busElement = value.getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, side);
        return busElement instanceof BusController;
    }

    @Override
    public SpatialUIClient provide(final SpatialUIContext context) {
        return new SpatialUIClientImpl(context);
    }

    // --------------------------------------------------------------------- //

    private final class SpatialUIClientImpl extends AbstractSpatialUIClient {
        private static final int UI_SIZE = 128;
        private static final int CURRENT_COLOR = 0x00FF00;

        // --------------------------------------------------------------------- //

        private final Window window;

        // --------------------------------------------------------------------- //

        public SpatialUIClientImpl(final SpatialUIContext context) {
            super(context);
            window = new Window().
                    setWidth(UI_SIZE).
                    setHeight(UI_SIZE).
                    setLayout(new VerticalLayout().
                            setHorizontalAlignment(Alignment.Horizontal.CENTER).
                            setVerticalAlignment(Alignment.Vertical.MIDDLE).
                            setExpandWidth(true));
        }

        // --------------------------------------------------------------------- //

        @Override
        public void handleData(final NBTTagCompound data) {
            window.clear();

            final int count = data.getInteger(SpatialUIProviderServerAddressMapping.MAPPING_COUNT_TAG);
            final int current = data.getInteger(SpatialUIProviderServerAddressMapping.MAPPING_CURRENT_TAG);

            for (int i = 0; i < count; i++) {
                final int index = i;
                final Button button = new Button().
                        setText(String.valueOf(i)).
                        addListener(b -> setCurrent(index));
                if (i == current) {
                    button.setTextColor(CURRENT_COLOR);
                }
                window.add(button);
            }
        }

        // --------------------------------------------------------------------- //
        // AbstractSpatialUIClient

        @Override
        protected int getSize() {
            return UI_SIZE;
        }

        @Override
        protected boolean isValid() {
            return SpatialUIProviderClientAddressMapping.INSTANCE.canProvideFor(context.getPlayer(), context.getTarget(), context.getSide());
        }

        @Nullable
        @Override
        protected Canvas getWindow() {
            return window;
        }

        // --------------------------------------------------------------------- //

        private void setCurrent(final int i) {
            final NBTTagCompound data = new NBTTagCompound();
            data.setInteger(SpatialUIProviderServerAddressMapping.MAPPING_CURRENT_TAG, i);
            context.sendData(data);
        }
    }
}
