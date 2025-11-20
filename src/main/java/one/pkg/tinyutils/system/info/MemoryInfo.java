package one.pkg.tinyutils.system.info;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

public record MemoryInfo(long current, long total) {
    public static MemoryInfo getSystemInstance() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        return new MemoryInfo(totalMemory - freeMemory, totalMemory);
    }

    public static MemoryInfo getJvmInstance() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        return new MemoryInfo(total - free, total);
    }

    public String format() {
        Type type = Type.MB;
        for (Type t : Type.VALUES) {
            if (total / t.bytes < 1024L) {
                type = t;
                break;
            }
        }
        return formatCurrent(type) + " / " + formatFull(type);
    }

    public String formatFull(Type type) {
        return String.format("%.2f %s", (double) total / type.bytes, type.name());
    }

    public long full(Type type) {
        return total / type.bytes;
    }

    public String formatCurrent(Type type) {
        return String.format("%.2f %s", (double) current / type.bytes, type.name());
    }

    public long current(Type type) {
        return current / type.bytes;
    }

    public double getUsagePercentage() {
        return ((double) current / total) * 100;
    }

    public enum Type {
        KB(1024),
        MB(1024 * 1024),
        GB(1024 * 1024 * 1024),
        TB(1024L * 1024 * 1024 * 1024),
        PB(1024L * 1024 * 1024 * 1024 * 1024);

        public static final Type[] VALUES = values();
        private final long bytes;

        Type(long bytes) {
            this.bytes = bytes;
        }
    }
}