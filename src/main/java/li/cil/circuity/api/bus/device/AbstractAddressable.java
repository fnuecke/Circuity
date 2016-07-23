package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

@Serializable
public abstract class AbstractAddressable extends AbstractBusDevice implements Addressable {
    @Serialize
    protected AddressBlock address;

    protected abstract AddressBlock validateAddress(final AddressBlock address);

    @Override
    public AddressBlock getAddress(final AddressBlock address) {
        if (this.address != null) {
            return this.address;
        } else {
            return validateAddress(address);
        }
    }

    @Override
    public void setAddress(@Nullable final AddressBlock address) {
        this.address = address;
    }
}
