package li.cil.lib;

import li.cil.lib.api.SerializationAPI;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.serialization.SerializerCollection;
import li.cil.lib.serialization.SerializerCollectionImpl;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

public enum Serialization implements SerializationAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private final SerializerCollection client = new SerializerCollectionImpl(true);
    private final SerializerCollection server = new SerializerCollectionImpl(false);
    private final Map<String, Class<?>> classRemapping = new HashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.serialization = INSTANCE;
    }

    // --------------------------------------------------------------------- //

    @Override
    public SerializerCollection get(final Side side) {
        return get(side.isClient());
    }

    @Override
    public SerializerCollection get(final boolean isRemote) {
        return isRemote ? client : server;
    }

    @Override
    public SerializerCollection get(final World world) {
        return get(world.isRemote);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void addClassRemapping(final String className, final Class<?> remappedClass) {
        classRemapping.put(className, remappedClass);
    }

    @Override
    public Class<?> getRemappedClass(final String className) throws ClassNotFoundException {
        final Class<?> clazz = classRemapping.get(className);
        return (clazz != null) ? clazz : ReflectionUtil.getClass(className);
    }
}
