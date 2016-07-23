package li.cil.circuity.api.bus.device;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

@Serializable
public abstract class AbstractAddressableInterruptSink extends AbstractAddressable implements InterruptSink {
    @Serialize
    protected int[] interruptSinks;

    protected abstract int[] validateInterrupts(final int[] interrupts);

    @Override
    public int[] getAcceptedInterrupts(final int[] interrupts) {
        if (this.interruptSinks != null) {
            return this.interruptSinks;
        } else {
            return validateInterrupts(interrupts);
        }
    }

    @Override
    public void setAcceptedInterrupts(@Nullable final int[] interrupts) {
        this.interruptSinks = interrupts;
    }
}
