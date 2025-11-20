package one.pkg.tinyutils.system.cpu;

import java.util.Locale;

public enum CPUType {
    x32, x64, none;

    private static final CPUType CPU_Type = detectCPUType();

    public static CPUType getInstance() {
        return CPU_Type;
    }

    public static boolean is(CPUType cpuType) {
        return getInstance().equals(cpuType);
    }

    private static CPUType detectCPUType() {
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ENGLISH);

        if (osArch.contains("64") || osArch.contains("amd64") || osArch.contains("x86_64") ||
                osArch.contains("aarch64") || osArch.contains("arm64") || osArch.contains("ppc64") ||
                osArch.contains("riscv64") || osArch.contains("mips64"))
            return CPUType.x64;

        if (osArch.contains("86") || osArch.contains("32") || osArch.contains("arm") ||
                osArch.contains("i386") || osArch.contains("i486") || osArch.contains("i586") ||
                osArch.contains("i686") || osArch.contains("mips"))
            return CPUType.x32;

        String dataModel = System.getProperty("sun.arch.data.model", "");
        if ("64".equals(dataModel)) {
            return CPUType.x64;
        } else if ("32".equals(dataModel)) {
            return CPUType.x32;
        }

        return CPUType.none;
    }
}