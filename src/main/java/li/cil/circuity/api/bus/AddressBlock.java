package li.cil.circuity.api.bus;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

@Serializable
public final class AddressBlock implements INBTSerializable<NBTTagCompound> {
    private static final String OFFSET_TAG = "offset";
    private static final String LENGTH_TAG = "length";
    private static final String WORD_SIZE_TAG = "wordSize";
    private static final AddressBlock EMPTY = new AddressBlock(0, 0, 0);

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
    private int offset;

    @Serialize
    private int length;

    @Serialize
    private int wordSize;

    // --------------------------------------------------------------------- //

    public AddressBlock(final int offset, final int length, final int wordSize) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative.");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative.");
        }
        if (wordSize < 0) {
            throw new IllegalArgumentException("Word size must not be negative.");
        }

        this.offset = offset;
        this.length = length;
        this.wordSize = wordSize;
    }

    private AddressBlock() {
    }

    // --------------------------------------------------------------------- //

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    /**
     * The word size of this address block in bits.
     * <p>
     * The total size of this address block in bits is therefore <code>getLength() * getWordSize()</code>.
     * <p>
     * An example for this is random access memory, which has a constant size,
     * but supports multiple data bus widths, optimizing access by using
     * different array types (<code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code>).
     *
     * @return the word size of the data in this address block.
     */
    public int getWordSize() {
        return wordSize;
    }

    // --------------------------------------------------------------------- //

    public AddressBlock take(final int bits) {
        return new AddressBlock(offset, (int) Math.ceil(bits / (double) wordSize), wordSize);
    }

    // --------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger(OFFSET_TAG, offset);
        nbt.setInteger(LENGTH_TAG, length);
        nbt.setInteger(WORD_SIZE_TAG, wordSize);
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        offset = Math.max(0, nbt.getInteger(OFFSET_TAG));
        length = Math.max(0, nbt.getInteger(LENGTH_TAG));
        wordSize = Math.max(0, nbt.getInteger(WORD_SIZE_TAG));
    }
}
