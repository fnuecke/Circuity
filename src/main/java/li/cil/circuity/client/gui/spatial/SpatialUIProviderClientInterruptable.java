package li.cil.circuity.client.gui.spatial;

import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.circuity.common.init.Items;
import li.cil.circuity.common.item.ItemConfigurator;
import li.cil.circuity.server.gui.spatial.SpatialUIProviderServerInterruptable;
import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.gui.spatial.SpatialUIClient;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.spatial.SpatialUIProviderClient;
import li.cil.lib.api.gui.widget.Canvas;
import li.cil.lib.client.gui.layout.VerticalLayout;
import li.cil.lib.client.gui.widget.Button;
import li.cil.lib.client.gui.widget.Label;
import li.cil.lib.client.gui.widget.Window;
import li.cil.lib.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public enum SpatialUIProviderClientInterruptable implements SpatialUIProviderClient {
    INSTANCE;

    // --------------------------------------------------------------------- //

    @Override
    public boolean canProvideFor(final EntityPlayer player, final ICapabilityProvider value, @Nullable final EnumFacing side) {
        final ItemStack stack = PlayerUtil.getHeldItemOfType(player, Items.configurator);
        if (stack.isEmpty()) {
            return false;
        }

        if (!ItemConfigurator.isMode(stack, ItemConfigurator.Mode.INTERRUPT)) {
            return false;
        }

        if (!value.hasCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, side)) {
            return false;
        }

        final BusElement busElement = value.getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, side);
        if (ItemConfigurator.hasInterruptSource(stack)) {
            return busElement instanceof InterruptSink;
        } else {
            return busElement instanceof InterruptSource;
        }
    }

    @Override
    public SpatialUIClient provide(final SpatialUIContext context) {
        return new SpatialUIClientImpl(context);
    }

    // --------------------------------------------------------------------- //

    private final class SpatialUIClientImpl extends AbstractSpatialUIClient {
        private static final int UI_SIZE = 128;
        private static final int UI_MARGIN = 8;

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
                            setPadding(UI_MARGIN, UI_MARGIN, UI_MARGIN, UI_MARGIN).
                            setExpandWidth(true));
        }

        // --------------------------------------------------------------------- //

        @Override
        public void handleData(final NBTTagCompound data) {
            window.clear();

            final ItemStack stack = PlayerUtil.getHeldItemOfType(context.getPlayer(), Items.configurator);
            if (stack.isEmpty()) {
                return;
            }

            if (ItemConfigurator.hasInterruptSource(stack)) {
                final NBTTagList sinks = data.getTagList(SpatialUIProviderServerInterruptable.SINKS_TAG, Constants.NBT.TAG_COMPOUND);
                if (sinks.tagCount() > 0) {
                    window.add(new Label().
                            setText("Sinks").
                            setHorizontalAlignment(Alignment.Horizontal.CENTER));
                }

                for (int i = 0; i < sinks.tagCount(); i++) {
                    final NBTTagCompound tag = sinks.getCompoundTagAt(i);
                    final int id = tag.getInteger(SpatialUIProviderServerInterruptable.ID_TAG);
                    final String json = tag.getString(SpatialUIProviderServerInterruptable.NAME_TAG);
                    final ITextComponent name = ITextComponent.Serializer.jsonToComponent(json);

                    window.add(new Button().
                            setText(name.getUnformattedText()).
                            addListener(btn -> setSink(id)));
                }
            } else {
                final NBTTagList sources = data.getTagList(SpatialUIProviderServerInterruptable.SOURCES_TAG, Constants.NBT.TAG_COMPOUND);
                if (sources.tagCount() > 0) {
                    window.add(new Label().
                            setText("Sources").
                            setHorizontalAlignment(Alignment.Horizontal.CENTER));
                }

                for (int i = 0; i < sources.tagCount(); i++) {
                    final NBTTagCompound tag = sources.getCompoundTagAt(i);
                    final int id = tag.getInteger(SpatialUIProviderServerInterruptable.ID_TAG);
                    final String json = tag.getString(SpatialUIProviderServerInterruptable.NAME_TAG);
                    final ITextComponent name = ITextComponent.Serializer.jsonToComponent(json);

                    window.add(new Button().
                            setText(name.getUnformattedText()).
                            addListener(btn -> setSource(id)));
                }
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
            return SpatialUIProviderClientInterruptable.INSTANCE.canProvideFor(context.getPlayer(), context.getTarget(), context.getSide());
        }

        @Nullable
        @Override
        protected Canvas getWindow() {
            return window;
        }

        // --------------------------------------------------------------------- //

        public void setSource(final int source) {
            final NBTTagCompound data = new NBTTagCompound();
            data.setInteger(SpatialUIProviderServerInterruptable.SELECT_SOURCE_TAG, source);
            context.sendData(data);
        }

        public void setSink(final int sink) {
            final NBTTagCompound data = new NBTTagCompound();
            data.setInteger(SpatialUIProviderServerInterruptable.SELECT_SINK_TAG, sink);
            context.sendData(data);
        }
    }
}
