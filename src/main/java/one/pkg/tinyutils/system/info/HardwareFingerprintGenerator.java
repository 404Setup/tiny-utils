package one.pkg.tinyutils.system.info;

import one.pkg.tinyutils.data.Hash;

public class HardwareFingerprintGenerator {
    public static String generateFingerprint() {
        HardwareReader reader = new HardwareReader();
        String rawFingerprint = reader.getHardwareFingerprint();

        return Hash.format(Hash.hash(rawFingerprint));
    }
}