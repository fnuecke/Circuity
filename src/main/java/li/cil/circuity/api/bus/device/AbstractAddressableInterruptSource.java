package li.cil.circuity.api.bus.device;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

@Serializable
public abstract class AbstractAddressableInterruptSource extends AbstractAddressable implements InterruptSource {
    @Serialize
    protected int[] interruptsSources;

    protected abstract int[] validateInterrupts(final int[] interrupts);

    @Override
    public int[] getEmittedInterrupts(final int[] interrupts) {
        if (this.interruptsSources != null) {
            return this.interruptsSources;
        } else {
            return validateInterrupts(interrupts);
        }
    }

    @Override
    public void setEmittedInterrupts(@Nullable final int[] interrupts) {
        this.interruptsSources = interrupts;
    }
}
