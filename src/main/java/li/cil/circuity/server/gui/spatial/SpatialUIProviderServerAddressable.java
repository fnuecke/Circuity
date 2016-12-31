package li.cil.circuity.server.gui.spatial;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
import li.cil.lib.api.gui.spatial.SpatialUIContext;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import li.cil.lib.api.gui.spatial.SpatialUIServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

public enum SpatialUIProviderServerAddressable implements SpatialUIProviderServer {
    INSTANCE;

    // --------------------------------------------------------------------- //

    public static final String ADDRESS_TAG = "address";
    public static final String VALID_TAG = "valid";

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
            if (!(busElement instanceof Addressable)) {
                return;
            }

            final Addressable addressable = (Addressable) busElement;
            final BusController controller = addressable.getBusController();
            if (controller == null) {
                return;
            }

            final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
            if (mapper == null) {
                return;
            }

            final AddressBlock address = mapper.getAddressBlock(addressable);
            if (address == null) {
                return;
            }

            final long offset = data.getLong(ADDRESS_TAG);

            mapper.setDeviceAddress(addressable, address.at(offset));
        }

        @Override
        public void update() {
            final BusElement busElement = context.getTarget().getCapability(CapabilityBusElement.BUS_ELEMENT_CAPABILITY, context.getSide());
            if (!(busElement instanceof Addressable)) {
                clear();
                return;
            }

            final Addressable addressable = (Addressable) busElement;
            final BusController controller = addressable.getBusController();
            if (controller == null) {
                clear();
                return;
            }

            final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
            if (mapper == null) {
                clear();
                return;
            }

            final AddressBlock address = mapper.getAddressBlock(addressable);
            if (address == null) {
                clear();
                return;
            }

            final long offset = address.getOffset();
            final boolean isValid = mapper.isDeviceAddressValid(addressable);

            if (!isDifferent(offset, isValid)) {
                return;
            }

            data.setLong(ADDRESS_TAG, offset);
            data.setBoolean(VALID_TAG, isValid);

            context.sendData(data);
        }

        private boolean isDifferent(final long offset, final boolean isValid) {
            if (!data.hasKey(ADDRESS_TAG, Constants.NBT.TAG_LONG) || offset != data.getLong(ADDRESS_TAG)) {
                return true;
            }

            if (!data.hasKey(VALID_TAG, Constants.NBT.TAG_BYTE) || isValid != data.getBoolean(VALID_TAG)) {
                return true;
            }

            return false;
        }

        private void clear() {
            if (data.getLong(ADDRESS_TAG) == 0 && data.getBoolean(VALID_TAG) == false) {
                return;
            }

            data.setLong(ADDRESS_TAG, 0);
            data.setBoolean(VALID_TAG, false);

            context.sendData(data);
        }
    }
}
