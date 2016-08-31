package li.cil.lib.client.renderer.font;

/**
 * Base interface for font renderers.
 */
public interface FontRenderer {
    /**
     * Render the specified string.
     *
     * @param string the string to render.
     */
    void drawString(final String string);

    /**
     * Render up to the specified amount of characters of the specified string.
     * <p>
     * This is intended as a convenience method for clamped-width rendering,
     * avoiding additional string operations such as <tt>substring</tt>.
     *
     * @param string   the string to render.
     * @param maxChars the maximum number of characters to render.
     */
    void drawString(final String string, final int maxChars);

    /**
     * Render a segment of a byte array representing a list of characters.
     *
     * @param chars  the byte array with the characters.
     * @param offset the offset in the array to start reading at.
     * @param length the number of characters to render.
     */
    void drawString(final byte[] chars, final int offset, final int length);

    /**
     * Get the width of the characters drawn with the font renderer, in pixels.
     *
     * @return the width of the drawn characters.
     */
    int getCharWidth();

    /**
     * Get the height of the characters drawn with the font renderer, in pixels.
     *
     * @return the height of the drawn characters.
     */
    int getCharHeight();
}
