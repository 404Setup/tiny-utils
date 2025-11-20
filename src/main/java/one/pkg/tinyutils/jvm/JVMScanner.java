package one.pkg.tinyutils.jvm;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// TODO
public class JVMScanner {
    public List<JVMInfo> scanSystem() {
        List<JVMInfo> result = new ArrayList<>();
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();

        for (VirtualMachineDescriptor descriptor : vms) {
            try {
                VirtualMachine vm = VirtualMachine.attach(descriptor);
                Properties props = vm.getSystemProperties();

                JVMInfo info = new JVMInfo(
                        descriptor.id(),
                        props.getProperty("java.version"),
                        props.getProperty("java.vendor"),
                        Boolean.parseBoolean(props.getProperty("java.awt.headless", "false"))
                );

                result.add(info);
                vm.detach();
            } catch (Exception e) {
                // Skip JVMs that cannot be attached to
            }
        }

        return result;
    }

    public List<JVMInfo> scanPath(String path) {
        List<JVMInfo> result = new ArrayList<>();
        File directory = new File(path);

        if (!directory.exists() || !directory.isDirectory()) {
            return result;
        }

        String pid;
        try {
            for (File file : directory.listFiles()) {
                if (file.getName().endsWith(".java") || file.getName().endsWith(".class")) {
                    ProcessBuilder pb = new ProcessBuilder("jps", "-l");
                    Process p = pb.start();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream())
                    );
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(file.getName())) {
                            pid = line.split(" ")[0];
                            try {
                                VirtualMachine vm = VirtualMachine.attach(pid);
                                Properties props = vm.getSystemProperties();

                                JVMInfo info = new JVMInfo(
                                        pid,
                                        props.getProperty("java.version"),
                                        props.getProperty("java.vendor"),
                                        Boolean.parseBoolean(props.getProperty("java.awt.headless", "false"))
                                );

                                result.add(info);
                                vm.detach();
                            } catch (Exception e) {
                                // Skip JVMs that cannot be attached to
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Handle exceptions during path scanning
        }

        return result;
    }
}
