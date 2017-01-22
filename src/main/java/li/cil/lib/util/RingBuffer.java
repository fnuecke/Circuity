package li.cil.lib.util;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

@Serializable
public class RingBuffer {
    @Serialize
    private final byte[] buffer;
    @Serialize
    private int writeIdx;
    @Serialize
    private int readIdx;

    public RingBuffer(final int size) {
        this.buffer = new byte[size + 1];
    }

    public void clear() {
        writeIdx = readIdx = 0;
    }

    public int size() {
        if (readIdx <= writeIdx) {
            return writeIdx - readIdx;
        } else {
            return writeIdx + buffer.length - readIdx;
        }
    }

    public boolean isReadable() {
        return (readIdx % buffer.length) != (writeIdx % buffer.length);
    }

    public boolean isWritable() {
        return ((writeIdx + 1) % buffer.length) != (readIdx % buffer.length);
    }

    public void write(final byte value) {
        if (!isWritable()) {
            throw new IllegalStateException("Buffer is full.");
        }

        writeIdx = writeIdx % buffer.length;
        buffer[writeIdx++] = value;
    }

    public byte read() {
        if (!isReadable()) {
            throw new IllegalStateException("Buffer is empty.");
        }

        readIdx = readIdx % buffer.length;
        return buffer[readIdx++];
    }
}