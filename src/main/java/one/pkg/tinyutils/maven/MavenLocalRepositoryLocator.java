package one.pkg.tinyutils.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenLocalRepositoryLocator {
    private static final String SETTINGS_FILE_NAME = "settings.xml";
    private static final String M2_SETTINGS_DIR = ".m2";

    public static String getLocalRepositoryPath() {
        String repoPath = getRepositoryFromUserSettings();
        if (repoPath != null && isValidDirectory(repoPath)) {
            return repoPath;
        }

        repoPath = getRepositoryFromGlobalSettings();
        if (repoPath != null && isValidDirectory(repoPath)) {
            return repoPath;
        }

        return getDefaultRepositoryPath();
    }

    private static String getRepositoryFromUserSettings() {
        String userHome = System.getProperty("user.home");
        Path settingsPath = Paths.get(userHome, M2_SETTINGS_DIR, SETTINGS_FILE_NAME);

        if (Files.exists(settingsPath)) {
            return parseLocalRepositoryFromSettings(settingsPath.toFile());
        }

        return null;
    }

    private static String getRepositoryFromGlobalSettings() {
        String mavenHome = getMavenHome();

        if (mavenHome != null) {
            Path settingsPath = Paths.get(mavenHome, "conf", SETTINGS_FILE_NAME);
            if (Files.exists(settingsPath)) {
                return parseLocalRepositoryFromSettings(settingsPath.toFile());
            }
        }

        return null;
    }

    private static String getMavenHome() {
        String mavenHome = System.getenv("M2_HOME");
        if (mavenHome != null && !mavenHome.isEmpty()) {
            return mavenHome;
        }

        mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null && !mavenHome.isEmpty()) {
            return mavenHome;
        }

        return null;
    }

    private static String parseLocalRepositoryFromSettings(File settingsFile) {
        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(fis);

            document.getDocumentElement().normalize();

            NodeList nodeList = document.getElementsByTagName("localRepository");

            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String repoPath = node.getTextContent().trim();

                    repoPath = expandPath(repoPath);

                    return repoPath;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing settings.xml file: " + e.getMessage());
        }

        return null;
    }

    private static String expandPath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        path = path.replace("${user.home}", System.getProperty("user.home"));
        path = path.replace("${user.dir}", System.getProperty("user.dir"));
        if (path.contains("${env.")) {
            int startIndex = path.indexOf("${env.");
            while (startIndex != -1) {
                int endIndex = path.indexOf("}", startIndex);
                if (endIndex != -1) {
                    String envVarName = path.substring(startIndex + 6, endIndex);
                    String envValue = System.getenv(envVarName);
                    if (envValue != null) {
                        path = path.replace("${env." + envVarName + "}", envValue);
                    }
                }
                startIndex = path.indexOf("${env.", endIndex);
            }
        }

        if (path.startsWith("~")) {
            path = path.replaceFirst("^~", System.getProperty("user.home"));
        }

        return path;
    }

    private static String getDefaultRepositoryPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, M2_SETTINGS_DIR, "repository").toString();
    }

    private static boolean isValidDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        File dir = new File(path);
        return dir.exists() && dir.isDirectory();
    }

    public static File getLocalRepositoryFile() {
        return new File(getLocalRepositoryPath());
    }

    public static boolean isLocalRepositoryExists() {
        return getLocalRepositoryFile().exists();
    }

    public static RepositoryInfo getRepositoryInfo() {
        File repo = getLocalRepositoryFile();
        boolean exists = repo.exists();
        boolean canRead = exists && repo.canRead();
        boolean canWrite = exists && repo.canWrite();
        long size = exists ? calculateDirectorySize(repo) : 0;

        return new RepositoryInfo(repo.getAbsolutePath(), exists, canRead, canWrite, size);
    }

    private static long calculateDirectorySize(File directory) {
        long size = 0;

        if (directory.isFile()) {
            return directory.length();
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }

        return size;
    }

    public record RepositoryInfo(String absolutePath, boolean exists, boolean read, boolean write, long size) {
        public String formatSize() {
            if (!exists) return "";

            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.2f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", size / (1024.0 * 1024));
            } else {
                return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
            }
        }
    }
}