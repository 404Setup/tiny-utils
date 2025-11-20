package one.pkg.tinyutils.system.info;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// To run this class, you need to install Oshi Core 6.9.1 first.
public class HardwareReader {
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
    
    public List<PhysicalMemory> getPhysicalMemories() {
        return hardware.getMemory().getPhysicalMemory();
    }

    @Nullable
    public List<String> getMemoryIds() {
        List<PhysicalMemory> physicalMemories = getPhysicalMemories();

        if (physicalMemories.isEmpty()) {
            return null;
        }

        List<String> memoryIds = new ArrayList<>();
        for (PhysicalMemory memory : physicalMemories) {
            String serial = memory.getSerialNumber();
            if (serial == null || serial.trim().isEmpty() ||
                    "Unknown".equalsIgnoreCase(serial)) {

                memoryIds.add(String.format("%s-%d-%d",
                        memory.getManufacturer(),
                        memory.getCapacity(),
                        memory.getClockSpeed()
                ));
            } else {
                memoryIds.add(serial);
            }
        }

        return memoryIds;
    }

    @NotNull
    public String getMemoryId() {
        var memIds = getMemoryIds();
        if (memIds == null) return String.valueOf(hardware.getMemory().getTotal());
        return String.join(",", memIds);
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