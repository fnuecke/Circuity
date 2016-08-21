package li.cil.circuity.api.bus.device;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.Arrays;

@Serializable
public abstract class AbstractInterruptSink extends AbstractBusDevice implements InterruptSink {
    @Serialize
    private int[] interruptSinks;

    protected abstract int[] validateAcceptedInterrupts(final InterruptList interrupts);

    protected abstract void interruptIndexed(final int interrupt, final int data);

    protected abstract ITextComponent getInterruptNameIndexed(final int interrupt);

    @Override
    public final int[] getAcceptedInterrupts(final InterruptList interrupts) {
        if (this.interruptSinks != null) {
            return this.interruptSinks;
        } else {
            return validateAcceptedInterrupts(interrupts);
        }
    }

    @Override
    public final void setAcceptedInterrupts(@Nullable final int[] interrupts) {
        this.interruptSinks = interrupts;
    }

    @Nullable
    @Override
    public ITextComponent getInterruptName(final int interruptId) {
        if (interruptSinks == null) return null;
        final int index = Arrays.binarySearch(interruptSinks, interruptId);
        return getInterruptNameIndexed(index);
    }

    @Override
    public final void interrupt(final int interruptId, final int data) {
        if (interruptSinks == null) return;
        final int index = Arrays.binarySearch(interruptSinks, interruptId);
        interruptIndexed(index, data);
    }
}
