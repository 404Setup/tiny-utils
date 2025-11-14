package one.pkg.tinyutils.system;

import oshi.SystemInfo;
import oshi.hardware.*;

import java.util.List;
import java.util.stream.Collectors;

// To run this class, you need to install Oshi Core 6.9.1 first.
public class HardwareIdReader {
    private final HardwareAbstractionLayer hardware = new SystemInfo().getHardware();

    public String getMotherboardId() {
        ComputerSystem computerSystem = hardware.getComputerSystem();
        Baseboard baseboard = computerSystem.getBaseboard();

        String serialNumber = baseboard.getSerialNumber();
        if (serialNumber == null || serialNumber.trim().isEmpty() ||
                "Unknown".equalsIgnoreCase(serialNumber)) {
            return String.format("%s-%s-%s",
                    baseboard.getManufacturer(),
                    baseboard.getModel(),
                    baseboard.getVersion()
            );
        }
        return serialNumber;
    }

    public String getDiskId() {
        List<HWDiskStore> diskStores = hardware.getDiskStores();

        List<String> diskIds = diskStores.stream()
                .map(disk -> {
                    String serial = disk.getSerial();
                    if (serial == null || serial.trim().isEmpty() ||
                            "Unknown".equalsIgnoreCase(serial)) {

                        return disk.getModel() + "-" + disk.getSize();
                    }
                    return serial;
                })
                .collect(Collectors.toList());

        return String.join(",", diskIds);
    }

    public String getSystemId() {
        ComputerSystem computerSystem = hardware.getComputerSystem();

        String uuid = computerSystem.getHardwareUUID();
        if (uuid != null && !uuid.trim().isEmpty() &&
                !"Unknown".equalsIgnoreCase(uuid)) {
            return uuid;
        }

        return String.format("%s-%s-%s",
                computerSystem.getManufacturer(),
                computerSystem.getModel(),
                computerSystem.getSerialNumber()
        );
    }

    public String getMemoryId() {
        List<PhysicalMemory> physicalMemories = hardware.getMemory().getPhysicalMemory();

        if (physicalMemories.isEmpty()) {
            return String.valueOf(hardware.getMemory().getTotal());
        }

        List<String> memoryIds = physicalMemories.stream()
                .map(memory -> {
                    String serial = memory.getSerialNumber();
                    if (serial == null || serial.trim().isEmpty() ||
                            "Unknown".equalsIgnoreCase(serial)) {

                        return String.format("%s-%d-%d",
                                memory.getManufacturer(),
                                memory.getCapacity(),
                                memory.getClockSpeed()
                        );
                    }
                    return serial;
                })
                .collect(Collectors.toList());

        return String.join(",", memoryIds);
    }

    public String getHardwareFingerprint() {
        return String.format("MB:%s|DISK:%s|SYS:%s|MEM:%s",
                getMotherboardId(),
                getDiskId(),
                getSystemId(),
                getMemoryId()
        );
    }
}