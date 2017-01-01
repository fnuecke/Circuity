package li.cil.lib.common;

import li.cil.lib.api.GlobalObjectsAPI;
import li.cil.lib.api.SillyBeeAPI;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public enum GlobalObjects implements GlobalObjectsAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final Object lock = new Object();
    private final Map<World, Map<UUID, WeakReference<Object>>> worlds = new WeakHashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.globalObjects = INSTANCE;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void put(final World world, final UUID key, final Object value) {
        synchronized (lock) {
            worlds.computeIfAbsent(world, k -> new HashMap<>()).put(key, new WeakReference<>(value));
        }
    }

    @Override
    public void remove(final World world, final UUID key) {
        synchronized (lock) {
            final Map<UUID, WeakReference<Object>> objects = worlds.get(world);
            if (objects != null) {
                objects.remove(key);
            }
        }
    }

    @Nullable
    @Override
    public Object get(final World world, final UUID key) {
        synchronized (lock) {
            final Map<UUID, WeakReference<Object>> objects = worlds.get(world);
            if (objects != null) {
                final WeakReference<Object> valueRef = objects.get(key);
                if (valueRef != null) {
                    final Object value = valueRef.get();
                    if (value != null) {
                        return value;
                    } else {
                        objects.remove(key);
                    }
                }
            }
        }
        return null;
    }
}
