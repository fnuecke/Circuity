package li.cil.lib.capabilities;

public enum NullFactory {
    ;

    public static <T> T create() {
        return null;
    }
}
