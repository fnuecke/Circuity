package li.cil.circuity.server.processor.mips;

import li.cil.circuity.common.bus.AbstractBusController;
import li.cil.circuity.server.processor.BusControllerAccess;
import li.cil.lib.api.serialization.Serialize;

/** MIPS3 implementation based mostly on the IDT R4600.
 *
 * What's missing:
 * - probably still quite a few things
 * - big-endian mode (little-endian is used exclusively)
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
    @Serialize
    private long pcNext = VECTOR_BASE_RESET+4;
    @Serialize
    private long pcAfter = VECTOR_BASE_RESET+8;

    // Op fetched into pipeline
    @Serialize
    private boolean pcBranchDelay = false;

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
        DBE(7, "Bus error on dfetch"), // Unlike MIPS1, this CAN happen on writes.
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

    // Cache policy
    public static final int CACHE_WTHRU_NOWALLOC = 0x0;
    public static final int CACHE_WTHRU_WALLOC = 0x1;
    public static final int CACHE_NONE = 0x2;
    public static final int CACHE_WBACK = 0x3;

    // Exceptions
    public class MipsAddressErrorException extends Exception {}
    public class MipsBusErrorException extends Exception {}
    public class MipsTlbModException extends Exception {}
    public class MipsTlbMissException extends Exception {}
    public class MipsOpFaulted extends Exception {}

    // TLB entries as defined by MIPS
    @Serialize
    private long[] tlbEntryLo0 = new long[TLB_COUNT];
    @Serialize
    private long[] tlbEntryLo1 = new long[TLB_COUNT];
    @Serialize
    private long[] tlbEntryHi = new long[TLB_COUNT];
    @Serialize
    private long[] tlbPageMask = new long[TLB_COUNT];

    // TLB entry aux data
    @Serialize
    private long[] tlbBase = new long[TLB_COUNT];
    @Serialize
    private int[] tlbNext = new int[TLB_COUNT];
    @Serialize
    private int[] tlbPrev = new int[TLB_COUNT];
    @Serialize
    private int tlbFirst = 0;
    @Serialize
    private long tlbRecentLo = 0;

    // Caches
    // cache size: I=16KB D=16KB
    // line  size: I=32B  D=32B
    public static final int ICACHE_SIZE_WORDS = 16*256;
    public static final int DCACHE_SIZE_DWORDS = 16*256;
    public static final int ICACHE_SIZE_TAGS = 16*32;
    public static final int DCACHE_SIZE_TAGS = 16*32;
    @Serialize
    private int[] iCacheData = new int[ICACHE_SIZE_WORDS];
    @Serialize
    private long[] dCacheData = new long[DCACHE_SIZE_DWORDS];

    // Cache tags

    // Currently ignoring parity

    // Similarities:
    // 23:0 = PTag (paddr 35:12)
    // 24 = valid
    // 28 = writeback (0 on IC)

    // ICache:
    // 23:0 = PTag (paddr 35:12)
    // 24,25,26,27 = valid,0,0,0
    // x:28 = 0
    @Serialize
    private int[] iCacheTags = new int[ICACHE_SIZE_TAGS];

    // DCache:
    // 23:0 = PTag (paddr 35:12)
    // 25:24 = Cache state (0=inval, 3=dirty-ex - COHERENCY PROTOCOLS *NOT* SUPPORTED!)
    // 26,27,28,29 = 0,W,W,0 where W = writeback bit (read bit 28 please!)
    // x:30 = 0

    @Serialize
    private int[] dCacheTags = new int[DCACHE_SIZE_TAGS];

    // Cycles for timing
    @Serialize
    private int cycleBudget = 0;

    // Optimisation stuff
    @Serialize
    private boolean allow64 = true;
    @Serialize
    private boolean address64 = true;
    @Serialize
    private boolean kernelMode = true;
    @Serialize
    private boolean superMode = true;
    @Serialize
    private long k0base = 0x1C;

    // Benchmarking
    private long benchAccum = 0;
    private int benchTicksLeft = 20;

    // Actual code!

    // Constructor

    public Mips3(BusControllerAccess memory) {
        this.memory = memory;

        for(int i = 0; i < TLB_COUNT; i++) {
            this.tlbNext[i] = (i+1)%TLB_COUNT;
            this.tlbPrev[i] = (i+TLB_COUNT-1)%TLB_COUNT;
            this.tlbPageMask[i] = 0x0000_1FFFL;
        }
        this.tlbFirst = 0;

        this.reset();
    }

    // Faults

    private void fault(MFault code, MPipelineStage stage, long pc, boolean bd) {
        // Track old EXL flags
        boolean oldExl = (this.c0regs[C0_STATUS] & 2) != 0;

        // Set COP0 info
        this.c0regs[C0_STATUS] |= 2; // EXL
        this.c0regs[C0_CAUSE] = (long) (int) ((this.c0regs[C0_CAUSE] & 0x0000FF00)
                | ((code.code & 0x1F) << 2)
                | (bd ? 0x80000000 : 0));
        if(!oldExl) {
            this.c0regs[C0_EPC] = (bd ? pc - 4 : pc);
        }

        // Update important flags
        updateC0Cause(this.c0regs[C0_CAUSE]);
        updateC0Status(this.c0regs[C0_STATUS]);

        // Form contexts
        this.c0regs[C0_CONTEXT]
                = (this.c0regs[C0_CONTEXT]&0xFFFF_FFFF_FF80_0000L)
                | ((this.c0regs[C0_BADVADDR]>>(13-4))&0x0000_0000_007F_FFF0L);
        this.c0regs[C0_XCONTEXT]
                = (this.c0regs[C0_XCONTEXT]&0xFFFF_FFFF_0000_0000L)
                | ((this.c0regs[C0_BADVADDR]>>32)&0x0000_0000_C000_0000L)
                | ((this.c0regs[C0_BADVADDR]>>(13-4))&0x0000_0000_3FFF_FFF0L);

        // Branch
        long bvec = (((this.c0regs[C0_STATUS] >> 22) & 1) != 0 // BEV
                ? VECTOR_BASE_ROM
                : VECTOR_BASE_RAM);

        switch (code) {
            case TLBL:
            case TLBS:
                // Determine if we use the 64-bit XTLB refill handler
                if (oldExl) {
                    // SPECIAL CASE: Exceptions use the main handler
                    this.pcAfter = bvec + 0x180;
                } else if (kernelMode && ((this.c0regs[C0_STATUS] >> 7) & 1) != 0) {
                    this.pcAfter = bvec + 0x080;
                } else if (superMode && ((this.c0regs[C0_STATUS] >> 6) & 1) != 0) {
                    this.pcAfter = bvec + 0x080;
                } else if (((this.c0regs[C0_STATUS] >> 5) & 1) != 0) {
                    this.pcAfter = bvec + 0x080;
                } else {
                    // All failed. Use the 32-bit TLB refill handler instead
                    this.pcAfter = bvec + 0x000;
                }
                break;

            default:
                // Main handler
                this.pcAfter = bvec + 0x180;
                break;
        }

        // Kill op in pipeline
        skipInstruction();

        // DEBUG TOOL: print fault info
        if(false) {
            // Spew fault into log
            System.out.printf("MIPS FAULT:\n");
            System.out.printf("- Cause: %s (%d)\n", code.name, code.code);
            System.out.printf("- Pipeline stage: %s\n", stage.name);
            System.out.printf("- PC: %016X (BD = %s)\n", pc, bd ? "YES" : "no");
            System.out.printf("- c0_status:   %016X\n", this.c0regs[C0_STATUS]);
            System.out.printf("- c0_cause:    %016X\n", this.c0regs[C0_CAUSE]);
            System.out.printf("- c0_badvaddr: %016X\n", this.c0regs[C0_BADVADDR]);

            // Nuke cycle budget so we don't spam the console
            if (this.cycleBudget > 0) {
                this.cycleBudget = 0;
            }
        }
    }

    // "Immediate" reads and writes
    // These do not trigger MIPS faults.
    // Java exceptions perhaps, but no MIPS faults.
    //
    // They also completely bypass the cache,
    // and use physical addresses.

    private int read8Bus(final long addr) {
        // XXX: pending wider bus support
        //try {
            return 0xFF&(int)memory.read((int)addr);
        //} catch(ArrayIndexOutOfBoundsException e) {
            //return 0xFF;
        //}
    }

    private int read8Imm(final long addr) {
        this.cycleBudget -= 1;
        return read8Bus(addr);
    }

    private int read16Imm(final long addr) {
        this.cycleBudget -= 2;
        int v0 = read8Bus(addr+0);
        int v1 = read8Bus(addr+1);
        return v0 | (v1<<8);
    }

    private int read32Imm(final long addr) {
        this.cycleBudget -= 4;
        int v0 = read8Bus(addr+0);
        int v1 = read8Bus(addr+1);
        int v2 = read8Bus(addr+2);
        int v3 = read8Bus(addr+3);
        return v0 | (v1<<8) | (v2<<16) | (v3<<24);
    }

    private long read64Imm(final long addr) {
        long v0 = read32Imm(addr+0);
        long v1 = read32Imm(addr+4);
        return (v0 & 0xFFFFFFFFL) | (v1<<32L);
    }

    private void write8Imm(final long addr, final int data) {
        // XXX: pending wider data bus support
        this.cycleBudget -= 1;
        try {
            memory.write((int)addr, 0xFF&(int)data);
        } catch(ArrayIndexOutOfBoundsException e) {
        }
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

    // TLB functions

    private int getTlbIndex(long vaddr) {
        // Scan the TLB
        vaddr &= 0xC000_00FF_FFFF_F000L;
        for(int c = 0, i = tlbFirst; c < TLB_COUNT; c++, i = tlbNext[i]) {
            // Check if we hit
            if((vaddr & ~tlbPageMask[i]) == tlbBase[i]) {
                // Select subpage
                boolean use1 = ((vaddr & ((tlbPageMask[i]+1)>>1)) != 0);
                long lo = use1 ? tlbEntryLo1[i] : tlbEntryLo0[i];
                long hi = tlbEntryHi[i];

                // Check G(lobal), ASID
                if((lo&1) == 0 && ((hi^c0regs[C0_ENTRYHI])&0xFF) != 0) {
                    // ASID mismatch and nonglobal TLB - this one is MISSED
                    continue;
                }

                // Move entry to start
                // 1. ENSURE WE ARE NOT THE THING AT THE START
                if(i != tlbFirst) {
                    // 2. Remove self
                    int p = tlbPrev[i];
                    int n = tlbNext[i];
                    tlbNext[p] = tlbNext[i]; tlbPrev[n] = tlbPrev[i];
                    // 3. Insert self between last and first
                    int f = tlbFirst;
                    int l = tlbPrev[f];
                    tlbNext[l] = i; tlbPrev[i] = l;
                    tlbNext[i] = f; tlbPrev[f] = i;
                    // 4. Update first to point to us
                    tlbFirst = i;
                }

                // Check V(alid)
                if((lo&2) == 0) {
                    // Force miss
                    return -1;
                }

                // Return index
                return i;
            }
            //System.out.printf("TLB next %d -> %d\n", i, tlbNext[i]);
        }

        // TLB missed
        return -1;
    }

    private long getTlbPhysAddress(long vaddr) throws MipsTlbMissException {
        int idx = getTlbIndex(vaddr);
        if(idx < 0) {
            // TLB MISS
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsTlbMissException();
        }

        // Get fields
        boolean use1 = ((vaddr & ((tlbPageMask[idx]+1)>>1)) != 0);
        long lo = use1 ? tlbEntryLo1[idx] : tlbEntryLo0[idx];
        long mask = tlbPageMask[idx]>>1;

        // Pass on the lo field for things that need it
        this.tlbRecentLo = lo;

        // Remap
        return (vaddr&mask)|((lo<<6)&~mask);
    }

    private void tlbWriteAt(int idx) {
        idx = 63&idx;

        // Likely result of this
        //if(idx >= 32) { idx &= ~16; }

        // Easier result for adjusting things
        idx %= TLB_COUNT;

        // Set TLB fields
        this.tlbPageMask[idx] = ((this.c0regs[C0_PAGEMASK]&0x0000_0000_00FF_F000L)<<1)|0x1FFFL;
        this.tlbEntryHi[idx] = this.c0regs[C0_ENTRYHI]&~tlbPageMask[idx];
        this.tlbEntryLo0[idx] = this.c0regs[C0_ENTRYLO0];
        this.tlbEntryLo1[idx] = this.c0regs[C0_ENTRYLO1];

        // Set runtime TLB fields
        this.tlbBase[idx] = this.tlbEntryHi[idx] & ~this.tlbPageMask[idx];
    }

    // Memory fence (called during SYNC, LL, SC)
    private void memoryFence() {
        // TODO: flush cache and write-buffer
    }

    // Address remapping

    private long virtToPhys64(long inVaddr) throws MipsAddressErrorException, MipsTlbMissException {
        // Back up argument
        long vaddr = inVaddr;

        // Clamp if we are in 32-bit address mode
        if(!address64) {
            vaddr = (long)(int)vaddr;
        }

        // Check if we need to remap csegs
        if((vaddr>>31) == -1) {
            if(kernelMode || (superMode && (vaddr >= -0x40000000L && vaddr < -0x20000000L))) {
                switch (3&((int)(vaddr>>29L))) {
                    case 3: // 0xE0000000 - kernel TLB
                        vaddr = vaddr + 0xC000_0000_2000_0000L;
                        break;

                    case 2: // 0xC0000000 - supervisor TLB
                        vaddr = vaddr + 0x4000_0000_4000_0000L;
                        break;

                    case 1: // 0xA0000000 - kernel uncached unmapped
                        tlbRecentLo = 0x14;
                        return vaddr & 0x1FFFFFFFL;

                    case 0: // 0x80000000 - kernel cached unmapped
                        tlbRecentLo = this.k0base;
                        return vaddr & 0x1FFFFFFFL;
                }
            } else {
                this.c0regs[C0_BADVADDR] = inVaddr;
                throw new MipsAddressErrorException();
            }
        }

        switch(3&(int)(vaddr>>62)) {
            case 0: // User mode, TLB mapped
                if(((vaddr<<2)>>>40) != 0) {
                    break;
                }
                return getTlbPhysAddress(vaddr);

            case 1: // Supervisor mode, TLB mapped
                if(!superMode) {
                    break;
                }
                if(((vaddr<<2)>>>40) != 0) {
                    break;
                }
                return getTlbPhysAddress(vaddr);

            case 2: // Physical
                if(!kernelMode) {
                    break;
                } else {
                    long paddr = (vaddr<<5)>>>5;
                    if((paddr>>36) != 0) {
                        break;
                    }

                    long lo = (vaddr>>(59-3))&0x18;
                    tlbRecentLo = lo|4;
                    return paddr;
                }

            case 3: // Kernel mode, TLB mapped
                if(!kernelMode) {
                    break;
                }
                if(vaddr >= 0xC000_00FF_8000_0000L) {
                    break;
                }
                return getTlbPhysAddress(vaddr);
        }

        // ADDRESS ERROR
        this.c0regs[C0_BADVADDR] = inVaddr;
        throw new MipsAddressErrorException();
    }

    // Cache

    private boolean isInICache(long vaddr, long paddr) throws MipsBusErrorException {
        // Check if cached (VIPT cache)
        int ctidx = ((int)(vaddr>>5))&(ICACHE_SIZE_TAGS-1);
        int tag = iCacheTags[ctidx];
        if((tag&(3<<24)) != 0) {
            // It is. Check if paddr matches.
            if((tag&0x00FFFFFF) == (int)(paddr>>12)) {
                // It does. Return.
                return true;
            }
        }

        // Not in cache!
        return false;
    }

    private boolean isInDCache(long vaddr, long paddr) throws MipsBusErrorException {
        // Check if cached (VIPT cache)
        int ctidx = ((int)(vaddr>>5))&(DCACHE_SIZE_TAGS-1);
        int tag = dCacheTags[ctidx];
        if((tag&(3<<24)) != 0) {
            // It is. Check if paddr matches.
            if((tag&0x00FFFFFF) == (int)(paddr>>12)) {
                // It does. Return.
                return true;
            }
        }

        // Not in cache!
        return false;
    }

    private void invalidateDCacheImm(long vaddr) throws MipsBusErrorException {
        int ctidx = ((int)(vaddr>>5))&(DCACHE_SIZE_TAGS-1);
        int cdidx = ctidx<<2;
        int tag = this.dCacheTags[ctidx];

        // Evict properly if dirty + valid
        if((tag&0x13000000) == 0x13000000) {
            long opaddr = (((long)tag&0x00FFFFFFL)<<12) | (vaddr&0xFFF);

            write64Imm(opaddr + 0,  dCacheData[cdidx + 0]);
            write64Imm(opaddr + 8,  dCacheData[cdidx + 1]);
            write64Imm(opaddr + 16, dCacheData[cdidx + 2]);
            write64Imm(opaddr + 24, dCacheData[cdidx + 3]);
        }

        dCacheTags[ctidx] &= ~0x13000000;
    }

    private void fetchDCacheImm(long vaddr, long paddr) throws MipsBusErrorException {
        // Check if cached
        if(isInDCache(vaddr, paddr)) {
            return;
        }

        // XXX: do we handle cache errors?
        // Fetch 8 words / 4 dwords
        long pbase = paddr&~31;
        long d0 = read64Imm(pbase+0);
        long d1 = read64Imm(pbase+8);
        long d2 = read64Imm(pbase+16);
        long d3 = read64Imm(pbase+24);

        // Invalidate
        invalidateDCacheImm(vaddr);

        // Set tag
        int ctidx = ((int)(vaddr>>5))&(DCACHE_SIZE_TAGS-1);
        int cdidx = ctidx<<2;
        this.dCacheTags[ctidx] = ((int)(paddr>>12))|0x13000000;

        // Stash into cache
        /*
        System.out.printf("CD%04X = [%016X]->[%016X] %016X %016X %016X %016X\n"
                , ctidx<<5, vaddr, paddr
                , d0, d1, d2, d3
        );
        */
        dCacheData[cdidx + 0] = d0;
        dCacheData[cdidx + 1] = d1;
        dCacheData[cdidx + 2] = d2;
        dCacheData[cdidx + 3] = d3;

        // Done!
    }

    private void invalidateICacheV(long vaddr) {
        int ctidx = ((int)(vaddr>>5))&(ICACHE_SIZE_TAGS-1);
        iCacheTags[ctidx] &= ~0x01000000;
    }

    private void invalidateDCacheV(long vaddr) throws MipsBusErrorException {
        invalidateDCacheImm(vaddr);
    }

    private void invalidateICacheVHitNoWB(long vaddr) throws MipsAddressErrorException, MipsTlbMissException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        if(isInICache(vaddr, paddr)) {
            int ctidx = ((int) (vaddr >> 5)) & (ICACHE_SIZE_TAGS - 1);
            iCacheTags[ctidx] &= ~0x01000000;
        }
    }

    private void invalidateDCacheVHit(long vaddr) throws MipsAddressErrorException, MipsTlbMissException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        if(isInDCache(vaddr, paddr)) {
            invalidateDCacheV(vaddr);
        }
    }

    private void createDirtyExclusiveDCache(long vaddr) throws MipsAddressErrorException, MipsTlbMissException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        if(!isInDCache(vaddr, paddr)) {
            invalidateDCacheV(vaddr);
            this.dCacheTags[((int)vaddr>>5)&(DCACHE_SIZE_TAGS-1)]
                = 0x13000000 | ((int)paddr & 0x00FFFFFF);
        }
    }

    private void invalidateDCacheVHitNoWB(long vaddr) throws MipsAddressErrorException, MipsTlbMissException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        if(isInDCache(vaddr, paddr)) {
            int ctidx = ((int) (vaddr >> 5)) & (DCACHE_SIZE_TAGS - 1);
            dCacheTags[ctidx] &= ~0x01000000;
        }
    }

    private void writeBackICacheVHit(long vaddr) throws MipsAddressErrorException, MipsTlbMissException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        if(isInICache(vaddr, paddr)) {
            int ctidx = ((int) (vaddr >> 5)) & (ICACHE_SIZE_TAGS - 1);
            int cdidx = ctidx<<2;
            int tag = iCacheTags[ctidx];

            if((tag&0x01000000) == 0x01000000) {
                long opaddr = (((long)tag&0x00FFFFFFL)<<12) | (vaddr&0xFFF);

                long w0a = 0xFFFFFFFFL&(long)dCacheData[cdidx + 0];
                long w0b = 0xFFFFFFFFL&(long)dCacheData[cdidx + 1];
                long w1a = 0xFFFFFFFFL&(long)dCacheData[cdidx + 2];
                long w1b = 0xFFFFFFFFL&(long)dCacheData[cdidx + 3];
                long w2a = 0xFFFFFFFFL&(long)dCacheData[cdidx + 4];
                long w2b = 0xFFFFFFFFL&(long)dCacheData[cdidx + 5];
                long w3a = 0xFFFFFFFFL&(long)dCacheData[cdidx + 6];
                long w3b = 0xFFFFFFFFL&(long)dCacheData[cdidx + 7];
                long d0 = w0a | (w0b<<32);
                long d1 = w1a | (w1b<<32);
                long d2 = w2a | (w2b<<32);
                long d3 = w3a | (w3b<<32);

                write64Imm(opaddr + 0,  d0);
                write64Imm(opaddr + 8,  d1);
                write64Imm(opaddr + 16, d2);
                write64Imm(opaddr + 24, d3);
            }
        }
    }

    private void writeBackDCacheVHitDirty(long vaddr) throws MipsAddressErrorException, MipsTlbMissException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);
        if(isInDCache(vaddr, paddr)) {
            int ctidx = ((int) (vaddr >> 5)) & (DCACHE_SIZE_TAGS - 1);
            int cdidx = ctidx<<2;
            int tag = dCacheTags[ctidx];

            if((tag&0x13000000) == 0x13000000) {
                long opaddr = (((long)tag&0x00FFFFFFL)<<12) | (vaddr&0xFFF);

                long d0 = dCacheData[cdidx + 0];
                long d1 = dCacheData[cdidx + 1];
                long d2 = dCacheData[cdidx + 2];
                long d3 = dCacheData[cdidx + 3];

                write64Imm(opaddr + 0,  d0);
                write64Imm(opaddr + 8,  d1);
                write64Imm(opaddr + 16, d2);
                write64Imm(opaddr + 24, d3);

                dCacheTags[ctidx] &= ~0x10000000;
            }
        }
    }


    // Situational reads and writes

    private void fetchInstrLineImm(long vaddr, long paddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        // XXX: do we handle cache errors?
        // Fetch 8 words / 4 dwords
        long pbase = paddr&~31;
        long d0 = read64Imm(pbase+0);
        long d1 = read64Imm(pbase+8);
        long d2 = read64Imm(pbase+16);
        long d3 = read64Imm(pbase+24);

        // Set tag
        int ctidx = ((int)(vaddr>>5))&(ICACHE_SIZE_TAGS-1);
        this.iCacheTags[ctidx] = ((int)(paddr>>12))|(1<<24);

        // Stash into cache
        int cdidx = ctidx<<3;
        iCacheData[cdidx + 0] = (int)(d0);
        iCacheData[cdidx + 1] = (int)(d0>>32L);
        iCacheData[cdidx + 2] = (int)(d1);
        iCacheData[cdidx + 3] = (int)(d1>>32L);
        iCacheData[cdidx + 4] = (int)(d2);
        iCacheData[cdidx + 5] = (int)(d2>>32L);
        iCacheData[cdidx + 6] = (int)(d3);
        iCacheData[cdidx + 7] = (int)(d3>>32L);

    }

    private void fetchInstrLine(long vaddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        // TLB fetch
        long paddr = virtToPhys64(vaddr);

        // Actually do the fetch
        fetchInstrLineImm(vaddr, paddr);
    }

    private int readInstr(long vaddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        // Ensure proper alignment
        if((vaddr&3)!= 0) {
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsAddressErrorException();
        }

        // TLB fetch!
        long paddr = virtToPhys64(vaddr);

        // Check if cached (VIPT cache)
        int ctidx = ((int)(vaddr>>5))&(ICACHE_SIZE_TAGS-1);
        int tag = iCacheTags[ctidx];
        if((tag&(1<<24)) != 0) {
            // It is. Check if paddr matches.
            if((tag&0x00FFFFFF) == (int)(paddr>>12)) {
                // It does. Return the cache data.
                return iCacheData[((int)(vaddr>>2))&(ICACHE_SIZE_WORDS-1)];
            }
        }

        // Check if cacheable
        if(((tlbRecentLo>>3)&3) == 2) {
            // Uncacheable
            return read32Imm(paddr);
        } else {
            fetchInstrLineImm(vaddr, paddr);
            return iCacheData[((int)(vaddr>>2))&(ICACHE_SIZE_WORDS-1)];
        }

    }

    private void fetchDCache(long vaddr, long paddr) throws MipsBusErrorException {
        // Check if cached
        if (isInDCache(vaddr, paddr)) {
            return;
        }

        // Fetch it
        fetchDCacheImm(vaddr, paddr);
    }

    private int readData8(long vaddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        long paddr = virtToPhys64(vaddr);

        if(((tlbRecentLo>>3)&3) == 2) {
            return read8Imm(paddr);
        } else {
            fetchDCache(vaddr, paddr);
            return 0xFF&(int)(dCacheData[((int)(vaddr>>3))&(DCACHE_SIZE_DWORDS-1)]>>((vaddr&7)<<3));
        }
    }

    private int readData16(long vaddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        // Ensure proper alignment
        if((vaddr&1)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);

        if(((tlbRecentLo>>3)&3) == 2) {
            return read16Imm(paddr);
        } else {
            fetchDCache(vaddr, paddr);
            return 0xFFFF&(int)(dCacheData[((int)(vaddr>>3))&(DCACHE_SIZE_DWORDS-1)]>>((vaddr&6)<<3));
        }
    }

    private int readData32(long vaddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        // Ensure proper alignment
        if((vaddr&3)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);

        if(((tlbRecentLo>>3)&3) == 2) {
            return read32Imm(paddr);
        } else {
            fetchDCache(vaddr, paddr);
            return (int)(dCacheData[((int)(vaddr>>3))&(DCACHE_SIZE_DWORDS-1)]>>((vaddr&4)<<3));
        }
    }

    private long readData64(long vaddr) throws MipsAddressErrorException, MipsBusErrorException, MipsTlbMissException {
        // Ensure proper alignment
        if((vaddr&7)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);

        if(((tlbRecentLo>>3)&3) == 2) {
            return read64Imm(paddr);
        } else {
            fetchDCache(vaddr, paddr);
            return dCacheData[((int)(vaddr>>3))&(DCACHE_SIZE_DWORDS-1)];
        }
    }

    private void writeData8(long vaddr, int data) throws MipsAddressErrorException, MipsTlbMissException, MipsTlbModException, MipsBusErrorException {
        long paddr = virtToPhys64(vaddr);

        // Check dirty (writeable) bit
        if((tlbRecentLo & 4) == 0) {
            // PERMISSION DENIED
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsTlbModException();
        }

        // Fetch dcache if write-allocate
        boolean writeToCache;
        if((((int)tlbRecentLo>>3)&1) == 1) {
            fetchDCache(vaddr, paddr);
            writeToCache = true;
        } else if((((int)tlbRecentLo>>3)&3) == 0) {
            writeToCache = isInDCache(vaddr, paddr);
        } else {
            writeToCache = false;

        }

        // Write if not write-back cached
        if((((int)tlbRecentLo>>3)&3) != 3) {
            write8Imm(paddr, data);
        }

        // Write to cache where sensible
        if(writeToCache) {
            int cdidx = ((int)vaddr>>3)&(DCACHE_SIZE_DWORDS-1);
            int shift = (((int)vaddr&7)<<3);
            long mask = (0xFFL<<shift);
            data &= 0xFF;
            this.dCacheData[cdidx] &= ~mask;
            this.dCacheData[cdidx] |= ((long)data)<<shift;
        }
    }

    private void writeData16(long vaddr, int data) throws MipsAddressErrorException, MipsTlbMissException, MipsTlbModException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&1)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);

        // Check dirty (writeable) bit
        if((tlbRecentLo & 4) == 0) {
            // PERMISSION DENIED
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsTlbModException();
        }

        // Fetch dcache if write-allocate
        boolean writeToCache;
        if((((int)tlbRecentLo>>3)&1) == 1) {
            fetchDCache(vaddr, paddr);
            writeToCache = true;
        } else if((((int)tlbRecentLo>>3)&3) == 0) {
            writeToCache = isInDCache(vaddr, paddr);
        } else {
            writeToCache = false;

        }

        // Write if not write-back cached
        if((((int)tlbRecentLo>>3)&3) != 3) {
            write16Imm(paddr, data);
        }

        // Write to cache where sensible
        if(writeToCache) {
            int cdidx = ((int)vaddr>>3)&(DCACHE_SIZE_DWORDS-1);
            int shift = (((int)vaddr&7)<<3);
            long mask = (0xFFFFL<<shift);
            data &= 0xFFFF;
            this.dCacheData[cdidx] &= ~mask;
            this.dCacheData[cdidx] |= ((long)data)<<shift;
        }
    }

    private void writeData32(long vaddr, int data) throws MipsAddressErrorException, MipsTlbMissException, MipsTlbModException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&3)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);

        // Check dirty (writeable) bit
        if((tlbRecentLo & 4) == 0) {
            // PERMISSION DENIED
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsTlbModException();
        }

        // Fetch dcache if write-allocate
        boolean writeToCache;
        if((((int)tlbRecentLo>>3)&1) == 1) {
            fetchDCache(vaddr, paddr);
            writeToCache = true;
        } else if((((int)tlbRecentLo>>3)&3) == 0) {
            writeToCache = isInDCache(vaddr, paddr);
        } else {
            writeToCache = false;

        }

        // Write if not write-back cached
        if((((int)tlbRecentLo>>3)&3) != 3) {
            write32Imm(paddr, data);
        }

        // Write to cache where sensible
        if(writeToCache) {
            int cdidx = ((int)vaddr>>3)&(DCACHE_SIZE_DWORDS-1);
            int shift = (((int)vaddr&7)<<3);
            long mask = (0xFFFFFFFFL<<shift);
            this.dCacheData[cdidx] &= ~mask;
            this.dCacheData[cdidx] |= (0xFFFFFFFFL&(long)data)<<shift;
        }
    }

    private void writeData64(long vaddr, long data) throws MipsAddressErrorException, MipsTlbMissException, MipsTlbModException, MipsBusErrorException {
        // Ensure proper alignment
        if((vaddr&7)!= 0) {
            throw new MipsAddressErrorException();
        }

        long paddr = virtToPhys64(vaddr);

        // Check dirty (writeable) bit
        if((tlbRecentLo & 4) == 0) {
            // PERMISSION DENIED
            this.c0regs[C0_BADVADDR] = vaddr;
            throw new MipsTlbModException();
        }

        // Fetch dcache if write-allocate
        boolean writeToCache;
        if((((int)tlbRecentLo>>3)&1) == 1) {
            fetchDCache(vaddr, paddr);
            writeToCache = true;
        } else if((((int)tlbRecentLo>>3)&3) == 0) {
            writeToCache = isInDCache(vaddr, paddr);
        } else {
            writeToCache = false;

        }

        // Write if not write-back cached
        if((((int)tlbRecentLo>>3)&3) != 3) {
            write64Imm(paddr, data);
        }

        // Write to cache where sensible
        if(writeToCache) {
            int cdidx = ((int)vaddr>>3)&(DCACHE_SIZE_DWORDS-1);
            this.dCacheData[cdidx] = data;
        }
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

        if((sr&6) != 0) { modeBits = 0; }
        kernelMode = (modeBits <= 0);
        superMode = (modeBits <= 1) || kernelMode;
        address64 = ((sr>>(7-modeBits))&1) != 0;
        allow64 = address64 || kernelMode;

        /*
        System.err.printf("SR update: k=%c | s=%c | d=%c | a=%c\n"
                , (kernelMode ? 'Y' : 'n')
                , (superMode ? 'Y' : 'n')
                , (allow64 ? 'Y' : 'n')
                , (address64 ? 'Y' : 'n')
                );
        */
    }

    private void updateC0Cause(long v)
    {
        updateC0RegMasked(C0_CAUSE, v, (long)(int)0x00000300);
    }

    private void updateC0Config(long v)
    {
        updateC0RegMasked(C0_CONFIG, v, (long)(int)0x00000007);
        this.k0base = ((v<<3)&0x18)|0x04;
    }

    // Functions used in main run-op loop

    private void skipInstruction() {
        this.cycleBudget -= 1;
        // omitted for speed
        /*
        try {
            readInstr(this.pcNext);
        } catch (MipsAddressErrorException e) {
        } catch (MipsBusErrorException e) {
        } catch (MipsTlbMissException e) {
        }
        */
        this.pcBranchDelay = false;
        this.pcNext = this.pcAfter;
        this.pcAfter += 4;
    }

    private void branchTo(long newPc) {
        this.pcAfter = newPc;
        this.pcBranchDelay = true;
    }

    // Main run-op loop

    private void runOp() {
        this.cycleBudget -= 1;

        // Fetch op (IC)
        boolean ex_bd = this.pcBranchDelay;
        long ex_pc = this.pc;
        int ex_op;
        try {
            ex_op = readInstr(ex_pc);
        } catch (MipsTlbMissException e) {
            fault(MFault.TLBL, MPipelineStage.IC, ex_pc, ex_bd);
            this.pc = this.pcNext;
            this.pcNext = this.pcAfter;
            this.pcAfter += 4;
            return;
        } catch (MipsAddressErrorException e) {
            fault(MFault.AdEL, MPipelineStage.IC, ex_pc, ex_bd);
            this.pc = this.pcNext;
            this.pcNext = this.pcAfter;
            this.pcAfter += 4;
            return;
        } catch (MipsBusErrorException e) {
            fault(MFault.IBE, MPipelineStage.IC, ex_pc, ex_bd);
            this.pc = this.pcNext;
            this.pcNext = this.pcAfter;
            this.pcAfter += 4;
            return;
        }

        // Clear next status
        this.pcBranchDelay = false;

        // Execute op (RF/EX)
        // immu == ex_op&0xFFFF
        // imms == (int)(short)ex_op
        // shamt == (ex_op>>>6)&31
        // func == ex_op&63

        long wb_result;

        this.regs[0] = 0;
        int opcode_type = (ex_op >>> 26) & 63;
        int rs = (ex_op >>> 21) & 31;
        int rt = (ex_op >>> 16) & 31;
        if(opcode_type == 0) {
            int opcode_func = ex_op&63;
            int rd = (ex_op >>> 11) & 31;

            switch (opcode_func) {

                // Shifts

                case 0: // SLL
                    this.regs[rd] = (long)(((int)this.regs[rt]) << ((ex_op>>>6)&31));
                    break;
                case 2: // SRL
                    this.regs[rd] = (long)(((int)this.regs[rt]) >>> ((ex_op>>>6)&31));
                    break;
                case 3: // SRA
                    this.regs[rd] = (long)(((int)this.regs[rt]) >> ((ex_op>>>6)&31));
                    break;

                case 4: // SLLV
                    this.regs[rd] = (long)(((int)this.regs[rt]) << (this.regs[rs]&31));
                    break;
                case 6: // SRLV
                    this.regs[rd] = (long)(((int)this.regs[rt]) >>> (this.regs[rs]&31));
                    break;
                case 7: // SRAV
                    this.regs[rd] = (long)(((int)this.regs[rt]) >> (this.regs[rs]&31));
                    break;

                // Register branches

                case 9: // JALR
                    this.regs[rd] = ex_pc+8;
                    // *** FALL THROUGH
                case 8: // JR
                    this.pcAfter = this.regs[rs];
                    this.pcBranchDelay = true;
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
                    memoryFence();
                    break;

                // LO/HI moves

                case 16: // MFHI
                    this.regs[rd] = this.mdhi;
                    break;
                case 17: // MTHI
                    this.mdhi = this.regs[rs];
                    break;
                case 18: // MFLO
                    this.regs[rd] = this.mdlo;
                    break;
                case 19: // MTLO
                    this.mdlo = this.regs[rs];
                    break;

                // 64-bit register shifts

                case 20: // DSLLV
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] << (this.regs[rs]&63);
                    }
                    break;
                case 22: // DSRLV
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] >>> (this.regs[rs]&63);
                    }
                    break;
                case 23: // DSRAV
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] >> (this.regs[rs]&63);
                    }
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
                    wb_result = (long)(int)(((int)this.regs[rs]) + (int)this.regs[rt]);
                    if(this.regs[rt] >= 0
                            ? wb_result < (int)this.regs[rs]
                            : wb_result > (int)this.regs[rs]
                            ) {
                        fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = wb_result;
                    }
                    break;
                case 33: // ADDU
                    this.regs[rd] = (long)(int)(((int)this.regs[rs]) + (int)this.regs[rt]);
                    break;
                case 34: // SUB
                    wb_result = (long)(int)(((int)this.regs[rs]) - (int)this.regs[rt]);
                    if(this.regs[rt] < 0
                            ? wb_result < (int)this.regs[rs]
                            : wb_result > (int)this.regs[rs]
                            ) {
                        rd = 0;
                        fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = wb_result;
                    }
                    break;
                case 35: // SUBU
                    this.regs[rd] = (long)(int)(((int)this.regs[rs]) - (int)this.regs[rt]);
                    break;
                case 36: // AND
                    this.regs[rd] = this.regs[rs] & this.regs[rt];
                    break;
                case 37: // OR
                    this.regs[rd] = this.regs[rs] | this.regs[rt];
                    break;
                case 38: // XOR
                    this.regs[rd] = this.regs[rs] ^ this.regs[rt];
                    break;
                case 39: // NOR
                    this.regs[rd] = ~(this.regs[rs] | this.regs[rt]);
                    break;

                case 42: // SLT
                    this.regs[rd] = (this.regs[rs]) < (this.regs[rt]) ? 1 : 0;
                    break;
                case 43: // SLTU
                    this.regs[rd] = ((this.regs[rs])^0x8000000000000000L) <
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
                        fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = wb_result;
                    }
                    break;
                case 45: // DADDU
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rs] + this.regs[rt];
                    }
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
                        fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = wb_result;
                    }
                    break;
                case 47: // DSUBU
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rs] - this.regs[rt];
                    }
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
                    } else {
                        this.regs[rd] = this.regs[rt] << ((ex_op>>>6)&31);
                    }

                    break;
                case 58: // DSRL
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] >>> ((ex_op>>>6)&31);
                    }
                    break;
                case 59: // DSRA
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] >> ((ex_op>>>6)&31);
                    }
                    break;

                case 60: // DSLL32
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] << (32+((ex_op>>>6)&31));
                    }
                    break;
                case 62: // DSRL32
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] >>> (32+((ex_op>>>6)&31));
                    }
                    break;
                case 63: // DSRA32
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rd] = this.regs[rt] >> (32+((ex_op>>>6)&31));
                    }
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
        } else {
            switch(opcode_type) {

                // REGIMM

                case 1: switch(rt) {

                    // Branches

                    case 0: // BLTZ
                        if(this.regs[rs] < 0) {
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        }
                        break;
                    case 1: // BGEZ
                        if(this.regs[rs] >= 0) {
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        }
                        break;
                    case 2: // BLTZL
                        if(this.regs[rs] < 0) {
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        } else {
                            skipInstruction();
                        }
                        break;
                    case 3: // BGEZL
                        if(this.regs[rs] >= 0) {
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        } else {
                            skipInstruction();
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
                            this.regs[31] = ex_pc+8;
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        }
                        break;
                    case 17: // BGEZAL
                        if(this.regs[rs] >= 0) {
                            this.regs[31] = ex_pc+8;
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        }
                        break;
                    case 18: // BLTZALL
                        if(this.regs[rs] < 0) {
                            this.regs[31] = ex_pc+8;
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        } else {
                            skipInstruction();
                        }
                        break;
                    case 19: // BGEZALL
                        if(this.regs[rs] >= 0) {
                            this.regs[31] = ex_pc+8;
                            branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                        } else {
                            skipInstruction();
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
                    this.regs[31] = ex_pc+8;
                    // *** FALL THROUGH
                case 2: // J
                    branchTo((ex_pc & ~0x0FFFFFFFL) | (long)((ex_op<<2)&0x0FFFFFFF));
                    break;

                // Branches

                case 4: // BEQ
                    if(this.regs[rs] == this.regs[rt]) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    }
                    break;
                case 5: // BNE
                    if(this.regs[rs] != this.regs[rt]) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    }
                    break;
                case 6: // BLEZ
                    if(this.regs[rs] <= 0) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    }
                    break;
                case 7: // BGTZ
                    if(this.regs[rs] > 0) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    }
                    break;

                // Immediate arithmetricks

                case 8: // ADDI
                    wb_result = (long)(int)(((int)this.regs[rs]) + (int)(short)(ex_op));
                    if((short)ex_op >= 0
                            ? wb_result < (int)this.regs[rs]
                            : wb_result > (int)this.regs[rs]
                            ) {
                        fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rt] = wb_result;
                    }
                    break;

                case 9: // ADDIU
                    this.regs[rt] = (long)(int)(((int)this.regs[rs]) + (int)(short)(ex_op));
                    break;

                case 10: // SLTI
                    this.regs[rt] = (this.regs[rs]) < (long)(short)(ex_op) ? 1 : 0;
                    break;
                case 11: // SLTIU
                    // If I had a dollar for every time I wished this were C,
                    // ...at the time of writing I think I'm up to $2
                    // UPDATE: I'm at about $5 now.
                    this.regs[rt] = ((this.regs[rs])^0x8000000000000000L) <
                            (((long)(short)(ex_op))^0x8000000000000000L) ? 1 : 0;
                    break;

                case 12: // ANDI
                    this.regs[rt] = this.regs[rs] & (0xFFFF&(long)(ex_op));
                    break;
                case 13: // ORI
                    this.regs[rt] = this.regs[rs] | (0xFFFF&(long)(ex_op));
                    break;
                case 14: // XORI
                    this.regs[rt] = this.regs[rs] ^ (0xFFFF&(long)(ex_op));
                    break;

                case 15: // LUI
                    this.regs[rt] = (long)(int)(ex_op<<16);
                    break;

                // COP0

                case 16: if((this.c0regs[C0_STATUS]&(1<<28)) == 0 && !kernelMode) {
                    fault(MFault.CpU, MPipelineStage.EX, ex_pc, ex_bd);
                    this.c0regs[C0_CAUSE] |= 0<<28;
                } else if(rs < 16) {
                    int rd = (ex_op >>> 11) & 31;
                    switch(rs) {
                        // MFC0
                        case 0: switch(rd) {
                            case C0_INDEX:
                            case C0_RANDOM:
                            case C0_ENTRYLO0:
                            case C0_ENTRYLO1:
                            case C0_CONTEXT:
                            case C0_PAGEMASK:
                            case C0_WIRED:
                            case C0_BADVADDR:
                            case C0_ENTRYHI:
                            case C0_STATUS:
                            case C0_CAUSE:
                            case C0_EPC:
                            case C0_CONFIG:
                            case C0_XCONTEXT:
                            case C0_TAGLO:
                            case C0_TAGHI:
                                this.regs[rt] = (long)(int)this.c0regs[rd];
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
                                case C0_INDEX:
                                case C0_RANDOM:
                                case C0_ENTRYLO0:
                                case C0_ENTRYLO1:
                                case C0_CONTEXT:
                                case C0_PAGEMASK:
                                case C0_WIRED:
                                case C0_BADVADDR:
                                case C0_ENTRYHI:
                                case C0_STATUS:
                                case C0_CAUSE:
                                case C0_EPC:
                                case C0_CONFIG:
                                case C0_XCONTEXT:
                                case C0_TAGLO:
                                case C0_TAGHI:
                                    this.regs[rt] = this.c0regs[rd];
                                    break;

                                default:
                                    System.err.printf("RI DMFC0: %d\n", rd);
                                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                                    break;
                            }
                        } break;

                        // MTC0
                        case 4: switch(rd) {
                            case C0_INDEX:
                                this.c0regs[rd] = 0x8000_003FL&this.regs[rt];
                                break;
                            case C0_RANDOM:
                                this.c0regs[rd] = 63&this.regs[rt];
                                break;

                            case C0_ENTRYLO0:
                            case C0_ENTRYLO1:
                                this.c0regs[rd] = 0x3FFF_FFFFL&this.regs[rt];
                                break;

                            case C0_CONTEXT:
                                this.c0regs[rd] = (this.c0regs[rd]&0x007FFFFFL)
                                        |(((long)(int)this.c0regs[rt])&~0x007FFFFFL);
                                break;

                            case C0_PAGEMASK:
                                this.c0regs[rd] = 0x0055_5000L&this.regs[rt];
                                this.c0regs[rd] |= this.c0regs[rd]<<1;
                                break;

                            case C0_WIRED:
                                this.c0regs[rd] = 63&this.regs[rt];
                                this.c0regs[C0_RANDOM] = TLB_COUNT;
                                break;

                            case C0_ENTRYHI:
                                this.c0regs[rd] = 0xC000_00FF_FFFF_E000L&(long)(int)this.regs[rt];
                                break;

                            case C0_STATUS:
                                updateC0Status((long)(int)this.regs[rt]);
                                break;
                            case C0_CAUSE:
                                updateC0Cause((long)(int)this.regs[rt]);
                                break;
                            case C0_CONFIG:
                                updateC0Config((long)(int)this.regs[rt]);
                                break;

                            case C0_XCONTEXT:
                                this.c0regs[rd] = (this.c0regs[rd]&0xFFFFFFFFL)
                                        |(((long)(int)this.c0regs[rt])&~0xFFFFFFFFL);
                                break;

                            case C0_TAGLO:
                                this.c0regs[rd] = (long)(int)this.regs[rt];
                                this.c0regs[rd] &= ~2;
                                break;
                            case C0_TAGHI:
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
                                case C0_INDEX:
                                    this.c0regs[rd] = 0x8000_003FL&this.regs[rt];
                                    break;
                                case C0_RANDOM:
                                    this.c0regs[rd] = 63&this.regs[rt];
                                    break;

                                case C0_ENTRYLO0:
                                case C0_ENTRYLO1:
                                    this.c0regs[rd] = 0x3FFF_FFFFL&this.regs[rt];
                                    break;

                                case C0_CONTEXT:
                                    this.c0regs[rd] = (this.c0regs[rd]&0x007FFFFFL)
                                            |(this.c0regs[rt]&~0x007FFFFFL);
                                    break;

                                case C0_PAGEMASK:
                                    this.c0regs[rd] = 0x0055_5000L&this.regs[rt];
                                    this.c0regs[rd] |= this.c0regs[rd]<<1;
                                    break;

                                case C0_WIRED:
                                    this.c0regs[rd] = 63&this.regs[rt];
                                    this.c0regs[C0_RANDOM] = TLB_COUNT;
                                    break;

                                case C0_ENTRYHI:
                                    this.c0regs[rd] = 0xC000_00FF_FFFF_E000L&this.regs[rt];
                                    break;

                                case C0_STATUS:
                                    updateC0Status(this.regs[rt]);
                                    break;
                                case C0_CAUSE:
                                    updateC0Cause(this.regs[rt]);
                                    break;
                                case C0_CONFIG:
                                    updateC0Config(this.regs[rt]);
                                    break;

                                case C0_XCONTEXT:
                                    this.c0regs[rd] = (this.c0regs[rd]&0xFFFFFFFFL)
                                            |(this.c0regs[rt]&~0xFFFFFFFFL);
                                    break;

                                case C0_TAGLO:
                                    this.c0regs[rd] = (long)(int)this.regs[rt];
                                    this.c0regs[rd] &= ~2;
                                    break;
                                case C0_TAGHI:
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

                        case 1: { // TLBR
                            int idx = 63&(int)this.c0regs[C0_INDEX];

                            // Likely result of this
                            //if(idx >= 32) { idx &= ~16; }

                            // Easier result for adjusting things
                            idx %= TLB_COUNT;

                            this.c0regs[C0_PAGEMASK] = (this.tlbPageMask[idx]>>1) & 0x0000_0000_00FF_F000L;
                            this.c0regs[C0_ENTRYHI] = this.tlbEntryHi[idx];
                            this.c0regs[C0_ENTRYLO0] = this.tlbEntryLo0[idx];
                            this.c0regs[C0_ENTRYLO1] = this.tlbEntryLo1[idx];
                        } break;
                        case 2: // TLBWI
                            tlbWriteAt((int)this.c0regs[C0_INDEX]);
                            break;
                        case 6: // TLBWR
                            //System.out.printf("TLBWR %d\n", this.c0regs[C0_RANDOM]);
                            //tlbWriteAt((int)this.c0regs[C0_RANDOM]);
                            tlbWriteAt((int)Math.floor(Math.random()*(48-this.c0regs[C0_WIRED])+this.c0regs[C0_WIRED]));
                            break;
                        case 8: { // TLBP
                            this.c0regs[C0_INDEX] = (long)(int)0x80000000;
                            int idx = getTlbIndex(this.c0regs[C0_ENTRYHI]);
                            if(idx >= 0) {
                                this.c0regs[C0_INDEX] = (long)(int)idx;
                            }
                        } break;

                        // 16 is RFE, which was REMOVED in MIPS3.

                        case 24: // ERET
                            if((this.c0regs[C0_STATUS]&4) != 0) {
                                // return from error
                                this.c0regs[C0_STATUS] &= ~4; // ERL
                                this.pcAfter = this.c0regs[C0_ERROREPC];
                            } else {
                                // return from exception
                                this.c0regs[C0_STATUS] &= ~2; // EXL
                                this.pcAfter = this.c0regs[C0_EPC];
                            }

                            updateC0Status(this.c0regs[C0_STATUS]);

                            // TODO: clear LLbit (in other words, make any active LL/SC fail)

                            // cancel next op
                            skipInstruction();

                            // done!
                            break;

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
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    } else {
                        skipInstruction();
                    }
                    break;
                case 21: // BNEL
                    if(this.regs[rs] != this.regs[rt]) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    } else {
                        skipInstruction();
                    }
                    break;
                case 22: // BLEZL
                    if(this.regs[rs] <= 0) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    } else {
                        skipInstruction();
                    }
                    break;
                case 23: // BGTZL
                    if(this.regs[rs] > 0) {
                        branchTo(ex_pc + 4 + (((long)(short)ex_op)<<2));
                    } else {
                        skipInstruction();
                    }
                    break;

                // Immediate 64-bit arithmetricks

                case 24: // DADDI
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    }
                    wb_result = this.regs[rs] + (long)(short)(ex_op);
                    if((short)ex_op >= 0
                            ? wb_result < (long)this.regs[rs]
                            : wb_result > (long)this.regs[rs]
                            ) {
                        fault(MFault.Ov, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rt] = wb_result;
                    }
                    break;

                case 25: // DADDIU
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    } else {
                        this.regs[rt] = this.regs[rs] + (long)(short)ex_op;
                    }
                    break;

                // Load primary

                case 32: // LB
                    try {
                        this.regs[rt] = (long)readData8(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 33: // LH
                    try {
                        this.regs[rt] = (long)readData16(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 35: // LW
                    try {
                        this.regs[rt] = (long)readData32(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 36: // LBU
                    try {
                        this.regs[rt] = 0xFFL&(long)readData8(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 37: // LHU
                    try {
                        this.regs[rt] = 0xFFFFL&(long)readData16(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdEL, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 39: // LWU
                    try {
                        this.regs[rt] = 0xFFFFFFFFL&(long)readData32(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
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
                    } catch (MipsTlbModException e) {
                        fault(MFault.Mod, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBS, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 41: // SH
                    try {
                        writeData16(this.regs[rs] + (long)(short)ex_op, (int)this.regs[rt]);
                    } catch (MipsTlbModException e) {
                        fault(MFault.Mod, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBS, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                case 43: // SW
                    try {
                        writeData32(this.regs[rs] + (long)(short)ex_op, (int)this.regs[rt]);
                    } catch (MipsTlbModException e) {
                        fault(MFault.Mod, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBS, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                // Store unaligned - TODO
                // case 42: // SWL break;
                // case 46: // SWR break;
                // case 44: // SDL break;
                // case 45: // SDR break;

                // Cache ops

                case 47: if((this.c0regs[C0_STATUS]&(1<<28)) == 0 && !kernelMode) {
                    fault(MFault.CpU, MPipelineStage.EX, ex_pc, ex_bd);
                    this.c0regs[C0_CAUSE] |= 0<<28;
                } else {
                    long vaddr = this.regs[rs] + (long)(short)ex_op;
                    if(!address64) {
                        vaddr = (long)(int)vaddr;
                    }
                    switch(rt) {
                        case 0: // Index Invalidate [I]
                            invalidateICacheV(vaddr);
                            break;
                        case 1: // Index Write-Back Invalidate [D]
                            try {
                                invalidateDCacheV(vaddr);
                            } catch (MipsBusErrorException e) {
                                // TODO: cache fault
                            }
                            break;

                        case 4: // Index Load Tag [I]
                        {
                            int tag = this.iCacheTags[((int)vaddr>>5)&(ICACHE_SIZE_TAGS-1)];
                            int v = (int)this.c0regs[C0_TAGLO]&0x38;
                            v |= tag<<8;
                            if((tag&0x01000000)!=0) {
                                v |= 0xC0;
                            }
                            this.c0regs[C0_TAGLO] = (long)v;
                        } break;
                        case 5: // Index Load Tag [D]
                        {
                            int tag = this.dCacheTags[((int)vaddr>>5)&(DCACHE_SIZE_TAGS-1)];
                            int v = (int)this.c0regs[C0_TAGLO]&0x38;
                            v |= tag<<8;
                            v |= (tag&0x03000000)>>18;
                            if((tag&0x01000000)!=0) {
                                v |= 0xC0;
                            }
                            this.c0regs[C0_TAGLO] = (long)v;
                        } break;

                        case 8: // Index Store Tag [I]
                        {
                            int v = (int)this.c0regs[C0_TAGLO];
                            int ctidx = ((int)vaddr>>5)&(ICACHE_SIZE_TAGS-1);
                            int tag = this.iCacheTags[ctidx];
                            tag &= 0x10000000;
                            tag |= (v & 0xFFFFFF00)>>8;
                            tag |= (v & 0xC0)<<18;
                            this.iCacheTags[((int)vaddr>>5)&(ICACHE_SIZE_TAGS-1)] = tag;
                        } break;
                        case 9: // Index Store Tag [D]
                        {
                            int v = (int)this.c0regs[C0_TAGLO];
                            int ctidx = ((int)vaddr>>5)&(DCACHE_SIZE_TAGS-1);
                            int tag = this.dCacheTags[ctidx];
                            tag &= 0x00000000;
                            tag |= (v & 0xFFFFFF00)>>8;
                            tag |= (v & 0x40)<<18;
                            this.dCacheTags[((int)vaddr>>5)&(DCACHE_SIZE_TAGS-1)] = tag;
                        } break;

                        case 13: // Create Dirty Exclusive [D]
                            try {
                                createDirtyExclusiveDCache(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }

                        case 16: // Hit Invalidate [I]
                            try {
                                invalidateICacheVHitNoWB(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }
                            break;
                        case 17: // Hit Invalidate [D]
                            try {
                                invalidateDCacheVHitNoWB(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }
                            break;

                        case 20: // Fill [I]
                            try {
                                fetchInstrLine(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }
                            break;

                        case 21: // Hit Write-Back Invalidate [D]
                            try {
                                invalidateDCacheVHit(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }
                            break;

                        case 24: // Hit Write-Back [I] -- unconditional writeback from icache
                            try {
                                writeBackICacheVHit(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }
                            break;
                        case 25: // Hit Write-Back [D]
                            try {
                                writeBackDCacheVHitDirty(vaddr);
                            } catch (MipsAddressErrorException e) {
                            } catch (MipsTlbMissException e) {
                            } catch (MipsBusErrorException e) {
                            }
                            break;

                        default:
                            System.err.printf("RI CACHE: %d\n", rt);
                            fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                            break;
                    }

                } break;

                // Load fancy

                case 55: // LD
                    if(!allow64) {
                        fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                        break;
                    }
                    try {
                        this.regs[rt] = readData64(this.regs[rs] + (long)(short)ex_op);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBL, MPipelineStage.DC, ex_pc, ex_bd);
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
                    } catch (MipsTlbModException e) {
                        fault(MFault.Mod, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsTlbMissException e) {
                        fault(MFault.TLBS, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsAddressErrorException e) {
                        fault(MFault.AdES, MPipelineStage.DC, ex_pc, ex_bd);
                    } catch (MipsBusErrorException e) {
                        fault(MFault.DBE, MPipelineStage.DC, ex_pc, ex_bd);
                    }
                    break;

                // Reserved instruction fault

                default:
                    System.err.printf("RI: %d\n", opcode_type);
                    fault(MFault.RI, MPipelineStage.EX, ex_pc, ex_bd);
                    break;
            }
        }

        // Advance PC
        this.pc = this.pcNext;
        this.pcNext = this.pcAfter;
        this.pcAfter += 4;

        // omitted for speed
        /*
        // Advance c0_random
        // (R4600 only advances on valid instructions)
        if(this.fault_type != MFault.RI) {
            this.c0regs[C0_RANDOM]--;
            if(this.c0regs[C0_RANDOM] < this.c0regs[C0_WIRED]) {
                this.c0regs[C0_RANDOM] = TLB_COUNT-1;
            }
            this.fault_type = MFault.NONE; // prevent the next instr from firing here
        }
        */
    }

    // API requirements

    public void reset() {
        synchronized(lock) {
            this.pc = VECTOR_BASE_RESET;
            this.pcNext = this.pc + 4;
            this.pcAfter = this.pcNext + 4;
            this.pcBranchDelay = false;

            // Extra stuff that doesn't normally get covered
            for(int i = 0; i < 32; i++) {
                this.regs[i] = 0L;
            }
            for(int i = 0; i < 32; i++) {
                this.c0regs[i] = 0L;
            }
            for(int i = 0; i < ICACHE_SIZE_TAGS; i++) {
                this.iCacheTags[i] = 0;
            }
            for(int i = 0; i < DCACHE_SIZE_TAGS; i++) {
                this.dCacheTags[i] = 0;
            }

            this.c0regs[C0_STATUS] = (long)(int)0x040000E0;
            this.c0regs[C0_CAUSE] = (long)(int)0x00000000;
            this.c0regs[C0_CONFIG] = (long)(int)0x1002649B;

            // However, this DOES happen during a reset!
            this.c0regs[C0_STATUS] |= (long)(int)0x00400004;
            this.c0regs[C0_RANDOM] = TLB_COUNT-1;

            // This is also enforced anyway
            this.c0regs[C0_PRID] = (long)(int)0x00002000; // R4600?

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

            long timeBeg = System.currentTimeMillis();
            while(this.cycleBudget > 0) {
                //System.out.printf("PC = %016X / v0 = %016X\n", this.pc, this.regs[2]);
                runOp();
            }
            long timeEnd = System.currentTimeMillis();
            if(false) {
                benchAccum += (timeEnd-timeBeg);
                benchTicksLeft--;
                if(benchTicksLeft <= 0) {
                    double cpuLoad = benchAccum/1000.0;
                    System.out.printf("[LOAD: %5.3f]\n", cpuLoad);
                    benchAccum = 0;
                    benchTicksLeft = 20;
                }
            }
        }
    }
}
