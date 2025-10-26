package one.pkg.tiny.utils.parse.json;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;

public class JsonParser {
    private static final Gson gson = new Gson();

    public static Gson gson() {
        return gson;
    }

    /**
     * Sends an HTTP request using the provided connection, reads the response stream, and parses it
     * into an object of the specified type.
     *
     * @param <T>        the type of the object to return after parsing
     * @param connection the {@link HttpURLConnection} instance used to send the request
     * @param typeOfT    the {@link Type} of the object to parse the response into
     * @return the parsed object of the specified type
     * @throws IOException if an error occurs while reading the response or closing resources
     */
    public static <T> T requestAndParse(HttpURLConnection connection, Type typeOfT) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
            return gson.fromJson(reader, typeOfT);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Sends a request using the given HttpURLConnection and parses the response into the specified class type.
     *
     * @param <T>        the type of the object to be returned
     * @param connection the HttpURLConnection instance used to perform the request
     * @param classOfT   the class of the object to parse the response into
     * @return an instance of the specified class type containing the parsed response
     * @throws IOException if an I/O error occurs while making the request or reading the response
     */
    @SuppressWarnings("all")
    public static <T> T requestAndParse(HttpURLConnection connection, Class<T> classOfT) throws IOException {
        return requestAndParse(connection, (Type) classOfT);
    }

    /**
     * Parses the JSON content from the provided {@link Reader} into an object of the specified type.
     *
     * @param <T>      the type of the object to be returned
     * @param json     the {@link Reader} containing the JSON content
     * @param classOfT the {@link Class} of the object to parse the JSON into
     * @return an instance of the specified class type populated with the parsed data
     * @throws JsonSyntaxException if the JSON is not in the expected format
     * @throws JsonIOException     if an error occurs while reading the JSON data
     */
    public static <T> T fromJson(@NotNull Reader json, @NotNull Class<T> classOfT)
            throws JsonSyntaxException, JsonIOException {
        return gson.fromJson(json, classOfT);
    }

    /**
     * Converts the given object into its JSON representation.
     *
     * @param src the object to be converted to JSON; can be null
     * @return a JSON-formatted string representing the given object or "null" if the input is null
     */
    public static String toJson(@Nullable Object src) {
        return gson.toJson(src);
    }
}
