package li.cil.circuity.client.gui.spatial;

import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.circuity.common.item.ItemConfigurator;
import li.cil.circuity.server.gui.spatial.SpatialUIProviderServerAddressable;
import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.gui.spatial.SpatialUIClient;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.spatial.SpatialUIProviderClient;
import li.cil.lib.api.gui.widget.Canvas;
import li.cil.lib.client.gui.layout.HorizontalLayout;
import li.cil.lib.client.gui.layout.VerticalLayout;
import li.cil.lib.client.gui.widget.Button;
import li.cil.lib.client.gui.widget.Label;
import li.cil.lib.client.gui.widget.Panel;
import li.cil.lib.client.gui.widget.Window;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;

public enum SpatialUIProviderClientAddressable implements SpatialUIProviderClient {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Override
    public boolean canProvideFor(final EntityPlayer player, final ICapabilityProvider value, @Nullable final EnumFacing side) {
        if (!ItemConfigurator.isMode(player, ItemConfigurator.Mode.ADDRESS)) {
            return false;
        }

        if (!value.hasCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, side)) {
            return false;
        }

        final BusElement busElement = value.getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, side);
        return busElement instanceof Addressable;
    }

    @Override
    public SpatialUIClient provide(final SpatialUIContext context) {
        return new SpatialUIClientImpl(context);
    }

    // --------------------------------------------------------------------- //

    private final class SpatialUIClientImpl extends AbstractSpatialUIClient {
        private static final int UI_SIZE = 128;
        private static final int VALID_COLOR = 0xFFFFFF;
        private static final int INVALID_COLOR = 0xFF0000;

        // --------------------------------------------------------------------- //

        private final Window window;
        private final Label label;
        private long offset;

        // --------------------------------------------------------------------- //

        public SpatialUIClientImpl(final SpatialUIContext context) {
            super(context);
            window = new Window().
                    setWidth(UI_SIZE).
                    setHeight(UI_SIZE).
                    setLayout(new VerticalLayout().
                            setHorizontalAlignment(Alignment.Horizontal.CENTER).
                            setVerticalAlignment(Alignment.Vertical.MIDDLE).
                            setExpandWidth(true)).
                    add(new Panel().
                            setFlexibleWidth(1).
                            setLayout(new HorizontalLayout().
                                    setHorizontalAlignment(Alignment.Horizontal.CENTER)).
                            add(new Button().setText("+").addListener(b -> incrementOffset(0x10000))).
                            add(new Button().setText("+").addListener(b -> incrementOffset(0x01000))).
                            add(new Button().setText("+").addListener(b -> incrementOffset(0x00100))).
                            add(new Button().setText("+").addListener(b -> incrementOffset(0x00010))).
                            add(new Button().setText("+").addListener(b -> incrementOffset(0x00001)))).
                    add(label = new Label().setHorizontalAlignment(Alignment.Horizontal.CENTER).
                            setFlexibleWidth(1)).
                    add(new Panel().
                            setLayout(new HorizontalLayout().
                                    setHorizontalAlignment(Alignment.Horizontal.CENTER)).
                            add(new Button().setText("-").addListener(b -> incrementOffset(-0x10000))).
                            add(new Button().setText("-").addListener(b -> incrementOffset(-0x01000))).
                            add(new Button().setText("-").addListener(b -> incrementOffset(-0x00100))).
                            add(new Button().setText("-").addListener(b -> incrementOffset(-0x00010))).
                            add(new Button().setText("-").addListener(b -> incrementOffset(-0x00001))));
        }

        // --------------------------------------------------------------------- //

        @Override
        public void handleData(final NBTTagCompound data) {
            offset = data.getLong(SpatialUIProviderServerAddressable.ADDRESS_TAG);
            label.setText(String.format("0x%05X", offset));

            if (data.getBoolean(SpatialUIProviderServerAddressable.VALID_TAG)) {
                label.setTextColor(VALID_COLOR);
            } else {
                label.setTextColor(INVALID_COLOR);
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
            return SpatialUIProviderClientAddressable.INSTANCE.canProvideFor(context.getPlayer(), context.getTarget(), context.getSide());
        }

        @Nullable
        @Override
        protected Canvas getWindow() {
            return window;
        }

        // --------------------------------------------------------------------- //

        private void incrementOffset(final int delta) {
            offset = (offset + delta) & 0xFFFFF;

            final NBTTagCompound data = new NBTTagCompound();
            data.setLong(SpatialUIProviderServerAddressable.ADDRESS_TAG, offset);
            context.sendData(data);
        }
    }
}
