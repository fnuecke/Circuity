package li.cil.circuity.common;

public final class Constants {
    public static final String BUS_CONTROLLER_NAME = "busController";
    public static final String BUS_CABLE_NAME = "busCable";
    public static final String EEPROM_NAME = "eeprom";
    public static final String EEPROM_READER_NAME = "eepromReader";
    public static final String PROCESSOR_Z80_NAME = "processorZ80";
    public static final String RANDOM_ACCESS_MEMORY_NAME = "randomAccessMemory";
    public static final String REDSTONE_CONTROLLER_NAME = "redstoneController";
    public static final String SERIAL_CONSOLE_NAME = "serialConsole";

    // --------------------------------------------------------------------- //

    /*
     * Default addresses for some devices. These are merely that, defaults.
     * They can be adjusted by the user via the bus controller's GUI at any
     * time. However, the default layout is still recommended as it will allow
     * reusing programs across different computers, without having to adjust
     * them for the new memory layout.
     */

    public static final int MEMORY_ADDRESS = 0x0000; // up to 0x6FFF
    public static final int GRAPHICS_CARD_ADDRESS = 0x7000; // up to 0xBFFF
    public static final int EEPROM_ADDRESS = 0xC000; // up to 0xCFFF
    public static final int DISK_DRIVE_ADDRESS = 0xD000;
    public static final int REDSTONE_CONTROLLER_ADDRESS = 0xD000;
    public static final int SERIAL_CONSOLE_ADDRESS = 0xD000;

    public static final int BUS_CONTROLLER_ADDRESS = 0x10000;

    private Constants() {
    }
}
