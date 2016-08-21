package li.cil.circuity.common;

public final class Constants {
    /**
     * The tick rate of Minecraft, just to avoid that magic number everywhere.
     */
    public static final int TICKS_PER_SECOND = 20;

    // --------------------------------------------------------------------- //

    public static final String BUS_CONTROLLER_NAME = "bus_controller";
    public static final String BUS_CABLE_NAME = "bus_cable";
    public static final String CONFIGURATOR_NAME = "configurator";
    public static final String EEPROM_NAME = "eeprom";
    public static final String EEPROM_READER_NAME = "eeprom_reader";
    public static final String PROCESSOR_Z80_NAME = "processor_z80";
    public static final String RANDOM_ACCESS_MEMORY_NAME = "random_access_memory";
    public static final String REDSTONE_CONTROLLER_NAME = "redstone_controller";
    public static final String SERIAL_CONSOLE_NAME = "serial_console";

    // --------------------------------------------------------------------- //

    /**
     * Localization string IDs present in the external localization file.
     */
    public static final class I18N {
        public static final String BUS_ERROR_MULTIPLE_CONTROLLERS = "bus.error.multiple_controllers";
        public static final String BUS_ERROR_ADDRESSES_OVERLAP = "bus.error.addresses_overlap";
        public static final String BUS_ERROR_SEGMENT_FAILED = "bus.error.segment_failed";

        public static final String CONFIGURATOR_MODE_CHANGED = "configurator.mode_changed";
        public static final String CONFIGURATOR_CLEARED = "configurator.cleared";
        public static final String CONFIGURATOR_ADDRESS_SELECTED = "configurator.address_selected";
        public static final String CONFIGURATOR_INTERRUPT_SOURCE = "configurator.interrupt_source";
        public static final String CONFIGURATOR_NO_INTERRUPT_SOURCE = "configurator.no_interrupt_source";
        public static final String CONFIGURATOR_INTERRUPT_SINK = "configurator.interrupt_sink";
        public static final String CONFIGURATOR_NO_INTERRUPT_SINK = "configurator.no_interrupt_sink";
        public static final String CONFIGURATOR_INTERRUPT_SET = "configurator.interrupt_set";
        public static final String CONFIGURATOR_INTERRUPT_CLEARED = "configurator.interrupt_cleared";

        public static final String INTERRUPT_SINK_NON_MASKABLE_INTERRUPT = "interrupt_sink.nmi";
        public static final String INTERRUPT_SINK_INTERRUPT_REQUEST = "interrupt_sink.irq";
        public static final String INTERRUPT_SOURCE_KEYBOARD_INPUT = "interrupt_source.keyboard_input";

        public static final String UNKNOWN = "generic.unknown";

        private I18N() {
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * Device info constants.
     */
    public static final class DeviceInfo {
        public static final String BUS_CONTROLLER_NAME = "Bus Controller";
        public static final String EEPROM_READER_NAME = "EEPROM Reader";
        public static final String HARD_DISK_DRIVE_NAME = "HDD";
        public static final String RANDOM_ACCESS_MEMORY_NAME = "RAM";
        public static final String REDSTONE_CONTROLLER_NAME = "Redstone Controller";
        public static final String SERIAL_CONSOLE_NAME = "Serial Console";

        private DeviceInfo() {
        }
    }

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

    public static final int BUS_CONTROLLER_ADDRESS = 0x10000;
    public static final int SERIAL_CONSOLE_ADDRESS = 0x10020;
    public static final int DISK_DRIVE_ADDRESS = 0x10030;
    public static final int REDSTONE_CONTROLLER_ADDRESS = 0x10040;

    // --------------------------------------------------------------------- //

    private Constants() {
    }
}
