package one.pkg.tinyutils.system;

import java.io.File;
import java.util.Locale;

public enum OSPlatform {
    Windows, Linux, Android, Darwin, BSD, Fuchsia, Ohos, None;

    private static final OSPlatform OS = detectOS();

    public static OSPlatform getInstance() {
        return OS;
    }

    public static boolean is(OSPlatform info) {
        return getInstance().equals(info);
    }

    private static OSPlatform detectOS() {
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
}
