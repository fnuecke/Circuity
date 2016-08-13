package li.cil.circuity.util;

import com.google.common.base.Splitter;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// Supports I8HEX (https://en.wikipedia.org/wiki/Intel_HEX)
public final class IntelHexLoader {
    private static final Pattern INTEL_HEX_PATTERN = Pattern.compile("^:(.{2})(.{4})(.{2})(.*)(.{2})$");

    @FunctionalInterface
    public interface MemoryAccess {
        void write(final int address, final int data);
    }

    public static void load(final Iterable<String> lines, final MemoryAccess memory) {
        for (final String line : lines) {
            final Matcher matcher = INTEL_HEX_PATTERN.matcher(line);
            if (matcher.matches()) {
                final int bytes = Integer.parseInt(matcher.group(1), 16);
                final int offset = Integer.parseInt(matcher.group(2), 16);
                final int recordType = Integer.parseInt(matcher.group(3), 16);
                final List<Integer> data = matcher.group(4).isEmpty() ? Collections.emptyList() : StreamSupport.stream(Splitter.fixedLength(2).split(matcher.group(4)).spliterator(), false).map(b -> Integer.parseInt(b, 16)).collect(Collectors.toList());
                final int checksum = Integer.parseInt(matcher.group(5), 16);

                final int computedChecksum = (~(bytes + (offset >> 8) + (offset & 0xFF) + recordType + data.stream().reduce(0, (a, b) -> a + b)) + 1) & 0xFF;
                if (computedChecksum != checksum) {
                    throw new IllegalArgumentException(String.format("Checksum failed for line '%s'.", line));
                }
                if (bytes != data.size()) {
                    throw new IllegalArgumentException(String.format("Byte count does not match data size on line '%s'.", line));
                }

                if (recordType == 0) {
                    for (int address = offset, end = offset + bytes; address < end; ++address) {
                        memory.write(address, data.get(address - offset));
                    }
                } else if (recordType == 1) {
                    break; // EOF
                } else {
                    throw new IllegalArgumentException(String.format("Unsupported record type '%d'.", recordType));
                }
            }
        }
    }

    private IntelHexLoader() {
    }
}
