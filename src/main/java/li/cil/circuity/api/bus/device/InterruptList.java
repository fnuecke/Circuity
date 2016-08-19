package li.cil.circuity.api.bus.device;

/**
 * Utility class for allocating interrupt IDs in a way that tries to close
 * gaps in the existing list of interrupt IDs.
 */
public final class InterruptList {
    /**
     * This will contain the list of "holes" in the sequence of already
     * occupied interrupts IDs, as well as at least the last unused interrupt
     * ID. For example, if interrupts <code>1</code> and <code>4</code> are
     * already occupied, the list will be <code>{0,2,3,5}</code>. If no
     * interrupts are occupied, the list will be <code>{0}</code>. If at an
     * earlier time four interrupts were occupied, but now only interrupt
     * <code>1</code> is occupied, the list will be <code>{0,2,3}</code>.
     */
    public final int[] interrupts;

    public InterruptList(final int[] interrupts) {
        this.interrupts = interrupts;
    }

    /**
     * Build an interrupt ID list of the specified length using as many free
     * interrupts as possible.
     * <p>
     * This will use up all interrupt IDs present in this list before
     * generating additional interrupt IDs.
     *
     * @param count the number of interrupt IDs to get.
     * @return the list of interrupt IDs.
     */
    public int[] take(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }

        assert interrupts.length > 0 : "Trying to generate interrupts from the empty interrupt list.";

        final int[] result = new int[count];
        for (int i = 0, n = Math.min(result.length, interrupts.length); i < n; i++) {
            result[i] = interrupts[i];
        }
        for (int i = 0, n = result.length - interrupts.length; i < n; i++) {
            result[interrupts.length + i] = interrupts[interrupts.length - 1] + 1 + i;
        }
        return result;
    }
}
