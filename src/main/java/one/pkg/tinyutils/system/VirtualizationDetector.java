
package one.pkg.tinyutils.system;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class VirtualizationDetector {
    private static final int MAX_FILE_SIZE = 1024 * 1024;

    public static VirtualizationDetectionResult detect() {
        List<String> methods = new ArrayList<>();
        Map<String, String> details = new HashMap<>();

        VirtualizationType type = detectContainer(methods, details);
        if (type != VirtualizationType.UNKNOWN) {
            return new VirtualizationDetectionResult(type, true, details, methods);
        }

        type = detectWSL(methods, details);
        if (type != VirtualizationType.UNKNOWN) {
            return new VirtualizationDetectionResult(type, true, details, methods);
        }

        type = detectVM(methods, details);
        if (type != VirtualizationType.UNKNOWN) {
            return new VirtualizationDetectionResult(type, true, details, methods);
        }

        type = detectCloud(methods, details);
        if (type != VirtualizationType.UNKNOWN) {
            return new VirtualizationDetectionResult(type, true, details, methods);
        }

        methods.add("No virtualization detected");
        return new VirtualizationDetectionResult(VirtualizationType.PHYSICAL, false, details, methods);
    }

    private static VirtualizationType detectContainer(List<String> methods, Map<String, String> details) {
        if (fileExists("/.dockerenv")) {
            methods.add("Found /.dockerenv");
            details.put("type", "Docker");
            return VirtualizationType.DOCKER;
        }

        String cgroup = readFile("/proc/1/cgroup");
        if (cgroup != null) {
            if (cgroup.contains("docker")) {
                methods.add("Docker in cgroup");
                details.put("type", "Docker");
                return VirtualizationType.DOCKER;
            }
            if (cgroup.contains("kubepods") || cgroup.contains("kube")) {
                methods.add("Kubernetes in cgroup");
                details.put("type", "Kubernetes");
                return VirtualizationType.KUBERNETES;
            }
            if (cgroup.contains("lxc")) {
                methods.add("LXC in cgroup");
                return VirtualizationType.LXC;
            }
        }

        String mountinfo = readFile("/proc/self/mountinfo");
        if (mountinfo != null) {
            if (mountinfo.contains("/docker/")) {
                methods.add("Docker in mountinfo");
                return VirtualizationType.DOCKER;
            }
            if (mountinfo.contains("/kubepods/")) {
                methods.add("Kubernetes in mountinfo");
                return VirtualizationType.KUBERNETES;
            }
        }

        String k8sHost = System.getenv("KUBERNETES_SERVICE_HOST");
        if (k8sHost != null) {
            methods.add("K8S env variable");
            details.put("k8s_host", k8sHost);
            return VirtualizationType.KUBERNETES;
        }

        String container = System.getenv("container");
        if ("podman".equals(container)) {
            methods.add("Podman env");
            return VirtualizationType.PODMAN;
        }

        return VirtualizationType.UNKNOWN;
    }

    private static VirtualizationType detectWSL(List<String> methods, Map<String, String> details) {
        String version = readFile("/proc/version");
        if (version != null) {
            String lower = version.toLowerCase();
            if (lower.contains("microsoft") || lower.contains("wsl")) {
                methods.add("WSL in /proc/version");
                if (lower.contains("wsl2") || lower.contains("microsoft-standard")) {
                    return VirtualizationType.WSL2;
                }
                return VirtualizationType.WSL;
            }
        }

        if (System.getenv("WSL_DISTRO_NAME") != null) {
            methods.add("WSL_DISTRO_NAME env");
            details.put("distro", System.getenv("WSL_DISTRO_NAME"));
            return version != null && version.toLowerCase().contains("microsoft-standard")
                    ? VirtualizationType.WSL2 : VirtualizationType.WSL;
        }

        if (fileExists("/proc/sys/fs/binfmt_misc/WSLInterop")) {
            methods.add("WSLInterop found");
            return VirtualizationType.WSL2;
        }

        return VirtualizationType.UNKNOWN;
    }

    private static VirtualizationType detectVM(List<String> methods, Map<String, String> details) {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("linux")) {
            return checkLinuxVM(methods, details);
        } else if (os.contains("windows")) {
            return checkWindowsVM(methods, details);
        } else if (os.contains("mac")) {
            return checkMacVM(methods, details);
        }

        return VirtualizationType.UNKNOWN;
    }

    private static VirtualizationType checkLinuxVM(List<String> methods, Map<String, String> details) {
        String product = readFile("/sys/class/dmi/id/product_name");
        String bios = readFile("/sys/class/dmi/id/bios_vendor");
        String vendor = readFile("/sys/class/dmi/id/sys_vendor");
        String board = readFile("/sys/class/dmi/id/board_vendor");

        StringBuilder sb = new StringBuilder();
        if (product != null) sb.append(product).append(" ");
        if (bios != null) sb.append(bios).append(" ");
        if (vendor != null) sb.append(vendor).append(" ");
        if (board != null) sb.append(board).append(" ");

        String dmi = sb.toString().toLowerCase();
        if (!dmi.isEmpty()) {
            if (dmi.contains("vmware")) {
                methods.add("VMware in DMI");
                details.put("product", product);
                return VirtualizationType.VMWARE;
            }
            if (dmi.contains("virtualbox") || dmi.contains("vbox")) {
                methods.add("VirtualBox in DMI");
                details.put("product", product);
                return VirtualizationType.VIRTUALBOX;
            }
            if (dmi.contains("kvm") || dmi.contains("qemu")) {
                methods.add("KVM/QEMU in DMI");
                details.put("product", product);
                return VirtualizationType.KVM;
            }
            if (dmi.contains("xen")) {
                methods.add("Xen in DMI");
                return VirtualizationType.XEN;
            }
            if ((dmi.contains("microsoft") && dmi.contains("virtual")) || dmi.contains("hyper-v")) {
                methods.add("Hyper-V in DMI");
                return VirtualizationType.HYPER_V;
            }
            if (dmi.contains("parallels")) {
                methods.add("Parallels in DMI");
                return VirtualizationType.PARALLELS;
            }
        }

        String cpuinfo = readFile("/proc/cpuinfo");
        if (cpuinfo != null) {
            String cpu = cpuinfo.toLowerCase();
            if (cpu.contains("vmware")) {
                methods.add("VMware in CPU");
                return VirtualizationType.VMWARE;
            }
            if (cpu.contains("kvm")) {
                methods.add("KVM in CPU");
                return VirtualizationType.KVM;
            }
            if (cpu.contains("qemu")) {
                methods.add("QEMU in CPU");
                return VirtualizationType.QEMU;
            }
            if (cpu.contains("hypervisor")) {
                methods.add("Hypervisor flag");
                details.put("hypervisor", "detected");
            }
        }

        if (fileExists("/dev/vboxguest") || fileExists("/dev/vboxuser")) {
            methods.add("VBox device");
            return VirtualizationType.VIRTUALBOX;
        }

        if (fileExists("/proc/xen")) {
            methods.add("Xen proc");
            return VirtualizationType.XEN;
        }

        String modules = readFile("/proc/modules");
        if (modules != null) {
            if (modules.contains("vmw_") || modules.contains("vmwgfx") || modules.contains("vmxnet")) {
                methods.add("VMware module");
                return VirtualizationType.VMWARE;
            }
            if (modules.contains("vboxguest") || modules.contains("vboxsf")) {
                methods.add("VBox module");
                return VirtualizationType.VIRTUALBOX;
            }
        }

        String scsi = readFile("/proc/scsi/scsi");
        if (scsi != null) {
            String s = scsi.toLowerCase();
            if (s.contains("vmware")) {
                methods.add("VMware SCSI");
                return VirtualizationType.VMWARE;
            }
            if (s.contains("vbox")) {
                methods.add("VBox SCSI");
                return VirtualizationType.VIRTUALBOX;
            }
            if (s.contains("qemu")) {
                methods.add("QEMU SCSI");
                return VirtualizationType.QEMU;
            }
        }

        return checkNetworkMAC(methods, details);
    }

    private static VirtualizationType checkNetworkMAC(List<String> methods, Map<String, String> details) {
        try {
            File netDir = new File("/sys/class/net");
            if (!netDir.exists() || !netDir.isDirectory()) {
                return VirtualizationType.UNKNOWN;
            }

            String[] ifaces = netDir.list();
            if (ifaces == null) return VirtualizationType.UNKNOWN;

            for (String iface : ifaces) {
                String mac = readFile("/sys/class/net/" + iface + "/address");
                if (mac == null) continue;

                String m = mac.toLowerCase();
                if (m.startsWith("00:05:69") || m.startsWith("00:0c:29") || m.startsWith("00:50:56")) {
                    methods.add("VMware MAC");
                    details.put("mac", mac);
                    return VirtualizationType.VMWARE;
                }
                if (m.startsWith("08:00:27")) {
                    methods.add("VBox MAC");
                    details.put("mac", mac);
                    return VirtualizationType.VIRTUALBOX;
                }
            }
        } catch (Exception ignored) {
        }
        return VirtualizationType.UNKNOWN;
    }

    private static VirtualizationType checkWindowsVM(List<String> methods, Map<String, String> details) {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> e : env.entrySet()) {
            String val = e.getValue().toLowerCase();
            if (val.contains("vmware")) {
                methods.add("VMware env: " + e.getKey());
                return VirtualizationType.VMWARE;
            }
            if (val.contains("virtualbox") || val.contains("vbox")) {
                methods.add("VBox env: " + e.getKey());
                return VirtualizationType.VIRTUALBOX;
            }
            if (val.contains("parallels")) {
                methods.add("Parallels env: " + e.getKey());
                return VirtualizationType.PARALLELS;
            }
        }

        String[] vmwarePaths = {
                "C:\\Program Files\\VMware\\VMware Tools",
                "C:\\Windows\\System32\\drivers\\vmmouse.sys",
                "C:\\Windows\\System32\\drivers\\vmhgfs.sys"
        };
        for (String path : vmwarePaths) {
            if (fileExists(path)) {
                methods.add("VMware path");
                return VirtualizationType.VMWARE;
            }
        }

        String[] vboxPaths = {
                "C:\\Program Files\\Oracle\\VirtualBox Guest Additions",
                "C:\\Windows\\System32\\drivers\\VBoxGuest.sys",
                "C:\\Windows\\System32\\drivers\\VBoxMouse.sys"
        };
        for (String path : vboxPaths) {
            if (fileExists(path)) {
                methods.add("VBox path");
                return VirtualizationType.VIRTUALBOX;
            }
        }

        if (fileExists("C:\\Windows\\System32\\drivers\\vmbus.sys")
                || fileExists("C:\\Windows\\System32\\drivers\\storvsc.sys")) {
            methods.add("Hyper-V driver");
            return VirtualizationType.HYPER_V;
        }

        return VirtualizationType.UNKNOWN;
    }

    private static VirtualizationType checkMacVM(List<String> methods, Map<String, String> details) {
        if (fileExists("/Library/Application Support/VMware Tools")) {
            methods.add("VMware Tools dir");
            return VirtualizationType.VMWARE;
        }
        if (fileExists("/Library/Preferences/Parallels")) {
            methods.add("Parallels dir");
            return VirtualizationType.PARALLELS;
        }

        String[] kexts = {
                "/Library/Extensions/VMwareGfx.kext",
                "/System/Library/Extensions/VMwareGfx.kext"
        };
        for (String kext : kexts) {
            if (fileExists(kext)) {
                methods.add("VMware kext");
                return VirtualizationType.VMWARE;
            }
        }

        return VirtualizationType.UNKNOWN;
    }

    private static VirtualizationType detectCloud(List<String> methods, Map<String, String> details) {
        String uuid = readFile("/sys/hypervisor/uuid");
        if (uuid != null) {
            String u = uuid.toLowerCase();
            if (u.startsWith("ec2")) {
                methods.add("AWS EC2 UUID");
                details.put("cloud", "AWS");
                return VirtualizationType.AWS;
            }
        }

        String productUUID = readFile("/sys/class/dmi/id/product_uuid");
        if (productUUID != null && productUUID.toLowerCase().startsWith("ec2")) {
            methods.add("AWS product UUID");
            return VirtualizationType.AWS;
        }

        String product = readFile("/sys/class/dmi/id/product_name");
        if (product != null && product.toLowerCase().contains("microsoft corporation")) {
            String asset = readFile("/sys/class/dmi/id/chassis_asset_tag");
            if (asset != null && asset.contains("7783-7084-3265-9085-8269-3286-77")) {
                methods.add("Azure asset tag");
                details.put("cloud", "Azure");
                return VirtualizationType.AZURE;
            }
        }

        String bios = readFile("/sys/class/dmi/id/bios_vendor");
        if (bios != null && bios.toLowerCase().contains("google")) {
            methods.add("Google BIOS");
            details.put("cloud", "GCP");
            return VirtualizationType.GCP;
        }

        String vendor = readFile("/sys/class/dmi/id/sys_vendor");
        if (vendor != null && vendor.toLowerCase().contains("alibaba")) {
            methods.add("Alibaba vendor");
            details.put("cloud", "Alibaba");
            return VirtualizationType.ALIBABA_CLOUD;
        }

        return VirtualizationType.UNKNOWN;
    }

    private static String readFile(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p) || !Files.isReadable(p)) {
                return null;
            }
            if (Files.size(p) > MAX_FILE_SIZE) {
                return null;
            }
            return Files.readString(p).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean fileExists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (Exception e) {
            return false;
        }
    }
}