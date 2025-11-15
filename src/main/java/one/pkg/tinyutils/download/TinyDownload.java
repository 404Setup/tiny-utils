package one.pkg.tinyutils.download;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("all")
public class TinyDownload {
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES = 3;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    public static boolean newTask(int threadCount, URI url, File outputFile, @Nullable Map<String, String> header) throws RuntimeException {
        return newTask(threadCount, url, outputFile, header, null);
    }

    public static boolean newTask(URI url, File outputFile, @Nullable Map<String, String> header) throws RuntimeException {
        return newTask(1, url, outputFile, header, null);
    }

    public static boolean newTask(String url, File outputFile, @Nullable Map<String, String> header) throws RuntimeException {
        return newTask(URI.create(url), outputFile, header, null);
    }

    public static boolean newTask(URI url, File outputFile) throws RuntimeException {
        return newTask(url, outputFile, null, null);
    }

    public static boolean newTask(String url, File outputFile) throws RuntimeException {
        return newTask(url, outputFile, null, null);
    }

    public static boolean newTask(int threadCount, URI url, File outputFile, @Nullable Map<String, String> header, @Nullable Proxy proxy) throws RuntimeException {
        try {
            return downloadWithRetry(threadCount, url, outputFile, header, proxy, MAX_RETRIES);
        } catch (Exception e) {
            throw new RuntimeException("Download failed: " + e.getMessage(), e);
        }
    }

    public static boolean newTask(URI url, File outputFile, @Nullable Map<String, String> header, @Nullable Proxy proxy) throws RuntimeException {
        return newTask(1, url, outputFile, header, proxy);
    }

    public static boolean newTask(String url, File outputFile, @Nullable Map<String, String> header, @Nullable Proxy proxy) throws RuntimeException {
        return newTask(URI.create(url), outputFile, header, proxy);
    }

    public static boolean newTask(URI url, File outputFile, @Nullable Proxy proxy) throws RuntimeException {
        return newTask(url, outputFile, null, proxy);
    }

    public static boolean newTask(String url, File outputFile, @Nullable Proxy proxy) throws RuntimeException {
        return newTask(url, outputFile, null, proxy);
    }

    public static boolean newTaskToStream(URI url, OutputStream outputStream, @Nullable Map<String, String> header)
            throws RuntimeException {
        return newTaskToStream(url, outputStream, header, null);
    }

    public static boolean newTaskToStream(String url, OutputStream outputStream, @Nullable Map<String, String> header)
            throws RuntimeException {
        return newTaskToStream(URI.create(url), outputStream, header, null);
    }

    public static boolean newTaskToStream(URI url, OutputStream outputStream) throws RuntimeException {
        return newTaskToStream(url, outputStream, null, null);
    }

    public static boolean newTaskToStream(String url, OutputStream outputStream) throws RuntimeException {
        return newTaskToStream(url, outputStream, null, null);
    }

    public static boolean newTaskToStream(URI url, OutputStream outputStream, @Nullable Map<String, String> header, @Nullable Proxy proxy)
            throws RuntimeException {
        try {
            return downloadToStreamWithRetry(url, outputStream, header, proxy, MAX_RETRIES);
        } catch (Exception e) {
            throw new RuntimeException("Download failed: " + e.getMessage(), e);
        }
    }

    public static boolean newTaskToStream(String url, OutputStream outputStream, @Nullable Map<String, String> header, @Nullable Proxy proxy)
            throws RuntimeException {
        return newTaskToStream(URI.create(url), outputStream, header, proxy);
    }

    public static boolean newTaskToStream(URI url, OutputStream outputStream, @Nullable Proxy proxy) throws RuntimeException {
        return newTaskToStream(url, outputStream, null, proxy);
    }

    public static boolean newTaskToStream(String url, OutputStream outputStream, @Nullable Proxy proxy) throws RuntimeException {
        return newTaskToStream(url, outputStream, null, proxy);
    }

    public static byte[] newTaskToBytes(URI url, @Nullable Map<String, String> header) throws RuntimeException {
        return newTaskToBytes(url, header, null);
    }

    public static byte[] newTaskToBytes(String url, @Nullable Map<String, String> header) throws RuntimeException {
        return newTaskToBytes(URI.create(url), header, null);
    }

    public static byte[] newTaskToBytes(URI url) throws RuntimeException {
        return newTaskToBytes(url, null, null);
    }

    public static byte[] newTaskToBytes(String url) throws RuntimeException {
        return newTaskToBytes(url, null, null);
    }

    public static byte[] newTaskToBytes(URI url, @Nullable Map<String, String> header, @Nullable Proxy proxy) throws RuntimeException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            newTaskToStream(url, baos, header, proxy);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Download failed: " + e.getMessage(), e);
        }
    }

    public static byte[] newTaskToBytes(String url, @Nullable Map<String, String> header, @Nullable Proxy proxy) throws RuntimeException {
        return newTaskToBytes(URI.create(url), header, proxy);
    }

    public static byte[] newTaskToBytes(URI url, @Nullable Proxy proxy) throws RuntimeException {
        return newTaskToBytes(url, null, proxy);
    }

    public static byte[] newTaskToBytes(String url, @Nullable Proxy proxy) throws RuntimeException {
        return newTaskToBytes(url, null, proxy);
    }

    private static boolean downloadWithRetry(int threadCount, URI url, File outputFile,
                                             @Nullable Map<String, String> header, @Nullable Proxy proxy, int retriesLeft) throws RuntimeException {
        try {
            return performDownload(threadCount, url, outputFile, header, proxy);
        } catch (Exception e) {
            if (retriesLeft > 0) {
                // retry
                return downloadWithRetry(threadCount, url, outputFile, header, proxy, retriesLeft - 1);
            } else {
                throw new RuntimeException("Download failed, maximum number of retries reached", e);
            }
        }
    }

    private static boolean downloadToStreamWithRetry(URI url, OutputStream outputStream,
                                                     @Nullable Map<String, String> header, @Nullable Proxy proxy, int retriesLeft) throws RuntimeException {
        try {
            return performDownloadToStream(url, outputStream, header, proxy);
        } catch (Exception e) {
            if (retriesLeft > 0) {
                // retry
                return downloadToStreamWithRetry(url, outputStream, header, proxy, retriesLeft - 1);
            } else {
                throw new RuntimeException("Download failed, maximum number of retries reached", e);
            }
        }
    }

    private static boolean performDownload(int threadCount, URI url, File outputFile,
                                           @Nullable Map<String, String> header, @Nullable Proxy proxy) throws Exception {
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        URL urlObj = url.toURL();
        HttpURLConnection connection = createConnection(urlObj, header, proxy);

        try {
            long contentLength = connection.getContentLengthLong();
            boolean supportRange = "bytes".equalsIgnoreCase(connection.getHeaderField("Accept-Ranges"));

            if (contentLength <= 0 || threadCount <= 1 || !supportRange) {
                return singleThreadDownload(urlObj, outputFile, header, proxy);
            } else {
                return multiThreadDownload(threadCount, urlObj, outputFile, header, proxy, contentLength);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean performDownloadToStream(URI url, OutputStream outputStream,
                                                   @Nullable Map<String, String> header, @Nullable Proxy proxy) throws Exception {
        URL urlObj = url.toURL();
        HttpURLConnection connection = createConnection(urlObj, header, proxy);

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            return true;
        } finally {
            connection.disconnect();
        }
    }

    private static boolean singleThreadDownload(URL url, File outputFile,
                                                @Nullable Map<String, String> header, @Nullable Proxy proxy) throws IOException {
        HttpURLConnection connection = createConnection(url, header, proxy);

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return true;
        } finally {
            connection.disconnect();
        }
    }

    private static boolean multiThreadDownload(int threadCount, URL url, File outputFile,
                                               @Nullable Map<String, String> header, @Nullable Proxy proxy, long contentLength) throws Exception {
        File tempDir = new File(outputFile.getParentFile(), outputFile.getName() + ".tmp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        long chunkSize = contentLength / threadCount;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicLong downloadedBytes = new AtomicLong(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                long start = i * chunkSize;
                long end = (i == threadCount - 1) ? contentLength - 1 : (start + chunkSize - 1);
                File tempFile = new File(tempDir, "part_" + i);

                DownloadTask task = new DownloadTask(url, header, proxy, start, end, tempFile, downloadedBytes);
                futures.add(executor.submit(task));
            }

            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    throw new RuntimeException("Download task failed");
                }
            }

            mergeFiles(tempDir, outputFile, threadCount);

            deleteDirectory(tempDir);

            return true;
        } finally {
            executor.shutdown();
        }
    }

    private static void mergeFiles(File tempDir, File outputFile, int threadCount) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            for (int i = 0; i < threadCount; i++) {
                File tempFile = new File(tempDir, "part_" + i);
                try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            directory.delete();
        }
    }

    private static HttpURLConnection createConnection(URL url, @Nullable Map<String, String> header, @Nullable Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (proxy != null ? url.openConnection(proxy) : url.openConnection());
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");

        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet())
                connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        return connection;
    }

    private record DownloadTask(URL url, @Nullable Map<String, String> header, @Nullable Proxy proxy, long start,
                                long end, File outputFile, AtomicLong downloadedBytes) implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            int retries = 0;
            Exception lastException = null;

            while (retries < MAX_RETRIES) {
                HttpURLConnection connection = null;
                try {
                    connection = createConnection(url, header, proxy);
                    connection.setRequestProperty("Range", "bytes=" + start + "-" + end);

                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Server returned error code: " + responseCode);
                    }

                    try (InputStream inputStream = connection.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            downloadedBytes.addAndGet(bytesRead);
                        }
                    }

                    return true;
                } catch (Exception e) {
                    lastException = e;
                    retries++;
                    if (retries < MAX_RETRIES) {
                        // retry
                        Thread.sleep(1000L * retries);
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            throw new RuntimeException("Download task failed, maximum number of retries reached", lastException);
        }
    }
}