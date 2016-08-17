package li.cil.circuity.common.bus;

import li.cil.circuity.api.CircuityAPI;
import li.cil.lib.api.SillyBeeAPI;
import net.minecraftforge.common.MinecraftForge;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public enum BusThreadPool {
    INSTANCE;

    private static final int CORE_POOL_SIZE = 8;

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<>(), new BusThreadFactory());

    BusThreadPool() {
        SillyBeeAPI.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public Future submit(final Runnable task) {
        return pool.submit(task);
    }

    private static final class BusThreadFactory implements ThreadFactory {
        private final ThreadFactory factory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = factory.newThread(r);
            t.setName(t.getName() + " (" + CircuityAPI.MOD_NAME + " Bus)");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
