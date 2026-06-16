package one.pkg.tinyutils.compress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ICompress {
    int BUFFER_SIZE = 1024;

    static void copyStream(InputStream is, OutputStream os) throws IOException {
        // Bolt: Optimization - Utilize Java 9+ native transferTo for significantly faster zero-copy stream operations
        is.transferTo(os);
    }

    default void decompress(InputStream is, OutputStream os) throws IOException {
        try (InputStream decompressStream = createDecompressStream(is)) {
            copyStream(decompressStream, os);
        }
    }

    default void compress(InputStream is, OutputStream os) throws IOException {
        try (OutputStream compressStream = createCompressStream(os)) {
            copyStream(is, compressStream);
        }
    }

    InputStream createDecompressStream(InputStream is) throws IOException;

    OutputStream createCompressStream(OutputStream os) throws IOException;

    String getFileExtension();
}
