package li.cil.lib;

import gnu.trove.set.hash.TIntHashSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import li.cil.lib.common.Serialization;
import li.cil.lib.synchronization.value.SynchronizedArray;
import li.cil.lib.synchronization.value.SynchronizedBoolean;
import li.cil.lib.synchronization.value.SynchronizedByte;
import li.cil.lib.synchronization.value.SynchronizedByteArray;
import li.cil.lib.synchronization.value.SynchronizedChar;
import li.cil.lib.synchronization.value.SynchronizedDouble;
import li.cil.lib.synchronization.value.SynchronizedFloat;
import li.cil.lib.synchronization.value.SynchronizedInt;
import li.cil.lib.synchronization.value.SynchronizedIntArray;
import li.cil.lib.synchronization.value.SynchronizedLong;
import li.cil.lib.synchronization.value.SynchronizedObject;
import li.cil.lib.synchronization.value.SynchronizedShort;
import li.cil.lib.synchronization.value.SynchronizedString;
import net.minecraft.network.PacketBuffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SynchronizationTest {
    @Before
    public void setup() {
        SillyBeeAPI.serialization = Serialization.INSTANCE;
    }

    @Test
    public void synchronizeStrings() {
        synchronizeString("");
        synchronizeString("foo bar baz");
        synchronizeString("foo \uBEEF bar");
    }

    @Test
    public void synchronizeDoubles() {
        synchronizeDouble(Double.MIN_VALUE);
        synchronizeDouble(Double.MAX_VALUE);
        synchronizeDouble(-123.45);
        synchronizeDouble(-1.0);
        synchronizeDouble(-0.0);
        synchronizeDouble(0.0);
        synchronizeDouble(1.0);
        synchronizeDouble(123.45);
    }

    @Test
    public void synchronizeFloats() {
        synchronizeFloat(Float.MIN_VALUE);
        synchronizeFloat(Float.MAX_VALUE);
        synchronizeFloat(-123.45f);
        synchronizeFloat(-1.0f);
        synchronizeFloat(-0.0f);
        synchronizeFloat(0.0f);
        synchronizeFloat(1.0f);
        synchronizeFloat(123.45f);
    }

    @Test
    public void synchronizeChars() {
        synchronizeChar(Character.MIN_VALUE);
        synchronizeChar(Character.MAX_VALUE);
        synchronizeChar((char) -1);
        synchronizeChar((char) -0);
        synchronizeChar((char) 0);
        synchronizeChar((char) 1);
        synchronizeChar('a');
        synchronizeChar('A');
        synchronizeChar('z');
        synchronizeChar('Z');
        synchronizeChar('\uBEEF');
    }

    @Test
    public void synchronizeLongs() {
        synchronizeLong(Long.MIN_VALUE);
        synchronizeLong(Long.MAX_VALUE);
        synchronizeLong(-1L);
        synchronizeLong(-0L);
        synchronizeLong(0L);
        synchronizeLong(1L);
    }

    @Test
    public void synchronizeInts() {
        synchronizeInt(Integer.MIN_VALUE);
        synchronizeInt(Integer.MAX_VALUE);
        synchronizeInt(-1);
        synchronizeInt(-0);
        synchronizeInt(0);
        synchronizeInt(1);
    }

    @Test
    public void synchronizeShorts() {
        synchronizeShort(Short.MIN_VALUE);
        synchronizeShort(Short.MAX_VALUE);
        synchronizeShort((short) -1);
        synchronizeShort((short) -0);
        synchronizeShort((short) 0);
        synchronizeShort((short) 1);
    }

    @Test
    public void synchronizeBytes() {
        synchronizeByte(Byte.MIN_VALUE);
        synchronizeByte(Byte.MAX_VALUE);
        synchronizeByte((byte) -1);
        synchronizeByte((byte) -0);
        synchronizeByte((byte) 0);
        synchronizeByte((byte) 1);
    }

    @Test
    public void synchronizeBooleans() {
        synchronizeBoolean(false);
        synchronizeBoolean(true);
    }

    @Test
    public void synchronizeByteArrays() {
        synchronizeByteArray(new byte[]{1, 2, 3, 4}, (byte) 6);
        synchronizeByteArray(new byte[]{5, 6, 7, 8}, (byte) 9);
    }

    @Test
    public void synchronizeIntArrays() {
        synchronizeIntArray(new int[]{1, 2, 3, 4}, 6);
        synchronizeIntArray(new int[]{5, 6, 7, 8}, 9);
    }

    @Test
    public void synchronizeArrays() {
        synchronizeArray(Integer.class, new Integer[]{1, 2, 3, 4}, 6);
        synchronizeArray(Double.class, new Double[]{1.0, 2.0, 3.0, 4.0}, 6.0);
        synchronizeArray(String.class, new String[]{"foo", "bar", "baz", "123"}, "bcx");
    }

    @Test
    public void synchronizeObjects() {
        synchronizeObject(String.class, "foo bar baz");
        synchronizeObject(A.class, new A(5.432f));
    }

    @Serializable
    public static final class A {
        @Serialize
        public float f = 1.234f;

        public A() {
        }

        public A(final float f) {
            this.f = f;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof A)) return false;
            final A other = (A) obj;
            return other.f == f;
        }
    }

    private static void synchronizeBoolean(final boolean value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedBoolean i1 = new SynchronizedBoolean(!value);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedBoolean i2 = new SynchronizedBoolean(!value);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static void synchronizeByte(final byte value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedByte i1 = new SynchronizedByte((byte) 42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedByte i2 = new SynchronizedByte((byte) 42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static void synchronizeShort(final short value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedShort i1 = new SynchronizedShort((short) 42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedShort i2 = new SynchronizedShort((short) 42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static void synchronizeInt(final int value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedInt i1 = new SynchronizedInt(42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedInt i2 = new SynchronizedInt(42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static void synchronizeLong(final long value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedLong i1 = new SynchronizedLong(42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedLong i2 = new SynchronizedLong(42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static void synchronizeChar(final char value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedChar i1 = new SynchronizedChar((char) 42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedChar i2 = new SynchronizedChar((char) 42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static void synchronizeFloat(final float value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedFloat i1 = new SynchronizedFloat(42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value, 0.0f);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedFloat i2 = new SynchronizedFloat(42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value, 0.0f);
    }

    private static void synchronizeDouble(final double value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedDouble i1 = new SynchronizedDouble(42);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value, 0.0);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedDouble i2 = new SynchronizedDouble(42);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value, 0.0);
    }

    private static void synchronizeString(final String value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedString i1 = new SynchronizedString("nope");
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedString i2 = new SynchronizedString("nope");

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }

    private static <T> void synchronizeByteArray(final byte[] data, final byte value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final byte[] copy = data.clone();

        final SynchronizedByteArray i1 = new SynchronizedByteArray(data);
        i1.setManager(manager);
        i1.set(2, value);

        assertEquals(i1.get(2), value);

        verify(manager, times(1)).setDirtyAdvanced(eq(i1), any());

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, Collections.singletonList(new TIntHashSet(new int[]{2})));

        final SynchronizedByteArray i2 = new SynchronizedByteArray(data);

        assertNotEquals(i2.get(2), value);

        i2.deserialize(packet);

        assertEquals(i2.get(2), value);

        assertArrayEquals(data, copy);
    }

    private static <T> void synchronizeIntArray(final int[] data, final int value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final int[] copy = data.clone();

        final SynchronizedIntArray i1 = new SynchronizedIntArray(data);
        i1.setManager(manager);
        i1.set(2, value);

        assertEquals(i1.get(2), value);

        verify(manager, times(1)).setDirty(i1, 2);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, Collections.singletonList(2));

        final SynchronizedIntArray i2 = new SynchronizedIntArray(data);

        assertNotEquals(i2.get(2), value);

        i2.deserialize(packet);

        assertEquals(i2.get(2), value);

        assertArrayEquals(data, copy);
    }

    private static <T> void synchronizeArray(final Class<T> elementType, final T[] data, final T value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final T[] copy = data.clone();

        final SynchronizedArray<T> i1 = new SynchronizedArray<>(elementType, data);
        i1.setManager(manager);
        i1.set(2, value);

        assertEquals(i1.get(2), value);

        verify(manager, times(1)).setDirty(i1, 2);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, Collections.singletonList(2));

        final SynchronizedArray<T> i2 = new SynchronizedArray<>(elementType, data);

        assertNotEquals(i2.get(2), value);

        i2.deserialize(packet);

        assertEquals(i2.get(2), value);

        assertArrayEquals(data, copy);
    }

    private static <T> void synchronizeObject(final Class<T> clazz, final T value) {
        final SynchronizationManagerServer manager = Mockito.mock(SynchronizationManagerServer.class);

        final SynchronizedObject<T> i1 = new SynchronizedObject<>(clazz);
        i1.setManager(manager);
        i1.set(value);

        assertEquals(i1.get(), value);

        verify(manager, times(1)).setDirty(i1, null);

        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        i1.serialize(packet, null);

        final SynchronizedObject<T> i2 = new SynchronizedObject<>(clazz);

        assertNotEquals(i2.get(), value);

        i2.deserialize(packet);

        assertEquals(i2.get(), value);
    }
}
