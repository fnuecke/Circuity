package li.cil.lib.util;

import com.google.common.base.Throwables;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTUtil {
    public static void write(@Nullable final NBTBase nbt, final PacketBuffer packet) {
        try {
            final DataOutput stream = new ByteBufOutputStream(packet);
            if (nbt != null) {
                CompressedStreamTools.writeTag(nbt, new ByteBufOutputStream(packet));
            } else {
                stream.writeByte((byte) -1);
            }
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Nullable
    public static NBTBase read(final PacketBuffer packet) {
        try {
            final ByteBufInputStream stream = new ByteBufInputStream(packet);
            stream.mark(0);
            if (stream.readByte() != (byte) -1) {
                stream.reset();
                return CompressedStreamTools.read(stream, 0, new NBTSizeTracker(2000000L));
            } else {
                return null;
            }
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private NBTUtil() {
    }
}
