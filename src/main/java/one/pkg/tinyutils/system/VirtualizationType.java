package one.pkg.tinyutils.system;

public enum VirtualizationType {
    VMWARE("VMware"),
    VIRTUALBOX("VirtualBox"),
    HYPER_V("Hyper-V"),
    KVM("KVM"),
    XEN("Xen"),
    QEMU("QEMU"),
    PARALLELS("Parallels"),

    DOCKER("Docker"),
    KUBERNETES("Kubernetes"),
    LXC("LXC"),
    PODMAN("Podman"),

    AWS("AWS"),
    AZURE("Azure"),
    GCP("GCP"),
    ALIBABA_CLOUD("Alibaba Cloud"),

    WSL("WSL"),
    WSL2("WSL2"),

    PHYSICAL("Physical"),

    UNKNOWN("Unknown");

    public final String name;

    VirtualizationType(String name) {
        this.name = name;
    }
}
