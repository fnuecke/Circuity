package li.cil.circuity.common.bus.util;

import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.util.SerialPortManager;

public interface SerialPortManagerProxy extends Addressable {
    SerialPortManager getSerialPortManager();

    @Override
    default AddressBlock getPreferredAddressBlock(final AddressBlock memory) {
        return getSerialPortManager().getPreferredAddressBlock(memory);
    }

    @Override
    default int read(final long address) {
        return getSerialPortManager().read(address);
    }

    @Override
    default void write(final long address, final int value) {
        getSerialPortManager().write(address, value);
    }
}
