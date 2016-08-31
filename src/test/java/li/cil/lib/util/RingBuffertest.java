package li.cil.lib.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class RingBuffertest {
    @Test
    public void simple() {
        final RingBuffer buffer = new RingBuffer(8);
        buffer.write((byte) 1);
        buffer.write((byte) 2);
        buffer.write((byte) 3);
        buffer.write((byte) 4);
        buffer.write((byte) 5);
        buffer.write((byte) 6);
        buffer.write((byte) 7);
        buffer.write((byte) 8);

        assertFalse(buffer.isWritable());
        assertTrue(buffer.isReadable());

        assertEquals(1, buffer.read());
        assertEquals(2, buffer.read());
        assertEquals(3, buffer.read());
        assertEquals(4, buffer.read());
        assertEquals(5, buffer.read());
        assertEquals(6, buffer.read());
        assertEquals(7, buffer.read());
        assertEquals(8, buffer.read());

        assertTrue(buffer.isWritable());
        assertFalse(buffer.isReadable());

        buffer.write((byte) 1);
        buffer.write((byte) 2);
        buffer.write((byte) 3);
        buffer.write((byte) 4);

        assertTrue(buffer.isWritable());
        assertTrue(buffer.isReadable());

        assertEquals(1, buffer.read());
        assertEquals(2, buffer.read());

        buffer.write((byte) 5);
        buffer.write((byte) 6);
        buffer.write((byte) 7);
        buffer.write((byte) 8);

        assertEquals(3, buffer.read());
        assertEquals(4, buffer.read());

        assertTrue(buffer.isWritable());
        assertTrue(buffer.isReadable());
    }

    @Test(expected = IllegalStateException.class)
    public void underflow() {
        final RingBuffer buffer = new RingBuffer(8);

        buffer.write((byte) 1);
        buffer.write((byte) 2);

        assertEquals(1, buffer.read());
        assertEquals(2, buffer.read());

        buffer.read();
    }

    @Test(expected = IllegalStateException.class)
    public void overflow() {
        final RingBuffer buffer = new RingBuffer(8);

        buffer.write((byte) 1);
        buffer.write((byte) 2);

        assertEquals(1, buffer.read());
        assertEquals(2, buffer.read());

        buffer.write((byte) 3);
        buffer.write((byte) 4);
        buffer.write((byte) 5);
        buffer.write((byte) 6);
        buffer.write((byte) 7);
        buffer.write((byte) 8);
        buffer.write((byte) 8);
        buffer.write((byte) 10);

        buffer.write((byte) 11);
    }
}
