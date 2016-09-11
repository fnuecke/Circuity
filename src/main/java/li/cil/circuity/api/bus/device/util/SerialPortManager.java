package li.cil.circuity.api.bus.device.util;

import li.cil.circuity.api.bus.device.AddressBlock;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing a list of serial ports.
 * <p>
 * This provides a more flexible, less error-prone way of defining ports for
 * devices, where each port has a distinct functionality. This is <em>not</em>
 * intended for block devices, such as RAM.
 */
public final class SerialPortManager {
    private final List<Port> ports = new ArrayList<>();
    private long preferredAddressOffset = 0L;

    // --------------------------------------------------------------------- //

    /**
     * Register a port provided by this device.
     * <p>
     * To be called from the constructor of subclasses.
     *
     * @param reader      operation to perform when reading from the port.
     * @param writer      operation to perform when writing to the port.
     * @param description a description of the port.
     */
    public void addSerialPort(@Nullable final Reader reader, @Nullable final Writer writer, @Nullable final ITextComponent description) {
        ports.add(new Port(reader, writer, description));
    }

    public void setPreferredAddressOffset(final long offset) {
        preferredAddressOffset = offset;
    }

    // --------------------------------------------------------------------- //

    public AddressBlock getPreferredAddressBlock(final AddressBlock memory) {
        return memory.take(preferredAddressOffset, ports.size());
    }

    public int read(final long address) {
        final Port port = ports.get((int) address);
        if (port.reader != null) {
            return port.reader.read(address);
        } else {
            return 0xFFFFFFFF;
        }
    }

    public void write(final long address, final int value) {
        final Port port = ports.get((int) address);
        if (port.writer != null) {
            port.writer.write(address, value);
        }
    }

    // --------------------------------------------------------------------- //

    @FunctionalInterface
    public interface Reader {
        int read(final long address);
    }

    @FunctionalInterface
    public interface Writer {
        void write(final long address, final int value);
    }

    // --------------------------------------------------------------------- //

    private static final class Port {
        public final Reader reader;
        public final Writer writer;
        public final ITextComponent description;

        private Port(@Nullable final Reader reader, @Nullable final Writer writer, @Nullable final ITextComponent description) {
            this.reader = reader;
            this.writer = writer;
            this.description = description;
        }
    }
}
