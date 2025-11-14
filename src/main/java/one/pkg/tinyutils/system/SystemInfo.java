package one.pkg.tinyutils.system;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum SystemInfo {
    Windows, Linux, Android, Darwin, BSD, Fuchsia, Ohos, None;

    private static final STDLIB[] STDLIBS;
    private static final SystemInfo INFO;
    private static final CPUArch CPU_ARCH;
    private static final CPUType CPU_Type;

    static {
        INFO = detectOS();
        CPU_ARCH = detectCPUArch();
        CPU_Type = detectCPUType();
        STDLIBS = detectStdLib();
    }

    public static SystemInfo getInstance() {
        return INFO;
    }

    public static boolean is(SystemInfo info) {
        return getInstance().equals(info);
    }

    private static SystemInfo detectOS() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);

        if (isAndroid()) return Android;
        if (isOhos()) return Ohos;
        if (osName.contains("win")) return Windows;
        if (osName.contains("mac") || osName.contains("darwin")) return Darwin;
        if (isBSD(osName)) return BSD;
        if (osName.contains("fuchsia")) return Fuchsia;
        if (osName.contains("linux") || osName.contains("nix") || osName.contains("nux")) return Linux;
        return None;
    }

    private static boolean isAndroid() {
        try {
            String javaVendor = System.getProperty("java.vendor", "").toLowerCase(Locale.ENGLISH);
            String javaVmVendor = System.getProperty("java.vm.vendor", "").toLowerCase(Locale.ENGLISH);
            String javaVmName = System.getProperty("java.vm.name", "").toLowerCase(Locale.ENGLISH);

            if (javaVendor.contains("android") || javaVmVendor.contains("android") || javaVmName.contains("dalvik"))
                return true;

            try {
                Class.forName("android.os.Build");
                return true;
            } catch (ClassNotFoundException ignored) {
            }

            String javaRuntime = System.getProperty("java.runtime.name", "").toLowerCase(Locale.ENGLISH);
            if (javaRuntime.contains("android")) {
                return true;
            }

            if (new File("/system/bin/app_process").exists() ||
                    new File("/system/bin/app_process32").exists() ||
                    new File("/system/bin/app_process64").exists()) {
                return true;
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isOhos() {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
            if (osName.contains("ohos") || osName.contains("harmonyos") || osName.contains("harmony")) {
                return true;
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isBSD(String osName) {
        return osName.contains("bsd") ||
                osName.contains("freebsd") ||
                osName.contains("openbsd") ||
                osName.contains("netbsd") ||
                osName.contains("dragonfly");
    }

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

    private static STDLIB[] detectStdLib() {
        List<STDLIB> stdlibs = new ArrayList<>();

        SystemInfo os = INFO != null ? INFO : detectOS();

        if (os == Windows && hasMSVC()) {
            stdlibs.add(STDLIB.MSVC);
            return stdlibs.toArray(new STDLIB[0]);
        }
        if (os == Android) stdlibs.add(STDLIB.Bionic);
        if (os == Darwin && hasLibSystem()) stdlibs.add(STDLIB.LibSystem);

        if (hasGLibC()) stdlibs.add(STDLIB.GNU);
        if (hasMusl()) stdlibs.add(STDLIB.Musl);
        if (hasUClibc()) stdlibs.add(STDLIB.UClibc);
        if (hasNewLib()) stdlibs.add(STDLIB.NewLib);

        return stdlibs.toArray(new STDLIB[0]);
    }

    private static boolean hasMSVC() {
        try {
            String systemRoot = System.getenv("SystemRoot");
            if (systemRoot != null) {
                File system32 = new File(systemRoot, "System32");
                if (new File(system32, "msvcrt.dll").exists() ||
                        new File(system32, "msvcp140.dll").exists() ||
                        new File(system32, "vcruntime140.dll").exists() ||
                        new File(system32, "ucrtbase.dll").exists()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean hasGLibC() {
        try {
            /*Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ldd --version 2>&1"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase(Locale.ENGLISH).contains("glibc") ||
                        line.toLowerCase(Locale.ENGLISH).contains("gnu libc")) {
                    reader.close();
                    return true;
                }
            }
            reader.close();*/

            if (Files.exists(Paths.get("/lib/libc.so.6")) ||
                    Files.exists(Paths.get("/lib64/libc.so.6")) ||
                    Files.exists(Paths.get("/usr/lib/libc.so.6")) ||
                    Files.exists(Paths.get("/usr/lib64/libc.so.6"))) {
                return true;
            }

            /*process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "getconf GNU_LIBC_VERSION 2>&1"});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (reader.readLine() != null) {
                reader.close();
                return true;
            }
            reader.close();*/

        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean hasMusl() {
        try {
            /*Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ldd --version 2>&1 | head -1"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line != null && line.toLowerCase(Locale.ENGLISH).contains("musl")) {
                return true;
            }*/

            File libDir = new File("/lib");
            if (libDir.exists() && libDir.isDirectory()) {
                File[] files = libDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().contains("ld-musl") || file.getName().contains("libc.musl")) {
                            return true;
                        }
                    }
                }
            }

            if (Files.exists(Paths.get("/etc/alpine-release"))) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean hasBionic() {
        try {
            if (Files.exists(Paths.get("/system/lib/libc.so")) ||
                    Files.exists(Paths.get("/system/lib64/libc.so")) ||
                    Files.exists(Paths.get("/apex/com.android.runtime/lib/bionic/libc.so")) ||
                    Files.exists(Paths.get("/apex/com.android.runtime/lib64/bionic/libc.so"))) {
                return true;
            }

            if (Files.exists(Paths.get("/system/bin/linker")) ||
                    Files.exists(Paths.get("/system/bin/linker64"))) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean hasLibSystem() {
        try {
            if (Files.exists(Paths.get("/usr/lib/libSystem.dylib")) ||
                    Files.exists(Paths.get("/usr/lib/libSystem.B.dylib")) ||
                    Files.exists(Paths.get("/usr/lib/libc.dylib"))) {
                return true;
            }

            if (Files.exists(Paths.get("/usr/lib/dyld")))
                return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean hasUClibc() {
        try {
            /*Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ldd --version 2>&1 | head -1"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line != null && line.toLowerCase(Locale.ENGLISH).contains("uclibc")) {
                return true;
            }*/

            if (Files.exists(Paths.get("/lib/ld-uClibc.so.0")) ||
                    Files.exists(Paths.get("/lib/libc.so.0"))) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean hasNewLib() {
        try {
            if (Files.exists(Paths.get("/usr/include/newlib.h")) ||
                    Files.exists(Paths.get("/usr/local/include/newlib.h"))) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public enum CPUArch {
        AARCH, X86, Mips, RiscV, PowerPC;

        public static CPUArch getInstance() {
            return CPU_ARCH;
        }

        public static boolean is(CPUArch arch) {
            return getInstance().equals(arch);
        }
    }

    public enum CPUType {
        x32, x64, none;

        public static CPUType getInstance() {
            return CPU_Type;
        }

        public static boolean is(CPUType cpuType) {
            return getInstance().equals(cpuType);
        }
    }

    public enum STDLIB {
        Musl, MSVC, GNU, Bionic, LibSystem, UClibc, NewLib;

        public static STDLIB[] getInstances() {
            return STDLIBS;
        }

        public static boolean is(STDLIB stdlib) {
            for (STDLIB lib : getInstances()) {
                if (lib.equals(stdlib)) return true;
            }
            return false;
        }
    }
}
