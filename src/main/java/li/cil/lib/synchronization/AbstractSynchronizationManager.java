package li.cil.lib.synchronization;

import li.cil.lib.api.synchronization.SynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractSynchronizationManager implements SynchronizationManager {
    protected static final String FIELD_TAG = "field";
    protected static final String VALUE_TAG = "value";

    // --------------------------------------------------------------------- //

    protected final List<Class> typeById = new ArrayList<>();
    protected final HashMap<Class, Integer> idByType = new HashMap<>();

    // --------------------------------------------------------------------- //

    protected int registerType(final Class type) {
        final int typeId = typeById.size();
        typeById.add(type);
        idByType.put(type, typeId);
        return typeId;
    }
}
