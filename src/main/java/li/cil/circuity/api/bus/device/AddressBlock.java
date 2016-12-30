package li.cil.circuity.api.bus.device;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

@Serializable
public final class AddressBlock implements INBTSerializable<NBTTagCompound> {
    private static final String OFFSET_TAG = "offset";
    private static final String LENGTH_TAG = "length";
    private static final AddressBlock EMPTY = new AddressBlock(0, 0);

    // --------------------------------------------------------------------- //

    public static AddressBlock empty() {
        return EMPTY;
    }

    public static AddressBlock fromNBT(final NBTTagCompound nbt) {
        final AddressBlock address = new AddressBlock();
        address.deserializeNBT(nbt);
        return address;
    }

    // --------------------------------------------------------------------- //

    @Serialize
    private long offset;

    @Serialize
    private long length;

    // --------------------------------------------------------------------- //

    public AddressBlock(final long offset, final long length) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative.");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative.");
        }
        if (Long.MAX_VALUE - length < offset) {
            throw new IllegalArgumentException("Address block overflow long capacity.");
        }

        this.offset = offset;
        this.length = length;
    }

    private AddressBlock() {
    }

    // --------------------------------------------------------------------- //

    public long getOffset() {
        return offset;
    }

    public long getLength() {
        return length;
    }

    public long getEnd() {
        return offset + length;
    }

    // --------------------------------------------------------------------- //

    public boolean contains(final AddressBlock block) {
        return block.getOffset() >= getOffset() && block.getEnd() <= getEnd();
    }

    public AddressBlock take(final int addresses) {
        return new AddressBlock(offset, addresses);
    }

    public AddressBlock take(final long minOffset, final int addresses) {
        return new AddressBlock(Math.max(offset, minOffset), addresses);
    }

    public AddressBlock clamp(final AddressBlock that) {
        return new AddressBlock(Math.max(offset, that.offset), Math.min(offset + length, that.offset + that.length));
    }

    public AddressBlock at(final long offset) {
        return new AddressBlock(offset, getLength());
    }

    // --------------------------------------------------------------------- //
    // Object

    @Override
    public String toString() {
        return String.format("%08Xh-%08Xh", offset, offset + length);
    }

    // --------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong(OFFSET_TAG, offset);
        nbt.setLong(LENGTH_TAG, length);
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        offset = Math.max(0, nbt.getLong(OFFSET_TAG));
        length = Math.max(0, nbt.getLong(LENGTH_TAG));
    }
}
