package li.cil.circuity.api.bus.device;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;

import javax.annotation.Nullable;

@Serializable
public abstract class AbstractInterruptSource extends AbstractAddressable implements InterruptSource {
    @Serialize
    private int[] interruptsSources;

    protected abstract int[] validateInterrupts(final InterruptList interrupts);

    protected void triggerInterrupt(final int index, final int data) {
        final int interruptId = interruptsSources[index];
        controller.interrupt(interruptId, data);
    }

    @Override
    public final int[] getEmittedInterrupts(final InterruptList interrupts) {
        if (this.interruptsSources != null) {
            return this.interruptsSources;
        } else {
            return validateInterrupts(interrupts);
        }
    }

    @Override
    public final void setEmittedInterrupts(@Nullable final int[] interrupts) {
        this.interruptsSources = interrupts;
    }
}
