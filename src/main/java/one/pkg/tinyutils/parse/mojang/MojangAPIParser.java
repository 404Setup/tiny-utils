package one.pkg.tinyutils.parse.mojang;

import com.google.gson.reflect.TypeToken;
import one.pkg.tinyutils.parse.json.JsonParser;
import one.pkg.tinyutils.parse.mojang.schemas.ProfileLookup;
import one.pkg.tinyutils.parse.uuid.UUIDParser;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class MojangAPIParser {
    private static final String BASE_URL = "https://api.minecraftservices.com/minecraft/profile/lookup";
    private static final String NAME_ENDPOINT = "/name/";
    private static final String BULK_ENDPOINT = "/bulk/byname";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String POST_METHOD = "POST";

    public static ProfileLookup lookupProfile(String name) throws IOException {
        return lookupProfile(Proxy.NO_PROXY, name);
    }

    public static ProfileLookup lookupProfile(Proxy proxy, String name) throws IOException {
        String url = BASE_URL + NAME_ENDPOINT + name;
        HttpURLConnection connection = createConnection(url, proxy);
        return JsonParser.requestAndParse(connection, ProfileLookup.class);
    }

    public static ProfileLookup lookupProfile(UUID uuid) throws IOException {
        return lookupProfile(Proxy.NO_PROXY, uuid);
    }

    public static ProfileLookup lookupProfile(Proxy proxy, UUID uuid) throws IOException {
        String url = BASE_URL + "/" + UUIDParser.removeDashes(uuid);
        HttpURLConnection connection = createConnection(url, proxy);
        return JsonParser.requestAndParse(connection, ProfileLookup.class);
    }

    public static List<ProfileLookup> lookupProfiles(String... names) throws IOException {
        return lookupProfiles(Proxy.NO_PROXY, names);
    }

    public static List<ProfileLookup> lookupProfiles(Proxy proxy, String... names) throws IOException {
        String url = BASE_URL + BULK_ENDPOINT;
        HttpURLConnection connection = createConnection(url, proxy);

        connection.setRequestMethod(POST_METHOD);
        connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
        connection.setDoOutput(true);

        String jsonBody = JsonParser.toJson(names);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        Type listType = new TypeToken<List<ProfileLookup>>() {
        }.getType();
        return JsonParser.requestAndParse(connection, listType);
    }

    private static HttpURLConnection createConnection(String url, Proxy proxy) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection(proxy);
    }
}