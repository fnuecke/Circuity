package li.cil.circuity.common;

import li.cil.circuity.api.CircuityAPI;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public enum Settings {
    INSTANCE;

    public static void init(final File file) {
        final Configuration config = new Configuration(file, CircuityAPI.MOD_VERSION);
    }
}
