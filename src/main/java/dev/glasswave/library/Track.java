package dev.glasswave.library;

public final class Track {
    public String id = "";
    public String title = "Unknown track";
    public String artist = "Unknown artist";
    public String album = "";
    public String source = "local";
    public String location = "";
    public String previewUrl = "";
    public String coverUrl = "";
    public boolean favorite = false;
    public boolean explicit = false;

    public Track() {
    }

    public static Track local(String absolutePath) {
        Track track = new Track();
        track.id = "file:" + absolutePath;
        track.location = absolutePath;
        track.source = "local";
        String fileName = java.nio.file.Path.of(absolutePath).getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        track.title = dot > 0 ? fileName.substring(0, dot) : fileName;
        track.artist = "On this device";
        return track;
    }

    public static Track online(String id, String title, String artist, String album, String previewUrl, String coverUrl) {
        Track track = new Track();
        track.id = "deezer:" + id;
        track.title = title;
        track.artist = artist;
        track.album = album;
        track.source = "deezer";
        track.previewUrl = previewUrl;
        track.coverUrl = coverUrl;
        track.location = "Deezer preview";
        return track;
    }

    public boolean isPlayable() {
        return "local".equals(source)
                ? !location.isBlank() && java.nio.file.Files.isRegularFile(java.nio.file.Path.of(location))
                : !previewUrl.isBlank();
    }
}
