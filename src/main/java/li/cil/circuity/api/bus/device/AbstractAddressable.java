package li.cil.circuity.api.bus.device;

import li.cil.circuity.api.bus.AddressBlock;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

@Serializable
public abstract class AbstractAddressable extends AbstractBusDevice implements Addressable {
    @Serialize
    protected AddressBlock memory;

    protected abstract AddressBlock validateAddress(final AddressBlock memory);

    @Override
    public AddressBlock getMemory(final AddressBlock memory) {
        if (this.memory != null) {
            return this.memory;
        } else {
            return validateAddress(memory);
        }
    }

    public void setMemory(@Nullable final AddressBlock memory) {
        this.memory = memory;
    }
}
