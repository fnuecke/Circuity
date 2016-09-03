package li.cil.lib;

import com.google.common.util.concurrent.Futures;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.serialization.SerializerCollection;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class SerializationTest {
    private final SerializerCollection s;

    public SerializationTest() {
        Serialization.init();
        s = SillyBeeAPI.serialization.get(Side.SERVER);
    }

    @Test
    public void getSerializers() throws Exception {
        assertNotNull(SillyBeeAPI.serialization.get(Side.CLIENT));
        assertNotNull(SillyBeeAPI.serialization.get(Side.SERVER));

        assertNotEquals(SillyBeeAPI.serialization.get(Side.CLIENT), SillyBeeAPI.serialization.get(Side.SERVER));
    }

    @Test
    public void serializeByte() {
        testRoundabout(s, Byte.MIN_VALUE, byte.class);
        testRoundabout(s, Byte.MAX_VALUE, byte.class);
        testRoundabout(s, (byte) -1, byte.class);
        testRoundabout(s, (byte) -0, byte.class);
        testRoundabout(s, (byte) 0, byte.class);
        testRoundabout(s, (byte) 1, byte.class);
        testRoundabout(s, (byte) -123, byte.class);
        testRoundabout(s, (byte) 123, byte.class);
    }

    @Test
    public void serializeShort() {
        testRoundabout(s, Short.MIN_VALUE, short.class);
        testRoundabout(s, Short.MAX_VALUE, short.class);
        testRoundabout(s, (short) -1, short.class);
        testRoundabout(s, (short) -0, short.class);
        testRoundabout(s, (short) 0, short.class);
        testRoundabout(s, (short) 1, short.class);
        testRoundabout(s, (short) -123, short.class);
        testRoundabout(s, (short) 123, short.class);
    }

    @Test
    public void serializeInt() {
        testRoundabout(s, Integer.MIN_VALUE, int.class);
        testRoundabout(s, Integer.MAX_VALUE, int.class);
        testRoundabout(s, -1, int.class);
        testRoundabout(s, -0, int.class);
        testRoundabout(s, 0, int.class);
        testRoundabout(s, 1, int.class);
        testRoundabout(s, -123, int.class);
        testRoundabout(s, 123, int.class);
    }

    @Test
    public void serializeLong() {
        testRoundabout(s, Long.MIN_VALUE, long.class);
        testRoundabout(s, Long.MAX_VALUE, long.class);
        testRoundabout(s, -1L, long.class);
        testRoundabout(s, -0L, long.class);
        testRoundabout(s, 0L, long.class);
        testRoundabout(s, 1L, long.class);
        testRoundabout(s, -123L, long.class);
        testRoundabout(s, 123L, long.class);
    }

    @Test
    public void serializeFloat() {
        testRoundabout(s, Float.MIN_VALUE, float.class);
        testRoundabout(s, Float.MAX_VALUE, float.class);
        testRoundabout(s, Float.NEGATIVE_INFINITY, float.class);
        testRoundabout(s, Float.POSITIVE_INFINITY, float.class);
        testRoundabout(s, Float.NaN, float.class);
        testRoundabout(s, -1.0f, float.class);
        testRoundabout(s, -0.0f, float.class);
        testRoundabout(s, 0.0f, float.class);
        testRoundabout(s, 1.0f, float.class);
        testRoundabout(s, -123.45f, float.class);
        testRoundabout(s, 123.45f, float.class);
    }

    @Test
    public void serializeDouble() {
        testRoundabout(s, Double.MIN_VALUE, double.class);
        testRoundabout(s, Double.MAX_VALUE, double.class);
        testRoundabout(s, Double.NEGATIVE_INFINITY, double.class);
        testRoundabout(s, Double.POSITIVE_INFINITY, double.class);
        testRoundabout(s, Double.NaN, double.class);
        testRoundabout(s, -1.0, double.class);
        testRoundabout(s, -0.0, double.class);
        testRoundabout(s, 0.0, double.class);
        testRoundabout(s, 1.0, double.class);
        testRoundabout(s, -123.45, double.class);
        testRoundabout(s, 123.45, double.class);
    }

    @Test
    public void serializeBoolean() {
        testRoundabout(s, false, boolean.class);
        testRoundabout(s, true, boolean.class);
    }

    @Test
    public void serializeChar() {
        testRoundabout(s, Character.MIN_VALUE, char.class);
        testRoundabout(s, Character.MAX_VALUE, char.class);
        testRoundabout(s, (char) -1, char.class);
        testRoundabout(s, (char) -0, char.class);
        testRoundabout(s, (char) 0, char.class);
        testRoundabout(s, (char) 1, char.class);
        testRoundabout(s, (char) -123, char.class);
        testRoundabout(s, (char) 123, char.class);
        testRoundabout(s, 'a', char.class);
        testRoundabout(s, 'A', char.class);
        testRoundabout(s, 'z', char.class);
        testRoundabout(s, 'Z', char.class);
        testRoundabout(s, '\uBEEF', char.class);
    }

    @Test
    public void serializeString() {
        testRoundabout(s, "", String.class);
        testRoundabout(s, "foo bar baz", String.class);
        testRoundabout(s, "foo \0 bar", String.class);
        testRoundabout(s, "foo \uBEEF bar", String.class);
    }

    @Test
    public void serializeByteArray() {
        {
            final byte[] data1 = new byte[]{};
            final byte[] data2 = s.deserialize(byte[].class, s.serialize(data1));
            assertArrayEquals(data1, data2);
        }

        {
            final byte[] data1 = new byte[]{1, 2, 3, 4};
            final byte[] data2 = s.deserialize(byte[].class, s.serialize(data1));
            assertArrayEquals(data1, data2);
        }
    }

    @Test
    public void serializeIntArray() {
        {
            final int[] data1 = new int[]{};
            final int[] data2 = s.deserialize(int[].class, s.serialize(data1));
            assertArrayEquals(data1, data2);
        }
        {
            final int[] data1 = new int[]{1, 2, 3, 4};
            final int[] data2 = s.deserialize(int[].class, s.serialize(data1));
            assertArrayEquals(data1, data2);
        }
    }

    @Test
    public void serializeEnum() {
        testRoundabout(s, TestEnum.ONE, TestEnum.class);
        testRoundabout(s, TestEnum.TWO, TestEnum.class);
        testRoundabout(s, TestEnum.THREE, TestEnum.class);
    }

    @Test
    public void serializeClass() {
        testRoundabout(s, Object.class, Class.class);
        testRoundabout(s, String.class, Class.class);
        testRoundabout(s, int.class, Class.class);
    }

    @Test
    public void serializeFuture() {
        {
            final Future<String> data1 = Futures.immediateFuture("foo bar");
            final Future data2 = s.deserialize(Future.class, s.serialize(data1));
            try {
                assertEquals(data1.get(), data2.get());
            } catch (InterruptedException | ExecutionException e) {
                fail("wat");
            }
        }
    }

    @Test
    public void serializeObjectArray() {
        {
            final String[] data1 = new String[]{};
            final String[] data2 = s.deserialize(String[].class, s.serialize(data1));
            assertArrayEquals(data1, data2);
        }
        {
            final String[] data1 = new String[]{"foo", "bar", "baz"};
            final String[] data2 = s.deserialize(String[].class, s.serialize(data1));
            assertArrayEquals(data1, data2);
        }
    }

    @Test
    public void serializeObjectArrayInto() {
        {
            final String[] data1 = new String[]{"foo", "bar", "baz"};
            final String[] data2 = new String[]{"lol", "wat", "hey"};
            final String[] data3 = s.deserialize(data2, s.serialize(data1));
            assertArrayEquals(data1, data3);
            assertArrayEquals(data2, data3); // Same length, reused.
        }
    }

    @Test
    public void serializeCollection() {
        {
            final List<String> data1 = new ArrayList<>();
            final List<String> data2 = s.deserialize(ArrayList.class, s.serialize(data1));
            assertArrayEquals(data1.toArray(), data2.toArray());
        }
        {
            final List<String> data1 = Arrays.asList("foo", "bar", "baz");
            final List<String> data2 = s.deserialize(ArrayList.class, s.serialize(data1));
            assertArrayEquals(data1.toArray(), data2.toArray());
        }

        {
            final Set<String> data1 = new HashSet<>();
            final Set<String> data2 = s.deserialize(HashSet.class, s.serialize(data1));
            assertArrayEquals(data1.toArray(), data2.toArray());
        }
        {
            final Set<String> data1 = new HashSet<>(Arrays.asList("foo", "bar", "baz"));
            final Set<String> data2 = s.deserialize(HashSet.class, s.serialize(data1));
            assertArrayEquals(data1.toArray(), data2.toArray());
        }
    }

    @Test
    public void serializeMap() {
        {
            final Map<String, String> data1 = new HashMap<>();
            final Map<String, String> data2 = s.deserialize(HashMap.class, s.serialize(data1));

            assertMapEquals(data1, data2);
        }
        {
            final Map<String, String> data1 = new HashMap<>();
            data1.put("foo", "bar");
            data1.put("baz", "wtf");
            data1.put("omg", "lol");
            final Map<String, String> data2 = s.deserialize(HashMap.class, s.serialize(data1));

            assertMapEquals(data1, data2);
        }
    }

    private static <K, V> void assertMapEquals(final Map<K, V> a, final Map<K, V> b) {
        assertEquals(a.size(), b.size());
        for (final Map.Entry<K, V> entry : a.entrySet()) {
            final K key = entry.getKey();
            final V value1 = entry.getValue();

            assertTrue(b.containsKey(key));
            final V value2 = b.get(key);
            assertEquals(value1, value2);
        }
    }

    @Test
    public void serializeSerializable() {
        {
            final A a1 = new A();
            a1.b = false;
            a1.increment();
            final A a2 = s.deserialize(A.class, s.serialize(a1));

            assertEquals(a1, a2);
        }

        {
            final A a1 = new A();
            a1.b = false;
            a1.increment();

            final A a2 = new A();
            a2.increment();
            a2.increment();

            final B b1 = new B();
            b1.a = a1;
            b1.is = new int[]{3, 4, 5};
            b1.as.add(a1);
            b1.as.add(a2);

            final B b2 = s.deserialize(B.class, s.serialize(b1));

            assertEquals(b1.a, b2.a);
            assertArrayEquals(b1.is, b2.is);
            assertArrayEquals(b1.as.toArray(), b2.as.toArray());
        }
    }

    public enum TestEnum {
        ONE,
        TWO,
        THREE
    }

    @Serializable
    public static class A {
        @Serialize
        public boolean b = true;

        @Serialize
        private int i = 10;

        public void increment() {
            i++;
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof A)) return false;
            final A other = (A) obj;
            return other.b == b && other.i == i;
        }
    }

    @Serializable
    private static final class B {
        @Serialize
        public A a;

        @Serialize
        public int[] is = new int[]{1, 2, 3};

        @Serialize
        public List<A> as = new ArrayList<>();
    }

    private static <T> void testRoundabout(final SerializerCollection s, final T data, final Class<T> clazz) {
        assertEquals(data, s.deserialize(clazz, s.serialize(data)));
    }
}