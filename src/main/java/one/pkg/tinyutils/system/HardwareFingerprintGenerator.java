package one.pkg.tinyutils.system;

import one.pkg.tinyutils.data.Hash;

public class HardwareFingerprintGenerator {
    public static String generateFingerprint() {
        HardwareIdReader reader = new HardwareIdReader();
        String rawFingerprint = reader.getHardwareFingerprint();

        return Hash.format(Hash.hash(rawFingerprint));
    }
}