package li.cil.circuity.server.gui.spatial;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import li.cil.lib.api.gui.spatial.SpatialUIServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

public enum SpatialUIProviderServerAddressMapping implements SpatialUIProviderServer {
    INSTANCE;

    // --------------------------------------------------------------------- //

    public static final String MAPPING_COUNT_TAG = "count";
    public static final String MAPPING_CURRENT_TAG = "current";

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
        }

        @Override
        public void handleData(final NBTTagCompound data) {
            final BusElement busElement = context.getTarget().getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, context.getSide());
            if (!(busElement instanceof BusController)) {
                return;
            }

            final BusController controller = (BusController) busElement;
            final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
            if (mapper == null) {
                return;
            }

            final int current = data.getInteger(MAPPING_CURRENT_TAG);

            mapper.setActiveConfiguration(current);
        }

        @Override
        public void update() {
            final BusElement busElement = context.getTarget().getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, context.getSide());
            if (!(busElement instanceof BusController)) {
                return;
            }

            final BusController controller = (BusController) busElement;
            final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
            if (mapper == null) {
                clear();
                return;
            }

            final int count = mapper.getConfigurationCount();
            final int current = mapper.getActiveConfiguration();

            if (!isDifferent(count, current)) {
                return;
            }

            data.setInteger(MAPPING_COUNT_TAG, count);
            data.setInteger(MAPPING_CURRENT_TAG, current);

            context.sendData(data);
        }

        private boolean isDifferent(final int count, final int current) {
            if (!data.hasKey(MAPPING_COUNT_TAG, Constants.NBT.TAG_INT) || count != data.getLong(MAPPING_COUNT_TAG)) {
                return true;
            }

            if (!data.hasKey(MAPPING_CURRENT_TAG, Constants.NBT.TAG_INT) || current != data.getLong(MAPPING_CURRENT_TAG)) {
                return true;
            }

            return false;
        }

        private void clear() {
            if (data.getInteger(MAPPING_COUNT_TAG) == 0 && data.getInteger(MAPPING_CURRENT_TAG) == 0) {
                return;
            }

            data.setInteger(MAPPING_COUNT_TAG, 0);
            data.setInteger(MAPPING_CURRENT_TAG, 0);

            context.sendData(data);
        }
    }
}
