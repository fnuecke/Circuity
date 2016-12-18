package li.cil.lib.capabilities;

import javax.annotation.Nullable;

public enum NullFactory {
    ;

    @Nullable
    public static <T> T create() {
        return null;
    }
}
