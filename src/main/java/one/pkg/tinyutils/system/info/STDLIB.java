package one.pkg.tinyutils.system.info;

import one.pkg.tinyutils.system.OSPlatform;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static one.pkg.tinyutils.system.OSPlatform.*;

public enum STDLIB {
    Musl, MSVC, GNU, Bionic, LibSystem, UClibc, NewLib;

    private static final STDLIB[] STDLIBS = detectStdLib();

    public static STDLIB[] getInstances() {
        return STDLIBS;
    }

    public static boolean is(STDLIB stdlib) {
        for (STDLIB lib : getInstances()) {
            if (lib.equals(stdlib)) return true;
        }
        return false;
    }

    private static STDLIB[] detectStdLib() {
        List<STDLIB> stdlibs = new ArrayList<>();

        OSPlatform os = getInstance();

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
}