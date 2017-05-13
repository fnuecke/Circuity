package li.cil.circuity.server.gui.spatial;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.circuity.common.init.Items;
import li.cil.circuity.common.item.ItemConfigurator;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import li.cil.lib.api.gui.spatial.SpatialUIServer;
import li.cil.lib.util.PlayerUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.ObjectUtils;

import java.util.PrimitiveIterator;

public enum SpatialUIProviderServerInterruptable implements SpatialUIProviderServer {
    INSTANCE;

    // --------------------------------------------------------------------- //

    public static final String SOURCES_TAG = "sources";
    public static final String SINKS_TAG = "sinks";
    public static final String ID_TAG = "id";
    public static final String NAME_TAG = "name";
    public static final String SELECT_SOURCE_TAG = "source";
    public static final String SELECT_SINK_TAG = "sink";

    // --------------------------------------------------------------------- //

    @Override
    public SpatialUIServer provide(final SpatialUIContext context) {
        return new SpatialUIServerImpl(context);
    }

    // --------------------------------------------------------------------- //

    private static final class SpatialUIServerImpl implements SpatialUIServer {
        private final SpatialUIContext context;
        private final NBTTagCompound data = new NBTTagCompound();

        public SpatialUIServerImpl(final SpatialUIContext context) {
            this.context = context;
            data.setTag(SOURCES_TAG, new NBTTagList());
            data.setTag(SINKS_TAG, new NBTTagList());
        }

        @Override
        public void handleData(final NBTTagCompound data) {
            final BusElement busElement = context.getTarget().getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, context.getSide());
            if (!(busElement instanceof InterruptSource) && !(busElement instanceof InterruptSink)) {
                clear();
                return;
            }

            final BusController controller = busElement.getBusController();
            if (controller == null) {
                clear();
                return;
            }

            final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
            if (mapper == null) {
                clear();
                return;
            }

            if (data.hasKey(SELECT_SOURCE_TAG, Constants.NBT.TAG_INT)) {
                final int sourceId = data.getInteger(SELECT_SOURCE_TAG);
                final ItemStack stack = PlayerUtil.getHeldItemOfType(context.getPlayer(), Items.configurator);
                if (!stack.isEmpty()) {
                    ItemConfigurator.setInterruptSource(stack, sourceId);
                    context.getPlayer().inventory.markDirty();
                }
            }

            if (data.hasKey(SELECT_SINK_TAG, Constants.NBT.TAG_INT)) {
                final int sinkId = data.getInteger(SELECT_SINK_TAG);
                final ItemStack stack = PlayerUtil.getHeldItemOfType(context.getPlayer(), Items.configurator);
                if (!stack.isEmpty()) {
                    final int sourceId = ItemConfigurator.getInterruptSource(stack);
                    mapper.setInterruptMapping(sourceId, sinkId);
                    ItemConfigurator.clearInterruptSource(stack);
                    context.getPlayer().inventory.markDirty();
                }
            }
        }

        @Override
        public void update() {
            final BusElement busElement = context.getTarget().getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, context.getSide());
            if (!(busElement instanceof InterruptSource) && !(busElement instanceof InterruptSink)) {
                clear();
                return;
            }

            final BusController controller = busElement.getBusController();
            if (controller == null) {
                clear();
                return;
            }

            final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
            if (mapper == null) {
                clear();
                return;
            }

            final ItemStack stack = PlayerUtil.getHeldItemOfType(context.getPlayer(), Items.configurator);
            if (stack.isEmpty()) {
                clear();
                return;
            }

            boolean changed = false;
            if (ItemConfigurator.hasInterruptSource(stack)) {
                if (busElement instanceof InterruptSink) {
                    final InterruptSink sink = (InterruptSink) busElement;
                    final NBTTagList sinkNames = data.getTagList(SINKS_TAG, Constants.NBT.TAG_COMPOUND);
                    final int count = sink.getAcceptedInterrupts();
                    changed = setSize(sinkNames, count);

                    final PrimitiveIterator.OfInt sinkIds = mapper.getInterruptSinkIds(sink);
                    for (int i = 0; i < count; i++) {
                        final NBTTagCompound tag = sinkNames.getCompoundTagAt(i);

                        assert sinkIds.hasNext() : "interrupt mapper provided sink ids do not match interrupt sink";
                        final int sinkId = sinkIds.nextInt();

                        final ITextComponent name = sink.getAcceptedInterruptName(i);
                        final String json = ITextComponent.Serializer.componentToJson(name);

                        if (sinkId != tag.getInteger(ID_TAG) || ObjectUtils.notEqual(json, tag.getString(NAME_TAG))) {
                            changed = true;
                        }

                        tag.setInteger(ID_TAG, sinkId);
                        tag.setString(NAME_TAG, json);
                    }
                }
            } else {
                if (busElement instanceof InterruptSource) {
                    final InterruptSource source = (InterruptSource) busElement;
                    final NBTTagList sourceNames = data.getTagList(SOURCES_TAG, Constants.NBT.TAG_COMPOUND);
                    final int count = source.getEmittedInterrupts();
                    changed = setSize(sourceNames, count);

                    final PrimitiveIterator.OfInt sourceIds = mapper.getInterruptSourceIds(source);
                    for (int i = 0; i < count; i++) {
                        final NBTTagCompound tag = sourceNames.getCompoundTagAt(i);

                        assert sourceIds.hasNext() : "interrupt mapper provided source ids do not match interrupt source";
                        final int sourceId = sourceIds.nextInt();

                        final ITextComponent name = source.getEmittedInterruptName(i);
                        final String json = ITextComponent.Serializer.componentToJson(name);

                        if (sourceId != tag.getInteger(ID_TAG) || ObjectUtils.notEqual(json, tag.getString(NAME_TAG))) {
                            changed = true;
                        }

                        tag.setInteger(ID_TAG, sourceId);
                        tag.setString(NAME_TAG, json);
                    }
                }
            }

            if (!changed) {
                return;
            }

            context.sendData(data);
        }

        private static boolean setSize(final NBTTagList list, final int count) {
            if (count == list.tagCount()) {
                return false;
            }

            while (count < list.tagCount()) {
                list.removeTag(list.tagCount() - 1);
            }
            while (count > list.tagCount()) {
                list.appendTag(new NBTTagCompound());
            }

            return true;
        }

        private void clear() {
            final NBTTagList sourceNames = data.getTagList(SOURCES_TAG, Constants.NBT.TAG_STRING);
            final NBTTagList sinkNames = data.getTagList(SINKS_TAG, Constants.NBT.TAG_STRING);
            if (sourceNames.tagCount() == 0 && sinkNames.tagCount() == 0) {
                return;
            }

            while (sourceNames.tagCount() > 0) {
                sourceNames.removeTag(sourceNames.tagCount() - 1);
            }
            while (sinkNames.tagCount() > 0) {
                sinkNames.removeTag(sinkNames.tagCount() - 1);
            }

            context.sendData(data);
        }
    }
}
