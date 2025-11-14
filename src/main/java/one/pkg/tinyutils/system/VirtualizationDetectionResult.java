package one.pkg.tinyutils.system;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record VirtualizationDetectionResult(VirtualizationType type, boolean isVirtualized, Map<String, String> details,
                                            List<String> detectionMethods) {
    @Override
    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Virtualization Type: ").append(type).append("\n");
        sb.append("Is Virtualized: ").append(isVirtualized ? "Yes" : "No").append("\n");
        sb.append("Detection Methods: ").append(String.join(", ", detectionMethods)).append("\n");
        if (!details.isEmpty()) {
            sb.append("Details:\n");
            details.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        return sb.toString();
    }
}
