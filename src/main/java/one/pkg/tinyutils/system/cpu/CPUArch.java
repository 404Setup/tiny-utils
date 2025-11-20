package one.pkg.tinyutils.system.cpu;

import java.util.Locale;

public enum CPUArch {
    AARCH, X86, Mips, RiscV, PowerPC;

    private static final CPUArch CPU_ARCH = detectCPUArch();

    private static CPUArch detectCPUArch() {
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ENGLISH);

        if (osArch.contains("aarch") || osArch.contains("arm")) return CPUArch.AARCH;
        if (osArch.contains("x86") || osArch.contains("amd64") || osArch.contains("i386") ||
                osArch.contains("i486") || osArch.contains("i586") || osArch.contains("i686")) return CPUArch.X86;
        if (osArch.contains("mips")) return CPUArch.Mips;
        if (osArch.contains("riscv")) return CPUArch.RiscV;
        if (osArch.contains("ppc") || osArch.contains("powerpc")) return CPUArch.PowerPC;

        return CPUArch.X86;
    }

    public static CPUArch getInstance() {
        return CPU_ARCH;
    }

    public static boolean is(CPUArch arch) {
        return getInstance().equals(arch);
    }
}
