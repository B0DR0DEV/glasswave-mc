package dev.glasswave.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.glasswave.audio.AudioEngine;
import dev.glasswave.online.DeezerSearch;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.loader.api.FabricLoader;

import javax.swing.JFileChooser;
import java.awt.GraphicsEnvironment;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MusicLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Track> tracks = new ArrayList<>();
    private static final List<Path> folders = new ArrayList<>();
    private static final List<Track> searchResults = new ArrayList<>();
    private static final List<Track> queue = new ArrayList<>();
    private static final Set<String> favoriteIds = new HashSet<>();

    private static Path configDirectory;
    private static Path defaultMusicDirectory;
    private static Path configFile;
    private static Track currentTrack;
    private static int queueIndex = -1;
    private static volatile boolean searching;
    private static volatile String status = "Drop music into glasswave/music to get started";

    private MusicLibrary() {
    }

    public static synchronized void initialize() {
        configDirectory = FabricLoader.getInstance().getGameDir().resolve("glasswave");
        defaultMusicDirectory = configDirectory.resolve("music");
        configFile = configDirectory.resolve("library.json");
        try {
            Files.createDirectories(defaultMusicDirectory);
            load();
            if (!folders.contains(defaultMusicDirectory)) folders.add(defaultMusicDirectory);
            rescanAllFolders();
            status = localTracks().isEmpty()
                    ? "Choose a folder or add music to glasswave/music"
                    : localTracks().size() + " tracks ready";
            save();
        } catch (Exception exception) {
            status = "Library could not be loaded: " + shortError(exception);
            System.err.println("[Glasswave] " + exception);
        }
    }

    private static void load() {
        if (!Files.isRegularFile(configFile)) return;
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray savedFolders = root.has("folders") && root.get("folders").isJsonArray()
                    ? root.getAsJsonArray("folders") : new JsonArray();
            for (JsonElement element : savedFolders) {
                try {
                    Path path = Path.of(element.getAsString()).toAbsolutePath().normalize();
                    if (!folders.contains(path)) folders.add(path);
                } catch (Exception ignored) {
                }
            }
            JsonArray savedFavorites = root.has("favorites") && root.get("favorites").isJsonArray()
                    ? root.getAsJsonArray("favorites") : new JsonArray();
            for (JsonElement element : savedFavorites) {
                try { favoriteIds.add(element.getAsString()); } catch (Exception ignored) { }
            }
            JsonArray savedTracks = root.has("tracks") && root.get("tracks").isJsonArray()
                    ? root.getAsJsonArray("tracks") : new JsonArray();
            for (JsonElement element : savedTracks) {
                try {
                    Track track = GSON.fromJson(element, Track.class);
                    if (track != null && track.id != null && !track.id.isBlank()
                            && "deezer".equals(track.source) && favoriteIds.contains(track.id)) {
                        track.favorite = true;
                        tracks.add(track);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception exception) {
            System.err.println("[Glasswave] Could not read library.json: " + exception.getMessage());
        }
    }

    public static synchronized void save() {
        if (configFile == null) return;
        try {
            Files.createDirectories(configDirectory);
            JsonObject root = new JsonObject();
            JsonArray savedFolders = new JsonArray();
            for (Path folder : folders) savedFolders.add(folder.toString());
            root.add("folders", savedFolders);
            JsonArray savedFavorites = new JsonArray();
            for (String id : favoriteIds) savedFavorites.add(id);
            root.add("favorites", savedFavorites);
            JsonArray savedTracks = new JsonArray();
            for (Track track : tracks) {
                if (track.favorite && "deezer".equals(track.source)) savedTracks.add(GSON.toJsonTree(track));
            }
            root.add("tracks", savedTracks);
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception exception) {
            System.err.println("[Glasswave] Could not save library: " + exception.getMessage());
        }
    }

    public static synchronized void rescanAllFolders() {
        Map<String, Track> existing = new LinkedHashMap<>();
        for (Track track : tracks) {
            if ("local".equals(track.source)) existing.put(track.id, track);
        }
        tracks.removeIf(track -> "local".equals(track.source));
        Set<String> scanned = new HashSet<>();

        for (Path folder : folders) {
            if (!Files.isDirectory(folder)) continue;
            try (var stream = Files.walk(folder)) {
                stream.filter(Files::isRegularFile)
                        .filter(MusicLibrary::isSupportedAudio)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(path -> {
                            String absolute = path.toAbsolutePath().normalize().toString();
                            String id = "file:" + absolute;
                            if (!scanned.add(id)) return;
                            Track found = existing.get(id);
                            Track selected = found != null ? found : Track.local(absolute);
                            selected.favorite = favoriteIds.contains(selected.id);
                            tracks.add(selected);
                        });
            } catch (Exception exception) {
                System.err.println("[Glasswave] Could not scan " + folder + ": " + exception.getMessage());
            }
        }
    }

    public static synchronized void addFolder(Path folder) {
        if (folder == null || !Files.isDirectory(folder)) {
            status = "That folder could not be opened";
            return;
        }
        Path normalized = folder.toAbsolutePath().normalize();
        if (!folders.contains(normalized)) folders.add(normalized);
        rescanAllFolders();
        save();
        status = "Added folder · " + localTracks().size() + " local tracks";
    }

    public static void chooseFolder(MinecraftClient client) {
        if (GraphicsEnvironment.isHeadless()) {
            status = "Folder picker is unavailable in this environment";
            return;
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose a music folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setCurrentDirectory(defaultMusicDirectory.toFile());
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    Path selected = chooser.getSelectedFile().toPath();
                    client.execute(() -> addFolder(selected));
                }
            } catch (Exception exception) {
                client.execute(() -> status = "Folder picker failed: " + shortError(exception));
            }
        });
    }

    public static void searchOnline(String query, MinecraftClient client) {
        String cleaned = query == null ? "" : query.trim();
        if (cleaned.isBlank()) {
            status = "Type a song or artist first";
            return;
        }
        if (searching) return;
        searching = true;
        status = "Searching the catalog…";
        CompletableFuture.supplyAsync(() -> {
            try {
                return DeezerSearch.search(cleaned);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((results, error) -> client.execute(() -> {
            synchronized (MusicLibrary.class) {
                searchResults.clear();
                if (error == null && results != null) {
                    searchResults.addAll(results);
                    status = results.isEmpty()
                            ? "No preview tracks found for this search"
                            : results.size() + " preview results · streamed from Deezer";
                } else {
                    String message = error == null ? "Unknown catalog error" : shortError(error.getCause() == null ? error : error.getCause());
                    status = "Online search unavailable · " + message;
                }
                searching = false;
            }
        }));
    }

    public static synchronized void playFrom(List<Track> source, int index) {
        if (source == null || source.isEmpty() || index < 0 || index >= source.size()) return;
        queue.clear();
        queue.addAll(source);
        queueIndex = index;
        currentTrack = queue.get(queueIndex);
        if (!currentTrack.isPlayable()) {
            status = "This audio file is no longer available";
            return;
        }
        status = "Playing · " + currentTrack.title;
        AudioEngine.play(currentTrack);
    }

    public static synchronized void togglePlayback() {
        if (currentTrack == null) {
            List<Track> local = localTracks();
            if (!local.isEmpty()) playFrom(local, 0);
            return;
        }
        if (AudioEngine.isPlaying() || AudioEngine.isPaused()) {
            AudioEngine.togglePause();
        } else if (currentTrack.isPlayable()) {
            AudioEngine.play(currentTrack);
        }
    }

    public static synchronized void next() {
        if (queue.isEmpty()) {
            List<Track> local = localTracks();
            if (!local.isEmpty()) playFrom(local, 0);
            return;
        }
        queueIndex = (queueIndex + 1) % queue.size();
        currentTrack = queue.get(queueIndex);
        if (currentTrack.isPlayable()) AudioEngine.play(currentTrack);
    }

    public static synchronized void previous() {
        if (queue.isEmpty()) return;
        queueIndex = (queueIndex - 1 + queue.size()) % queue.size();
        currentTrack = queue.get(queueIndex);
        if (currentTrack.isPlayable()) AudioEngine.play(currentTrack);
    }

    public static synchronized void toggleFavorite(Track track) {
        if (track == null) return;
        Track stored = tracks.stream().filter(candidate -> candidate.id.equals(track.id)).findFirst().orElse(null);
        if (stored == null) {
            stored = track;
            tracks.add(stored);
        }
        stored.favorite = !stored.favorite;
        track.favorite = stored.favorite;
        if (stored.favorite) favoriteIds.add(stored.id);
        else favoriteIds.remove(stored.id);
        save();
    }

    public static synchronized List<Track> localTracks() {
        return tracks.stream()
                .filter(track -> "local".equals(track.source) && track.isPlayable())
                .toList();
    }

    public static synchronized List<Track> favorites() {
        return tracks.stream().filter(track -> track.favorite && track.isPlayable()).toList();
    }

    public static synchronized List<Track> searchResults() {
        return List.copyOf(searchResults);
    }

    public static synchronized Track findStored(String id) {
        return tracks.stream().filter(track -> track.id.equals(id)).findFirst().orElse(null);
    }

    public static Track currentTrack() { return currentTrack; }
    public static boolean isSearching() { return searching; }
    public static String status() { return status; }
    public static int folderCount() { return folders.size(); }

    public static void onClientTick(MinecraftClient client) {
        if (AudioEngine.consumeFinished()) {
            client.execute(() -> {
                if (currentTrack != null) status = "Finished · " + currentTrack.title;
            });
        }
    }

    private static boolean isSupportedAudio(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".aiff")
                || name.endsWith(".aif") || name.endsWith(".au");
    }

    private static String shortError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return error.getClass().getSimpleName();
        return message.length() > 72 ? message.substring(0, 69) + "…" : message;
    }
}
