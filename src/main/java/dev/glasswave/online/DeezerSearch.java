package dev.glasswave.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.glasswave.library.Track;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Catalog metadata and short preview lookup. This never downloads or saves full songs. */
public final class DeezerSearch {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private DeezerSearch() {
    }

    public static List<Track> search(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.deezer.com/search?q=" + encoded + "&limit=25");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Glasswave-Minecraft/0.1.0")
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Catalog returned HTTP " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (root.has("error")) {
            throw new IllegalStateException(root.getAsJsonObject("error").has("message")
                    ? root.getAsJsonObject("error").get("message").getAsString()
                    : "Catalog request was rejected");
        }

        JsonArray data = root.has("data") && root.get("data").isJsonArray()
                ? root.getAsJsonArray("data") : new JsonArray();
        List<Track> tracks = new ArrayList<>();
        for (JsonElement element : data) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            if (item.has("explicit_lyrics") && item.get("explicit_lyrics").getAsBoolean()) continue;
            String preview = string(item, "preview");
            if (preview.isBlank()) continue;

            String id = item.has("id") ? item.get("id").getAsString() : "";
            if (id.isBlank()) continue;
            JsonObject artist = item.has("artist") && item.get("artist").isJsonObject()
                    ? item.getAsJsonObject("artist") : new JsonObject();
            JsonObject album = item.has("album") && item.get("album").isJsonObject()
                    ? item.getAsJsonObject("album") : new JsonObject();
            tracks.add(Track.online(
                    id,
                    string(item, "title"),
                    string(artist, "name"),
                    string(album, "title"),
                    preview
            ));
        }
        return tracks;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
}
