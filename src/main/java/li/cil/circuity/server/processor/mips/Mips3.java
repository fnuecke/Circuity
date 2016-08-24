package li.cil.circuity.server.processor.mips;

import li.cil.circuity.common.bus.AbstractBusController;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.lib.api.serialization.Serialize;

/** MIPS3 implementation based on the NEC VR4300 and IDT R4600.
 *
 * What's missing:
 * - just about everything
 * - big-endian mode (little-endian is used exclusively)
 * - TLB
 * - caches
 * - the entirety of COP0
 * - FPA (that is, COP1)
 */
public class Mips3 {

    public static final int TLB_COUNT = 48;

    //public static final long VECTOR_BASE_RESET = 0xFFFFFFFFBFC00000L; // real location
    public static final long VECTOR_BASE_RESET = 0xFFFFFFFFA0000000L; // temporary measure
    public static final long VECTOR_BASE_RAM = 0xFFFFFFFF80000000L;
    public static final long VECTOR_BASE_ROM = VECTOR_BASE_RESET + 0x100;

    private BusControllerAccess memory;
    private final Object lock = new Object();

    // Program counter
    @Serialize
    private long pc = VECTOR_BASE_RESET;

    // Op fetched into pipeline
    @Serialize
    private long pl0_pc = 0;
    @Serialize
    private int pl0_op = 0;
    @Serialize
    private boolean pl0_bd = false;

    // Registers
    // [0] is always 0
    // mdlo, mdhi are the multiply/divide temp regs
    @Serialize
    private long[] regs = new long[32];
    @Serialize
    private long[] c0regs = new long[32];
    @Serialize
    private long mdlo, mdhi;

    // COP0 register names
    public static final int C0_INDEX = 0;
    public static final int C0_RANDOM = 1;
    public static final int C0_ENTRYLO0 = 2;
    public static final int C0_ENTRYLO1 = 3;
    public static final int C0_CONTEXT = 4;
    public static final int C0_PAGEMASK = 5;
    public static final int C0_WIRED = 6;
    public static final int C0_BADVADDR = 8;
    public static final int C0_COUNT = 9;
    public static final int C0_ENTRYHI = 10;
    public static final int C0_COMPARE = 11;
    public static final int C0_STATUS = 12;
    public static final int C0_CAUSE = 13;
    public static final int C0_EPC = 14;
    public static final int C0_PRID = 15;
    public static final int C0_CONFIG = 16;
    public static final int C0_LLADDR = 17;
    public static final int C0_XCONTEXT = 20;
    public static final int C0_ECC = 26;
    public static final int C0_CACHEERR = 27;
    public static final int C0_TAGLO = 28;
    public static final int C0_TAGHI = 29;
    public static final int C0_ERROREPC = 30;

    // Pipeline stages
    public enum MPipelineStage {
        // Classic 5-stage MIPS pipeline
        //
        // Standard MIPS3 uses a weird 8-stage one
        // but both NEC's and IDT's offerings just use 5-stage
        //
        IC(1, "Instruction Cache Fetch"),
        RF(2, "Register Fetch"),
        EX(3, "Execute"),
        DC(4, "Data Cache Fetch"),
        WB(5, "Write Back"),

        // Special "nothing is happening anywhere" index
        NONE(0, "Good Boy");

        public final int idx;
        public final String name;

        MPipelineStage(int idx, String name) {
            this.idx = idx;
            this.name = name;
        }
    }

    // Fault codes
    public enum MFault {
        Int(0, "Interrupt"),
        Mod(1, "TLB modification"),
        TLBL(2, "TLB load"),
        TLBS(3, "TLB store"),
        AdEL(4, "Address error load"), // Includes instruction fetches
        AdES(5, "Address error store"),
        IBE(6, "Bus error on ifetch"),
        DBE(7, "Bus error on dfetch"), // DOES NOT HAPPEN ON STORES.
        Syscall(8, "SYSCALL instruction"),
        Bp(9, "BREAK instruction (breakpoint)"),
        RI(10, "Reserved instruction"),
        CpU(11, "Coprocessor unusable"),
        Ov(12, "Arithmetic overflow"),

        // MIPS3+ exceptions
        Tr(13, "Trap exception"),
        FPE(15, "Floating point exception"),
        WATCH(23, "Watchpoint exception"),

        // Special "nothing is wrong at all" code
        NONE(-1, "The operation completed successfully.");

        public final int code;
        public final String name;

        MFault(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    // Fault info strobes
    @Serialize
    private MFault fault_type = MFault.NONE;
    @Serialize
    private long fault_pc = 0;
    @Serialize
    private MPipelineStage fault_stage = MPipelineStage.NONE;
    @Serialize
    private boolean fault_bd = false;

    // Cache policy
    public static final int CACHE_WTHRU_NOWALLOC = 0x0;
    public static final int CACHE_WTHRU_WALLOC = 0x1;
    public static final int CACHE_NONE = 0x2;
    public static final int CACHE_WBACK = 0x3;

    // Exceptions
    public class MipsAddressErrorException extends Exception {}
    public class MipsBusErrorException extends Exception {}

    // Cycles for timing
    @Serialize
    private int cycleBudget = 0;

    // Mode masks
    @Serialize
    private boolean allow64 = true;
    @Serialize
    private boolean address64 = true;
    @Serialize
    private boolean kernelMode = true;
    @Serialize
    private boolean superMode = true;

    // Actual code!

    // Constructor

    public Mips3(BusControllerAccess memory) {
        this.memory = memory;
        this.reset();
    }

    // Faults

    private void fault(MFault code, MPipelineStage stage, long pc, boolean bd) {
        // Faults from later stages take priority
        if(stage.idx <= this.fault_stage.idx) {
            return;
        }

        // Set fault data
        this.fault_stage = stage;
        this.fault_type = code;
        this.fault_pc = pc;
        this.fault_bd = bd;

        // Set COP0 info
        this.c0regs[C0_STATUS] |= 2; // EXL
        this.c0regs[C0_CAUSE] = (long)(int)((this.c0regs[C0_CAUSE] & 0x0000FF00)
                | ((code.code&0x1F)<<2)
                | (bd ? 0x80000000 : 0));
        this.c0regs[C0_EPC] = (bd ? pc - 4 : pc);

        // Update important flags
        updateC0Cause(this.c0regs[C0_CAUSE]);
        updateC0Status(this.c0regs[C0_STATUS]);

        // Spew fault into log
        System.err.printf("MIPS FAULT:\n");
        System.err.printf("- Cause: %s (%d)\n", code.name, code.code);
        System.err.printf("- Pipeline stage: %s\n", stage.name);
        System.err.printf("- PC: %016X (BD = %s)\n", pc, bd?"YES":"no");
        System.err.printf("- c0_status:   %016X\n", this.c0regs[C0_STATUS]);
        System.err.printf("- c0_cause:    %016X\n", this.c0regs[C0_CAUSE]);
        System.err.printf("- c0_badvaddr: %016X\n", this.c0regs[C0_BADVADDR]);
    }

    // "Immediate" reads and writes
    // These do not trigger MIPS faults.
    // Java exceptions perhaps, but no MIPS faults.
    //
    // They also completely bypass the cache,
    // and use physical addresses.

    private int read8Imm(final long addr) {
        // XXX: pending wider bus support
        this.cycleBudget -= 1;
        return 0xFF&(int)memory.read((int)addr);
    }

    private int read16Imm(final long addr) {
        int v0 = read8Imm(addr+0);
        int v1 = read8Imm(addr+1);
        return (v0 & 0xFF) | (v1<<8);
    }

    private int read32Imm(final long addr) {
        int v0 = read16Imm(addr+0);
        int v1 = read16Imm(addr+2);
        return (v0 & 0xFFFF) | (v1<<16);
    }

    private long read64Imm(final long addr) {
        long v0 = read32Imm(addr+0);
        long v1 = read32Imm(addr+4);
        return (v0 & 0xFFFFFFFFL) | (v1<<32L);
    }

    private void write8Imm(final long addr, final int data) {
        // XXX: pending wider data bus support
        memory.write((int)addr, 0xFF&(int)data);
    }

    private void write16Imm(final long addr, final int data) {
        write8Imm(addr+0, data);
        write8Imm(addr+1, data>>8);
    }

    private void write32Imm(final long addr, final int data) {
        write16Imm(addr+0, data);
        write16Imm(addr+2, data>>16);
    }

    private void write64Imm(final long addr, final long data) {
        write32Imm(addr+0, (int)(data));
        write32Imm(addr+4, (int)(data>>32L));
    }

    // Address remapping

    private int cachePolicy(long vaddr, long paddr) {
        // TODO: TLB
        if(vaddr >= -0x80000000L && vaddr < -0x60000000L) {
            // ckseg0
            // K0 field([0,2]) in c0_config(16) determines this
            return CACHE_WBACK;
        } else if(vaddr >= -0x60000000L && vaddr < -0x40000000L) {
            // ckseg1
            return CACHE_NONE;
        } else if(vaddr >= -0x8000_0000_0000_0000L && vaddr < -0x4000_0000_0000_0000L) {
            // xkphys
            return 3&(int)(vaddr>>>59L);
        } else {
            return CACHE_WBACK;
        }
    }

    private long virtToPhys64(long vaddr) throws MipsAddressErrorException {
        if(!address64) {
            vaddr = (long)(int)vaddr;
        }

        if(vaddr >= 0) { // 0x0000_0000_0000_0000 to 0x0000_0000_7FFF_FFFF
            // temp workaround: any jumps to the TLB space should fault
            // this makes it easier to squash bugs while we don't have a TLB
            if(false && (vaddr&0x3FFF_FFFF_FFFF_FFFFL) < 0x0000_0100_0000_0000L) {
                // TODO: TLB

                if(vaddr >= 0x4000_0000_0000_0000L) {
                    // xksseg
                    if (!superMode) {
                        // PERMISSION DENIED
                        this.c0regs[C0_BADVADDR] = vaddr;
                        throw new MipsAddressErrorException();
                    }
                }
                // otherwise xkuseg/kuseg

                return (vaddr & 0x0000_0000_7FFF_FFFFL);
            } else {
                // INVALID!
                this.c0regs[C0_BADVADDR] = vaddr;
                throw new MipsAddressErrorException();

            }

        } else if(vaddr < -0x40000000) { // 0xFFFF_FFFF_C000_0000 to 0xFFFF_FFFF_FFFF_FFFF
            if(!kernelMode) {
                // PERMISSION DENIED
                this.c0regs[C0_BADVADDR] = vaddr;
                throw new MipsAddressErrorException();
            }

            if(vaddr >= -0x60000000) { // 0xFFFF_FFFF_A000_0000 to 0xFFFF_FFFF_BFFF_FFFF
                // ckseg1/kseg1
                return (vaddr & 0x0000_0000_1FFF_FFFFL);

            } else if(vaddr >= -0x80000000) { // 0xFFFF_FFFF_8000_0000 to 0xFFFF_FFFF_9FFF_FFFF
                // ckseg0/kseg0
                // TODO: cache
                return (vaddr & 0x0000_0000_1FFF_FFFFL);

            } else if(vaddr >= -0x8000_0000_0000_0000L && vaddr < -0x6000_0000_0000_0000L) {
                // xkphys
                // TODO: cache
                if((vaddr & 0x07FF_FFF0_0000_0000L) == 0) {
                    // valid (we support 36 physbits, partly to make Sangar's job harder --GM)
                    return (vaddr & 0x0000_000F_FFFF_FFFFL);
                } else {
                    // INVALID!
                    this.c0regs[C0_BADVADDR] = vaddr;
                    throw new MipsAddressErrorException();
                }

            } else if(vaddr >= -0x4000_0000_0000_0000L && vaddr < -0x3FFF_FFFF_8000_0000L) {
                // xkseg
                return (vaddr & 0x0000_000F_FFFF_FFFFL);

            } else {
                // INVALID!
                this.c0regs[C0_BADVADDR] = vaddr;
                throw new MipsAddressErrorException();
            }

        } else if(vaddr < -0x20000000) { // 0xFFFF_FFFF_C000_0000 to 0xFFFF_FFFF_DFFF_FFFF
            // ckseg2/kseg2
            if (!superMode) {
                // PERMISSION DENIED
                this.c0regs[C0_BADVADDR] = vaddr;
                throw new MipsAddressErrorException();
            }

            // TODO: TLB
            return (vaddr & 0x0000_0000_1FFF_FFFFL);

        } else { // 0xE0000000 to 0xFFFFFFFF
            // ckseg3/kseg3
            if (!kernelMode) {
                // PERMISSION DENIED
                this.c0regs[C0_BADVADDR] = vaddr;
                throw new MipsAddressErrorException();
            }

            // TODO: TLB
            return (vaddr & 0x0000_0000_1FFF_FFFFL);
        }
    }

    // Situational reads and writes

    private int readInstr(long vaddr) throws MipsAddressErrorException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&3)!= 0) {
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+4) >= AbstractBusController.ADDRESS_COUNT) {
            throw new MipsBusErrorException();
        }

        return read32Imm(paddr);
    }

    private int readData8(long vaddr) throws MipsAddressErrorException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+1) >= AbstractBusController.ADDRESS_COUNT) {
            throw new MipsBusErrorException();
        }

        return read8Imm(paddr);
    }

    private int readData16(long vaddr) throws MipsAddressErrorException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&1)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+2) >= AbstractBusController.ADDRESS_COUNT) {
            throw new MipsBusErrorException();
        }

        return read16Imm(paddr);
    }

    private int readData32(long vaddr) throws MipsAddressErrorException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&3)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+4) >= AbstractBusController.ADDRESS_COUNT) {
            throw new MipsBusErrorException();
        }

        return read32Imm(paddr);
    }

    private long readData64(long vaddr) throws MipsAddressErrorException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&7)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+8) >= AbstractBusController.ADDRESS_COUNT) {
            throw new MipsBusErrorException();
        }

        return read64Imm(paddr);
    }

    private void writeData8(long vaddr, int data) throws MipsAddressErrorException {
        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+1) >= AbstractBusController.ADDRESS_COUNT) {
            return;
        }

        write8Imm(paddr, data);
    }

    private void writeData16(long vaddr, int data) throws MipsAddressErrorException {
        // Ensure proper alignment
        if((vaddr&1)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+2) >= AbstractBusController.ADDRESS_COUNT) {
            return;
        }

        write16Imm(paddr, data);
    }

    private void writeData32(long vaddr, int data) throws MipsAddressErrorException {
        // Ensure proper alignment
        if((vaddr&3)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+4) >= AbstractBusController.ADDRESS_COUNT) {
            return;
        }

        write32Imm(paddr, data);
    }

    private void writeData64(long vaddr, long data) throws MipsAddressErrorException {
        // Ensure proper alignment
        if((vaddr&7)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);
        int cpol = cachePolicy(vaddr, paddr);

        // TODO: get the limit properly
        if((paddr+8) >= AbstractBusController.ADDRESS_COUNT) {
            return;
        }

        write64Imm(paddr, data);
    }

    // COP0 updates

    private void updateC0RegMasked(int idx, long v, long wmask)
    {
        v &= wmask;
        this.c0regs[idx] &= ~wmask;
        this.c0regs[idx] |= v;
    }

    private void updateC0Status(long v)
    {
        // bit 21 = TLB shutdown
        //
        // This does NOT trigger on an R4600
        // but DOES on a VR4300
        //
        // Considering I (GM) never bothered to check on OCMIPS,
        // it might as well just stay zero
        //
        // ...not to mention that you don't ever write to it here anyway

        updateC0RegMasked(C0_STATUS, v, (long)(int)0xF657FFFF);

        int sr = (int)this.c0regs[C0_STATUS];

        // If modeBits is 3, behaviour is undefined
        // Here we're just treating it as user mode to make it easier
        int modeBits = (sr>>3)&3;
        if(modeBits > 2) {
            // Our calculations depend on this being in [0,2]
            modeBits = 2;
        }

        boolean erl = ((sr>>2)&1) != 0;
        boolean exl = ((sr>>1)&1) != 0;

        kernelMode = (modeBits <= 0) || exl || erl;
        superMode = (modeBits <= 1) || kernelMode;
        address64 = ((sr>>(7-modeBits))&1) != 0;
        allow64 = address64 || kernelMode;

        System.err.printf("SR update: k=%c | s=%c | d=%c | a=%c\n"
                , (kernelMode ? 'Y' : 'n')
                , (superMode ? 'Y' : 'n')
                , (allow64 ? 'Y' : 'n')
                , (address64 ? 'Y' : 'n')
                );
    }

    private void updateC0Cause(long v)
    {
        updateC0RegMasked(C0_CAUSE, v, (long)(int)0x00000300);
    }

    private void updateC0Config(long v)
    {
        updateC0RegMasked(C0_CONFIG, v, (long)(int)0x00000007);
    }

    // Main run-op loop

    private void runOp() {
        this.cycleBudget -= 1;

        // Fetch op
        long ic_pc = this.pc;
        int ic_op = 0;
        this.pc += 4;
        try {
            ic_op = readInstr(ic_pc);
        } catch (MipsAddressErrorException e) {
            fault(MFault.AdEL, MPipelineStage.IC, ic_pc, false);
            ic_op = 0;
        } catch (MipsBusErrorException e) {
            fault(MFault.IBE, MPipelineStage.IC, ic_pc, false);
            ic_op = 0;
        }

        // Move pl0 data
        long ex_pc = this.pl0_pc;
        int ex_op = this.pl0_op;
        boolean ex_bd = this.pl0_bd;
        this.pl0_pc = ic_pc;
        this.pl0_op = ic_op;
        this.pl0_bd = false;

        // Execute op
        int rs = (ex_op>>>21)&31;
        int rt = (ex_op>>>16)&31;
        int rd = (ex_op>>>11)&31;

        // immu == ex_op&0xFFFF
        // imms == (int)(short)ex_op
        // shamt == (ex_op>>>6)&31
        // func == ex_op&63

        long wb_result = this.regs[rd];

        int opcode_type = (ex_op>>>26)&63;
        switch(opcode_type) {

            // Special

            case 0: {
                int opcode_func = ex_op&63;
                switch (opcode_func) {

                    // Shifts

                    case 0: // SLL
                        wb_result = this.regs[rt] << ((ex_op>>>6)&31);
                        wb_result = (long)(int)wb_result;
                        break;
                    case 2: // SRL
                        wb_result = (long)(((int)this.regs[rt]) >>> ((ex_op>>>6)&31));
                        wb_result = (long)(int)wb_result;
                        break;
                    case 3: // SRA
                        wb_result = (long)(((int)this.regs[rt]) >> ((ex_op>>>6)&31));
                        wb_result = (long)(int)wb_result;
                        break;

                    case 4: // SLLV
                        wb_result = this.regs[rt] << (this.regs[rs]&31);
                        wb_result = (long)(int)wb_result;
                        break;
                    case 6: // SRLV
                        wb_result = (long)(((int)this.regs[rt]) >>> (this.regs[rs]&31));
                        wb_result = (long)(int)wb_result;
                        break;
                    case 7: // SRAV
                        wb_result = (long)(((int)this.regs[rt]) >> (this.regs[rs]&31));
                        wb_result = (long)(int)wb_result;
                        break;

                    // Register branches

                    case 9: // JALR
                        wb_result = ex_pc+8;
                        // *** FALL THROUGH
                    case 8: // JR
                        this.pc = this.regs[rs];
                        this.pl0_bd = true;
                        break;

                    // Software exceptions

                    case 12: // SYSCALL
                        fault(MFault.Syscall, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    case 13: // BREAK
                        fault(MFault.Bp, MPipelineStage.EX, ex_pc, ex_bd);
                        break;

                    // Sync

                    case 15: // SYNC
                        // we can't really do anything with this right now
                        // but we can't RI it either
                        // let's do nothing for now.
                        break;

                    // LO/HI moves

                    case 16: // MFHI
                        wb_result = this.mdhi;
                        break;
                    case 17: // MTHI
                        this.mdhi = this.regs[rs];
                        break;
                    case 18: // MFLO
                        wb_result = this.mdlo;
                        break;
                    case 19: // MTLO
                        this.mdlo = this.regs[rs];
                        break;

                    // 64-bit register shifts

                    case 20: // DSLLV
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] << (long)(this.regs[rs]&31);
                        break;
                    case 22: // DSRLV
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] >>> (long)(this.regs[rs]&31);
                        break;
                    case 23: // DSRAV
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] >> (long)(this.regs[rs]&31);
                        break;

                    // Multivide

                    case 24: { // MULT
                        long res = ((long)(int)this.regs[rs]) * (long)(int)this.regs[rt];
                        this.mdlo = (long)(int)res;
                        this.mdhi = (long)(int)(res>>32);
                    } break;
                    case 25: { // MULTU
                        long res = (0xFFFFFFFFL&(long)(int)this.regs[rs]) * (0xFFFFFFFFL&(long)(int)this.regs[rt]);
                        this.mdlo = (long)(int)res;
                        this.mdhi = (long)(int)(res>>32);
                    } break;
                    case 26: { // DIV
                        if(((int)this.regs[rt]) == 0) {
                            this.mdhi = (long)(int)this.regs[rs];
                            this.mdlo = (this.mdhi < 0 ? 1 : -1);
                        } else {
                            this.mdlo = ((long)(int)this.regs[rs]) / (long)(int)this.regs[rt];
                            this.mdhi = ((long)(int)this.regs[rs]) % (long)(int)this.regs[rt];
                        }
                        this.mdlo = (long)(int)this.mdlo;
                        this.mdhi = (long)(int)this.mdhi;
                    } break;
                    case 27: { // DIVU
                        if(((int)this.regs[rt]) == 0) {
                            this.mdlo = -1;
                            this.mdhi = (long)(int)this.regs[rs];
                        } else {
                            this.mdlo = (0xFFFFFFFFL&(long)(int)this.regs[rs]) / (0xFFFFFFFFL&(long)(int)this.regs[rt]);
                            this.mdhi = (0xFFFFFFFFL&(long)(int)this.regs[rs]) % (0xFFFFFFFFL&(long)(int)this.regs[rt]);
                        }
                        this.mdlo = (long)(int)this.mdlo;
                        this.mdhi = (long)(int)this.mdhi;
                        /*
                        System.out.printf("%d/%d -> %d rem %d\n"
                                , (int)this.regs[rs]
                                , (int)this.regs[rt]
                                , (int)this.mdlo
                                , (int)this.mdhi
                        );
                        */
                    } break;

                    // Multivide 64-bit

                    case 28: { // DMULT
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        long res = ((long)(int)this.regs[rs]) * (long)(int)this.regs[rt];
                        this.mdlo = (long)(int)res;
                        this.mdhi = (long)(int)(res>>32);
                    } break;
                    case 29: { // DMULTU
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        long res = (0xFFFFFFFFL&(long)(int)this.regs[rs]) * (0xFFFFFFFFL&(long)(int)this.regs[rt]);
                        this.mdlo = (long)(int)res;
                        this.mdhi = (long)(int)(res>>32);
                    } break;
                    case 30: { // DDIV
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        if(this.regs[rt] == 0) {
                            this.mdhi = this.regs[rs];
                            this.mdlo = (this.mdhi < 0 ? 1 : -1);
                        } else {
                            this.mdlo = (this.regs[rs]) / this.regs[rt];
                            this.mdhi = (this.regs[rs]) % this.regs[rt];
                        }
                    } break;
                    case 31: { // DDIVU
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        if(this.regs[rt] == 0) {
                            this.mdlo = -1;
                            this.mdhi = this.regs[rs];
                        } else if(this.regs[rt] < 0) { // *rt >= (1<<63)
                            if(this.regs[rs] < 0 && this.regs[rs] >= this.regs[rt]) { // rt >= (1<<63)
                                this.mdlo = 1;
                                this.mdhi = this.regs[rs] - this.regs[rt];
                            } else {
                                this.mdlo = 0;
                                this.mdhi = this.regs[rs];
                            }
                        } else if(this.regs[rt] == 1) {
                            this.mdlo = this.regs[rs];
                            this.mdhi = 0;
                        } else if(this.regs[rt] == 2) {
                            this.mdlo = this.regs[rs]>>1L;
                            this.mdhi = this.regs[rs]&1L;
                        } else {
                            // we should hopefully be OK by now
                            // we are dividing by numbers no smaller than 3
                            long sh = 0xFFFFFFFFL&(long)(int)(this.regs[rs]>>32L);
                            long sl = 0xFFFFFFFFL&(long)(int)(this.regs[rs]);
                            long tx = this.regs[rt];

                            long qh = sh/tx;
                            long rh = sh%tx;
                            sl += rh<<32L;
                            long ql = sl/tx;
                            long rl = sl%tx;

                            this.mdlo = (qh<<32L)+ql;
                            this.mdhi = rl;
                        }
                    } break;

                    // Register arithmetricks

                    case 32: // ADD
                        wb_result = (long)(((int)this.regs[rs]) + (int)this.regs[rt]);
                        wb_result = (long)(int)wb_result;
                        if(this.regs[rt] >= 0
                                ? wb_result < (int)this.regs[rs]
                                : wb_result > (int)this.regs[rs]
                                ) {
                            rd = 0;
                            fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        break;
                    case 33: // ADDU
                        wb_result = (long)(((int)this.regs[rs]) + (int)this.regs[rt]);
                        wb_result = (long)(int)wb_result;
                        break;
                    case 34: // SUB
                        wb_result = (long)(((int)this.regs[rs]) - (int)this.regs[rt]);
                        wb_result = (long)(int)wb_result;
                        if(this.regs[rt] < 0
                                ? wb_result < (int)this.regs[rs]
                                : wb_result > (int)this.regs[rs]
                                ) {
                            rd = 0;
                            fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        break;
                    case 35: // SUBU
                        wb_result = (long)(((int)this.regs[rs]) - (int)this.regs[rt]);
                        wb_result = (long)(int)wb_result;
                        break;
                    case 36: // AND
                        wb_result = this.regs[rs] & this.regs[rt];
                        break;
                    case 37: // OR
                        wb_result = this.regs[rs] | this.regs[rt];
                        break;
                    case 38: // XOR
                        wb_result = this.regs[rs] ^ this.regs[rt];
                        break;
                    case 39: // NOR
                        wb_result = ~(this.regs[rs] | this.regs[rt]);
                        break;

                    case 42: // SLT
                        wb_result = (this.regs[rs]) < (this.regs[rt]) ? 1 : 0;
                        break;
                    case 43: // SLTU
                        wb_result = ((this.regs[rs])^0x8000000000000000L) <
                                ((this.regs[rt])^0x8000000000000000L) ? 1 : 0;
                        break;

                    // Register 64-bit arithmetricks

                    case 44: // DADD
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = this.regs[rs] + this.regs[rt];
                        if(this.regs[rt] >= 0
                                ? wb_result < this.regs[rs]
                                : wb_result > this.regs[rs]
                                ) {
                            rd = 0;
                            fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        break;
                    case 45: // DADDU
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = this.regs[rs] + this.regs[rt];
                        break;
                    case 46: // DSUB
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = this.regs[rs] - this.regs[rt];
                        if(this.regs[rt] < 0
                                ? wb_result < this.regs[rs]
                                : wb_result > this.regs[rs]
                                ) {
                            rd = 0;
                            fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        break;
                    case 47: // DSUBU
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = this.regs[rs] - this.regs[rt];
                        break;

                    // Conditional traps

                    case 48: // TGE
                        if(this.regs[rs] >= this.regs[rt]) {
                            this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                        }
                        break;
                    case 49: // TGEU
                        if((this.regs[rs]^0x8000000000000000L) >= (this.regs[rt]^0x8000000000000000L)) {
                            this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                        }
                        break;
                    case 50: // TLT
                        if(this.regs[rs] < this.regs[rt]) {
                            this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                        }
                        break;
                    case 51: // TLTU
                        if((this.regs[rs]^0x8000000000000000L) < (this.regs[rt]^0x8000000000000000L)) {
                            this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                        }
                        break;
                    case 52: // TEQ
                        if(this.regs[rs] == this.regs[rt]) {
                            this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                        }
                        break;
                    case 54: // TNE
                        if(this.regs[rs] != this.regs[rt]) {
                            this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                        }
                        break;

                    // 64-bit immediate shifts

                    case 56: // DSLL
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] << (long)((ex_op>>>6)&31);
                        break;
                    case 58: // DSRL
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] >>> (long)((ex_op>>>6)&31);
                        break;
                    case 59: // DSRA
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] >> (long)((ex_op>>>6)&31);
                        break;

                    case 60: // DSLL32
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] << (long)(32+((ex_op>>>6)&31));
                        break;
                    case 62: // DSRL32
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] >>> (long)(32+((ex_op>>>6)&31));
                        break;
                    case 63: // DSRA32
                        if(!allow64) {
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                        }
                        wb_result = (long)this.regs[rt] >> (long)(32+((ex_op>>>6)&31));
                        break;

                    // Reserved instruction fault

                    default:
                        if(ex_op == 0) {
                            break;
                        }

                        System.err.printf("RI Special: %d\n", opcode_func);
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                }
            } break;

            // REGIMM

            case 1: switch(rt) {

                // Branches

                case 0: // BLTZ
                    if(this.regs[rs] < 0) {
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    }
                    break;
                case 1: // BGEZ
                    if(this.regs[rs] >= 0) {
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    }
                    break;
                case 2: // BLTZL
                    if(this.regs[rs] < 0) {
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    } else {
                        this.pl0_op = 0;
                    }
                    break;
                case 3: // BGEZL
                    if(this.regs[rs] >= 0) {
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    } else {
                        this.pl0_op = 0;
                    }
                    break;

                // Immediate conditional traps

                case 8: // TGEI
                    if(this.regs[rs] >= (long)(short)ex_op) {
                        this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                    }
                    break;
                case 9: // TGEIU
                    if((this.regs[rs]^0x8000000000000000L) >= (0x8000000000000000L^(long)(short)ex_op)) {
                        this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                    }
                    break;
                case 10: // TLTI
                    if(this.regs[rs] < (long)(short)ex_op) {
                        this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                    }
                    break;
                case 11: // TLTIU
                    if((this.regs[rs]^0x8000000000000000L) < (0x8000000000000000L^(long)(short)ex_op)) {
                        this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                    }
                    break;
                case 12: // TEQ
                    if(this.regs[rs] == (long)(short)ex_op) {
                        this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                    }
                    break;
                case 14: // TNE
                    if(this.regs[rs] != (long)(short)ex_op) {
                        this.fault(MFault.Tr, MPipelineStage.EX, ex_pc, ex_bd);
                    }
                    break;

                // Branch and link

                case 16: // BLTZAL
                    if(this.regs[rs] < 0) {
                        rd = 31;
                        wb_result = ex_pc+8;
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    }
                    break;
                case 17: // BGEZAL
                    if(this.regs[rs] >= 0) {
                        rd = 31;
                        wb_result = ex_pc+8;
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    }
                    break;
                case 18: // BLTZALL
                    if(this.regs[rs] < 0) {
                        rd = 31;
                        wb_result = ex_pc+8;
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    } else {
                        this.pl0_op = 0;
                    }
                    break;
                case 19: // BGEZALL
                    if(this.regs[rs] >= 0) {
                        rd = 31;
                        wb_result = ex_pc+8;
                        this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                        this.pl0_bd = true;
                    } else {
                        this.pl0_op = 0;
                    }
                    break;

                // Reserved instruction fault

                default:
                    if(ex_op == 0) {
                        break;
                    }

                    System.err.printf("RI RegImm: %d\n", rt);
                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
            } break;

            // Jumps

            case 3: // JAL
                rd = 31;
                wb_result = ex_pc+8;
                // *** FALL THROUGH
            case 2: // J
                this.pc = (ex_pc & ~0x0FFFFFFFL) | (long)((ex_op<<2)&0x0FFFFFFF);
                this.pl0_bd = true;
                break;

            // Branches

            case 4: // BEQ
                if(this.regs[rs] == this.regs[rt]) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                }
                break;
            case 5: // BNE
                if(this.regs[rs] != this.regs[rt]) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                }
                break;
            case 6: // BLEZ
                if(this.regs[rs] <= 0) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                }
                break;
            case 7: // BGTZ
                if(this.regs[rs] > 0) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                }
                break;

            // Immediate arithmetricks

            case 8: // ADDI
                rd = rt;
                wb_result = (long)(((int)this.regs[rs]) + (int)(short)(ex_op));
                wb_result = (long)(int)wb_result;
                if((short)ex_op >= 0
                        ? wb_result < (int)this.regs[rs]
                        : wb_result > (int)this.regs[rs]
                            ) {
                    rd = 0;
                    fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
                }
                break;

            case 9: // ADDIU
                rd = rt;
                wb_result = (long)(((int)this.regs[rs]) + (int)(short)(ex_op));
                wb_result = (long)(int)wb_result;
                break;

            case 10: // SLTI
                rd = rt;
                wb_result = (this.regs[rs]) < (long)(short)(ex_op) ? 1 : 0;
                break;
            case 11: // SLTIU
                // If I had a dollar for every time I wished this were C,
                // ...at the time of writing I think I'm up to $2
                rd = rt;
                wb_result = ((this.regs[rs])^0x8000000000000000L) <
                        (((long)(short)(ex_op))^0x8000000000000000L) ? 1 : 0;
                break;

            case 12: // ANDI
                rd = rt;
                wb_result = this.regs[rs] & (long)(0xFFFF&(int)(ex_op));
                break;
            case 13: // ORI
                rd = rt;
                wb_result = this.regs[rs] | (long)(0xFFFF&(int)(ex_op));
                break;
            case 14: // XORI
                rd = rt;
                wb_result = this.regs[rs] ^ (long)(0xFFFF&(int)(ex_op));
                break;

            case 15: // LUI
                rd = rt;
                wb_result = (long)(int)(ex_op<<16);
                break;

            // COP0

            case 16: if((this.c0regs[C0_STATUS]&(1<<28)) == 0 && !kernelMode) {
                fault(MFault.CpU, MPipelineStage.EX, ex_pc, ex_bd);
                this.c0regs[C0_CAUSE] |= 0<<28;
            } else if(rs < 16) {
                switch(rs) {
                    // MFC0
                    case 0: switch(rd) {
                        case C0_STATUS:
                        case C0_CAUSE:
                        case C0_EPC:
                        case C0_CONFIG:
                            wb_result = (long)(int)this.c0regs[rd];
                            rd = rt;
                            break;

                        default:
                            System.err.printf("RI MFC0: %d\n", rd);
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                    } break;

                    // DMFC0
                    case 1: if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        switch(rd) {
                            case C0_STATUS:
                            case C0_CAUSE:
                            case C0_EPC:
                            case C0_CONFIG:
                                wb_result = this.c0regs[rd];
                                rd = rt;
                                break;

                            default:
                                System.err.printf("RI DMFC0: %d\n", rd);
                                fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                                break;
                        }
                    } break;

                    // MTC0
                    case 4: switch(rd) {
                        case C0_STATUS:
                            updateC0Status((long)(int)this.regs[rt]);
                            break;
                        case C0_CAUSE:
                            updateC0Cause((long)(int)this.regs[rt]);
                            break;
                        case C0_CONFIG:
                            updateC0Config((long)(int)this.regs[rt]);
                            break;

                        case C0_EPC:
                        case C0_ERROREPC:
                            this.c0regs[rd] = (long)(int)this.regs[rt];
                            break;

                        default:
                            System.err.printf("RI MTC0: %d\n", rd);
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                    } break;

                    // DMTC0
                    case 5: if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        switch(rd) {
                            case C0_STATUS:
                                updateC0Status(this.regs[rt]);
                                break;
                            case C0_CAUSE:
                                updateC0Cause(this.regs[rt]);
                                break;
                            case C0_CONFIG:
                                updateC0Config(this.regs[rt]);
                                break;

                            case C0_EPC:
                            case C0_ERROREPC:
                                this.c0regs[rd] = this.regs[rt];
                                break;

                            default:
                                System.err.printf("RI DMTC0: %d\n", rd);
                                fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                                break;
                        }
                    } break;
                }
            } else {
                int opcode_func = ex_op&63;
                switch(opcode_func) {

                    default:
                        System.err.printf("RI COP0: %d\n", opcode_func);
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                }
            } break;

            // COP1

            case 17:
                // TODO!
                fault(MFault.CpU, MPipelineStage.EX, ex_pc, ex_bd);
                this.c0regs[C0_CAUSE] |= 1<<28;
                break;

            // COP2

            case 18:
                fault(MFault.CpU, MPipelineStage.EX, ex_pc, ex_bd);
                this.c0regs[C0_CAUSE] |= 2<<28;
                break;

            // COP3 does not exist in MIPS3

            // Branches - Likely

            case 20: // BEQL
                if(this.regs[rs] == this.regs[rt]) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                } else {
                    this.pl0_op = 0;
                }
                break;
            case 21: // BNEL
                if(this.regs[rs] != this.regs[rt]) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                } else {
                    this.pl0_op = 0;
                }
                break;
            case 22: // BLEZL
                if(this.regs[rs] <= 0) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                } else {
                    this.pl0_op = 0;
                }
                break;
            case 23: // BGTZL
                if(this.regs[rs] > 0) {
                    this.pc = ex_pc + 4 + (((long)(short)ex_op)<<2);
                    this.pl0_bd = true;
                } else {
                    this.pl0_op = 0;
                }
                break;

            // Immediate 64-bit arithmetricks

            case 24: // DADDI
                if(!allow64) {
                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
                }
                rd = rt;
                wb_result = this.regs[rs] + (long)(short)(ex_op);
                if((short)ex_op >= 0
                        ? wb_result < (long)this.regs[rs]
                        : wb_result > (long)this.regs[rs]
                        ) {
                    rd = 0;
                    fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
                }
                break;

            case 25: // DADDIU
                if(!allow64) {
                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
                }
                rd = rt;
                wb_result = this.regs[rs] + (long)(short)ex_op;
                break;

            // Load primary

            case 32: // LB
                try {
                    wb_result = (long)readData8(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 33: // LH
                try {
                    wb_result = (long)readData16(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 35: // LW
                try {
                    wb_result = (long)readData32(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 36: // LBU
                try {
                    wb_result = 0xFFL&(long)readData8(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 37: // LHU
                try {
                    wb_result = 0xFFFFL&(long)readData16(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 39: // LWU
                try {
                    wb_result = 0xFFFFFFFFL&(long)readData32(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            // Load unaligned - TODO
            // case 34: // LWL break;
            // case 38: // LWR break;
            // case 26: // LDL break;
            // case 27: // LDR break;

            // Store primary

            case 40: // SB
                try {
                    writeData8(this.regs[rs] + (long)(short)ex_op, (int)this.regs[rt]);
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 41: // SH
                try {
                    writeData16(this.regs[rs] + (long)(short)ex_op, (int)this.regs[rt]);
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            case 43: // SW
                try {
                    writeData32(this.regs[rs] + (long)(short)ex_op, (int)this.regs[rt]);
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            // Store unaligned - TODO
            // case 42: // SWL break;
            // case 46: // SWR break;
            // case 44: // SDL break;
            // case 45: // SDR break;

            // Load fancy

            case 55: // LD
                if(!allow64) {
                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
                }
                try {
                    wb_result = readData64(this.regs[rs] + (long)(short)ex_op);
                    rd = rt;
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                } catch (MipsBusErrorException e) {
                    fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            // Store fancy

            case 63: // SD
                if(!allow64) {
                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
                }
                try {
                    writeData64(this.regs[rs] + (long)(short)ex_op, this.regs[rt]);
                } catch (MipsAddressErrorException e) {
                    fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                }
                break;

            // Reserved instruction fault

            default:
                System.err.printf("RI: %d\n", opcode_type);
                fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                break;
        }

        // Write back result
        if(rd != 0) {
            this.regs[rd] = wb_result;
        }

        // Set BD flag if IC stage faulted
        if(this.fault_stage == MPipelineStage.IC) {
            this.fault_bd = this.pl0_bd;
        }
    }

    // API requirements

    public void reset() {
        synchronized(lock) {
            this.pc = VECTOR_BASE_RESET;

            this.pl0_op = 0;
            this.pl0_pc = this.pc;
            this.pl0_bd = false;
            this.fault_type = MFault.NONE;
            this.fault_stage = MPipelineStage.NONE;
            this.fault_pc = 0;
            this.fault_bd = false;

            // Extra stuff that doesn't normally get covered
            for(int i = 0; i < 32; i++) {
                this.regs[i] = 0L;
            }
            for(int i = 0; i < 32; i++) {
                this.c0regs[i] = 0L;
            }

            this.c0regs[C0_STATUS] = (long)(int)0x040000E0;
            this.c0regs[C0_CAUSE] = (long)(int)0x00000000;
            this.c0regs[C0_CONFIG] = (long)(int)0x1002649B;

            // However, this DOES happen during a reset!
            this.c0regs[C0_STATUS] |= (long)(int)0x00400004;
            this.c0regs[C0_RANDOM] = TLB_COUNT-1;

            // Update important flags
            updateC0Cause(this.c0regs[C0_CAUSE]);
            updateC0Status(this.c0regs[C0_STATUS]);
        }
    }

    public void irq(final int interrupt) {
        synchronized(lock) {
            // TODO!
        }
    }

    public void run(final int cycles) {
        synchronized(lock) {
            this.cycleBudget += cycles;

            while(this.cycleBudget > 0) {
                // TODO!
                if(this.fault_type == MFault.NONE) {
                    //System.out.printf("PC = %016X / v0 = %016X\n", this.pc, this.regs[2]);
                    runOp();
                } else {
                    this.cycleBudget -= 10000;
                }
            }
        }
    }
}
