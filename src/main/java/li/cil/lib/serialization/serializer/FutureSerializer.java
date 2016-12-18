package li.cil.lib.serialization.serializer;

import com.google.common.util.concurrent.Futures;
import li.cil.lib.serialization.SerializationException;
import li.cil.lib.serialization.SerializerCollectionImpl;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FutureSerializer implements Serializer {
    private static final String CLASS_TAG = "class";
    private static final String VALUE_TAG = "value";

    // --------------------------------------------------------------------- //

    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public FutureSerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return Future.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final Future future = (Future) object;
        try {
            final Object value = future.get(50, TimeUnit.MILLISECONDS);
            final NBTTagCompound futureInfo = new NBTTagCompound();
            if (value != null) {
                futureInfo.setTag(CLASS_TAG, serialization.serialize(value.getClass()));
                futureInfo.setTag(VALUE_TAG, serialization.serialize(value));
            }
            return futureInfo;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new SerializationException("Trying to serialize unreached future.", e);
        }
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tag) {
        if (clazz != Future.class) {
            throw new SerializationException("Can only deserialize futures as Future.");
        }
        final NBTTagCompound futureInfo = (NBTTagCompound) tag;
        if (futureInfo.hasKey(CLASS_TAG) && futureInfo.hasKey(VALUE_TAG)) {
            final Class<?> valueClazz = serialization.deserialize(Class.class, futureInfo.getTag(CLASS_TAG));
            if (valueClazz != null) {
                final NBTBase valueTag = futureInfo.getTag(VALUE_TAG);
                return new LazyFuture(() -> serialization.deserialize(valueClazz, valueTag));
            }
        }
        return Futures.immediateFuture(null);
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }

    // --------------------------------------------------------------------- //

    // TODO Is there nothing like this in the google/apache libs?
    private static final class LazyFuture implements Future {
        private final Supplier supplier;
        private Object value;
        private boolean didEvaluate;

        public LazyFuture(final Supplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }

        @Nullable
        @Override
        public Object get() throws InterruptedException, ExecutionException {
            if (!didEvaluate) {
                didEvaluate = true;
                value = supplier.get();
            }
            return value;
        }

        @Nullable
        @Override
        public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            checkNotNull(unit);
            return get();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }
    }
}
