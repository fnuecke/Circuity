package li.cil.lib.api.gui.layout;

/**
 * Alignment constants used by {@link Layout}s and some {@link li.cil.lib.api.gui.widget.Widget}s.
 */
public final class Alignment {
    /**
     * Vertical alignment options, i.e. along the Y/up axis.
     */
    public enum Vertical {
        /**
         * Top aligned, e.g. the top of the element touches the top of its container.
         */
        TOP,

        /**
         * Middle aligned, e.g. the element is vertically centered in its container.
         */
        MIDDLE,

        /**
         * Bottom aligned, e.g. the bottom of the element touches the bottom of its container.
         */
        BOTTOM;

        public int computeOffset(final int inner, final int outer) {
            return Alignment.computeOffset(ordinal(), inner, outer);
        }
    }

    /**
     * Horizontal alignment options, i.e. along the X/right axis.
     */
    public enum Horizontal {
        /**
         * Left aligned, e.g. the left of the element touches the left of its container.
         */
        LEFT,

        /**
         * Center aligned, e.g. the element is horizontally centered in its container.
         */
        CENTER,

        /**
         * Right aligned, e.g. the right of the element touches the right of its container.
         */
        RIGHT;

        public int computeOffset(final int inner, final int outer) {
            return Alignment.computeOffset(ordinal(), inner, outer);
        }
    }

    // --------------------------------------------------------------------- //

    private static int computeOffset(final int alignment, final int inner, final int outer) {
        switch (alignment) {
            case 0:
                return 0;
            case 1:
                return (outer - inner) / 2;
            case 2:
                return outer - inner;
            default:
                throw new IllegalStateException();
        }
    }

    // --------------------------------------------------------------------- //

    private Alignment() {
    }
}
