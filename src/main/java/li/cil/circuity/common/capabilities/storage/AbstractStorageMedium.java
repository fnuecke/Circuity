package li.cil.circuity.common.capabilities.storage;

import io.netty.buffer.ByteBuf;
import li.cil.circuity.api.item.StorageMedium;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.storage.ExternalData;

import java.util.UUID;

public abstract class AbstractStorageMedium implements StorageMedium {
    UUID uuid;

    // --------------------------------------------------------------------- //

    /**
     * Get the capacity of the storage medium, in bytes.
     *
     * @return the size of the storage medium.
     */
    protected abstract int getCapacity();

    // --------------------------------------------------------------------- //
    // StorageMedium

    @Override
    public ByteBuf getData() {
        final ExternalData externalData = SillyBeeAPI.storage.getData(uuid);
        uuid = externalData.getDataId();

        final ByteBuf data = externalData.getData();
        data.capacity(getCapacity());
        return data;
    }
}
