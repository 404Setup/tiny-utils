import java.util.UUID;
import one.pkg.tinyutils.parse.uuid.UUIDParser;

public class BenchmarkUUID {
    public static void main(String[] args) {
        String uuidStr = UUID.randomUUID().toString();

        long t0 = System.nanoTime();
        for (int i=0; i<1000000; i++) {
            uuidStr.replace("-", "").toLowerCase();
        }
        long t1 = System.nanoTime();
        System.out.println("Old replace/lower (ms): " + (t1 - t0) / 1_000_000);

        long t2 = System.nanoTime();
        for (int i=0; i<1000000; i++) {
            UUIDParser.removeDashes(uuidStr);
        }
        long t3 = System.nanoTime();
        System.out.println("New UUIDParser (ms): " + (t3 - t2) / 1_000_000);
    }
}
