package li.cil.circuity.api.bus.device;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;
import java.util.Arrays;

@Serializable
public abstract class AbstractInterruptSink extends AbstractBusDevice implements InterruptSink {
    @Serialize
    private int[] interruptSinks;

    protected abstract int[] validateInterrupts(final InterruptList interrupts);

    protected abstract void handleInterrupt(final int interrupt, final int data);

    @Override
    public final int[] getAcceptedInterrupts(final InterruptList interrupts) {
        if (this.interruptSinks != null) {
            return this.interruptSinks;
        } else {
            return validateInterrupts(interrupts);
        }
    }

    @Override
    public final void setAcceptedInterrupts(@Nullable final int[] interrupts) {
        this.interruptSinks = interrupts;
    }

    @Override
    public final void interrupt(final int interruptId, final int data) {
        if (interruptSinks == null) return;
        final int index = Arrays.binarySearch(interruptSinks, interruptId);
        handleInterrupt(index, data);
    }
}
