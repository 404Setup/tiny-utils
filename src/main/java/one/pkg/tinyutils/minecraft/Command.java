package one.pkg.tinyutils.minecraft;

@SuppressWarnings("unused")
public class Command {

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
            int lastDot = p.lastIndexOf('.');
            if (lastDot != -1 && lastDot < p.length() - 1) {
                p = p.substring(0, lastDot) + ".b" + p.substring(lastDot + 1);
            }
        } else if (Platform.get() == Platform.Velocity) {
            int lastDot = p.lastIndexOf('.');
            if (lastDot != -1 && lastDot < p.length() - 1) {
                p = p.substring(0, lastDot) + ".v" + p.substring(lastDot + 1);
            }
        }

        return p;
    }
}
