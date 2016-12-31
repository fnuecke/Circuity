package li.cil.lib.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.lib.ModSillyBee;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;

public class MessageSpatialUISubscribe implements IMessage {
    private int dimension;
    private BlockPos pos;
    private EnumFacing side;
    private Class<? extends SpatialUIProviderServer> providerClass;

    // --------------------------------------------------------------------- //

    public MessageSpatialUISubscribe(final int dimension, final BlockPos pos, @Nullable final EnumFacing side, final Class<? extends SpatialUIProviderServer> providerClass) {
        this.dimension = dimension;
        this.pos = pos;
        this.side = side;
        this.providerClass = providerClass;
    }

    @SuppressWarnings("unused")
    public MessageSpatialUISubscribe() {
    }

    // --------------------------------------------------------------------- //

    public int getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    public EnumFacing getSide() {
        return side;
    }

    public Class<? extends SpatialUIProviderServer> getProviderClass() {
        return providerClass;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @SuppressWarnings("unchecked")
    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        dimension = packet.readVarInt();
        pos = packet.readBlockPos();
        final int sideOrdinal = packet.readVarInt();
        if (sideOrdinal < 0) {
            side = null;
        } else {
            side = EnumFacing.class.getEnumConstants()[sideOrdinal];
        }
        final String dataProviderName = packet.readString(255);
        try {
            providerClass = (Class<? extends SpatialUIProviderServer>) Class.forName(dataProviderName);
        } catch (final ClassNotFoundException e) {
            ModSillyBee.getLogger().error("Failed reading data provider class of type '{}'.", dataProviderName);
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer packet = new PacketBuffer(buf);
        packet.writeVarInt(dimension);
        packet.writeBlockPos(pos);
        if (side == null) {
            packet.writeVarInt(-1);
        } else {
            packet.writeVarInt(side.ordinal());
        }
        packet.writeString(providerClass.getName());
    }
}
