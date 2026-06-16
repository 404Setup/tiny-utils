package one.pkg.tinyutils.minecraft;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Command {
    // Bolt: Optimization - Precompile regex pattern to avoid repeated compilation overhead in getPermission
    private static final Pattern PERMISSION_SUFFIX_PATTERN = Pattern.compile("\\.([^.]+)$");

    public static String getName(String name) {
        String n = name;
        if (Platform.get() == Platform.BungeeCord) {
            n = "b" + n;
        } else if (Platform.get() == Platform.Velocity) {
            n = "v" + n;
        }
        return n;
    }

    public static String getPermission(String permission) {
        String p = permission;
        if (Platform.get() == Platform.BungeeCord) {
            p = PERMISSION_SUFFIX_PATTERN.matcher(permission).replaceFirst(".b$1");
        } else if (Platform.get() == Platform.Velocity) {
            p = PERMISSION_SUFFIX_PATTERN.matcher(permission).replaceFirst(".v$1");
        }

        return p;
    }
}
