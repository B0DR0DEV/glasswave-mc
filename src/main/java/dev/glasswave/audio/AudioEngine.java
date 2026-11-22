package dev.glasswave.audio;

import dev.glasswave.library.Track;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.io.FilterInputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/** Plays local audio and short remote previews without replacing Minecraft's audio engine. */
public final class AudioEngine {
    private static volatile SourceDataLine line;
    private static volatile Thread worker;
    private static volatile Track activeTrack;
    private static volatile boolean paused;
    private static volatile boolean finished;
    private static volatile boolean playing;
    private static volatile float volume = 0.82f;
    private static volatile double elapsedSeconds;
    private static volatile double durationSeconds;
    private static final AtomicLong SESSION = new AtomicLong();

    private AudioEngine() {
    }

    public static synchronized void play(Track track) {
        stop();
        long session = SESSION.incrementAndGet();
        activeTrack = track;
        paused = false;
        finished = false;
        elapsedSeconds = 0;
        durationSeconds = 0;
        Thread thread = new Thread(() -> decodeAndPlay(track, session), "Glasswave-Audio");
        thread.setDaemon(true);
        worker = thread;
        thread.start();
    }

    private static void decodeAndPlay(Track track, long session) {
        SourceDataLine output = null;
        boolean completed = false;
        try (InputStream source = open(track);
             AudioInputStream encoded = AudioSystem.getAudioInputStream(new BufferedInputStream(source))) {
            AudioFormat original = encoded.getFormat();
            float sampleRate = original.getSampleRate() > 0 ? original.getSampleRate() : 44100.0f;
            int channels = Math.max(1, Math.min(2, original.getChannels()));
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                    channels, channels * 2, sampleRate, false);

            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, encoded)) {
                long frames = decoded.getFrameLength();
                if (frames > 0 && pcm.getFrameRate() > 0) durationSeconds = frames / (double) pcm.getFrameRate();

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcm);
                output = (SourceDataLine) AudioSystem.getLine(info);
                if (session != SESSION.get()) return;
                line = output;
                output.open(pcm, 48 * 1024);
                applyVolume(output);
                output.start();
                playing = true;

                byte[] buffer = new byte[12 * 1024];
                int read;
                while (session == SESSION.get() && (read = decoded.read(buffer, 0, buffer.length)) != -1) {
                    while (paused && session == SESSION.get()) {
                        output.stop();
                        Thread.sleep(45);
                    }
                    if (session != SESSION.get()) break;
                    output.start();
                    applyVolume(output);
                    int written = output.write(buffer, 0, read);
                    if (pcm.getFrameRate() > 0 && pcm.getFrameSize() > 0) {
                        elapsedSeconds += written / (pcm.getFrameRate() * pcm.getFrameSize());
                    }
                }
                if (session == SESSION.get()) {
                    output.drain();
                    completed = true;
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            if (session == SESSION.get()) {
                System.err.println("[Glasswave] Audio playback failed: " + exception.getMessage());
                completed = true;
            }
        } finally {
            if (output != null) {
                try { output.stop(); } catch (Exception ignored) { }
                try { output.close(); } catch (Exception ignored) { }
            }
            if (session == SESSION.get()) {
                line = null;
                playing = false;
                paused = false;
                if (Thread.currentThread() == worker) worker = null;
                if (completed) finished = true;
            }
        }
    }

    private static InputStream open(Track track) throws Exception {
        if ("local".equals(track.source)) {
            return new FileInputStream(Path.of(track.location).toFile());
        }
        return openDeezerPreview(track.previewUrl);
    }

    /** Only HTTPS streams hosted by Deezer's dzcdn.net media domain are accepted. */
    private static InputStream openDeezerPreview(String rawUrl) throws Exception {
        URI current = URI.create(rawUrl);
        for (int redirect = 0; redirect <= 4; redirect++) {
            validateDeezerMediaUri(current);
            URLConnection rawConnection = current.toURL().openConnection();
            rawConnection.setConnectTimeout(8_000);
            rawConnection.setReadTimeout(15_000);
            rawConnection.setRequestProperty("User-Agent", "Glasswave-Minecraft/0.2.0");

            if (!(rawConnection instanceof HttpURLConnection connection)) {
                throw new IllegalStateException("Unsupported preview connection");
            }
            connection.setInstanceFollowRedirects(false);
            int response = connection.getResponseCode();
            if (response >= 300 && response < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.isBlank()) {
                    throw new IllegalStateException("Preview redirect did not include a destination");
                }
                current = current.resolve(location);
                continue;
            }
            if (response < 200 || response >= 300) {
                connection.disconnect();
                throw new IllegalStateException("Preview server returned HTTP " + response);
            }

            InputStream stream = connection.getInputStream();
            return new FilterInputStream(stream) {
                @Override
                public void close() throws java.io.IOException {
                    try {
                        super.close();
                    } finally {
                        connection.disconnect();
                    }
                }
            };
        }
        throw new IllegalStateException("Too many preview redirects");
    }

    private static void validateDeezerMediaUri(URI uri) {
        String host = uri.getHost();
        boolean allowedHost = host != null
                && (host.equalsIgnoreCase("dzcdn.net") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".dzcdn.net"));
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !allowedHost || uri.getUserInfo() != null) {
            throw new SecurityException("Only HTTPS previews from Deezer's dzcdn.net domain are allowed");
        }
    }

    public static synchronized void togglePause() {
        if (activeTrack == null || worker == null) return;
        paused = !paused;
        SourceDataLine current = line;
        if (current != null) {
            if (paused) current.stop();
            else current.start();
        }
    }

    public static synchronized void stop() {
        SESSION.incrementAndGet();
        paused = false;
        SourceDataLine current = line;
        if (current != null) {
            try { current.stop(); } catch (Exception ignored) { }
            try { current.flush(); } catch (Exception ignored) { }
            try { current.close(); } catch (Exception ignored) { }
        }
        Thread currentWorker = worker;
        if (currentWorker != null && currentWorker != Thread.currentThread()) currentWorker.interrupt();
        line = null;
        worker = null;
        playing = false;
        activeTrack = null;
        finished = false;
        elapsedSeconds = 0;
        durationSeconds = 0;
    }

    public static void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));
        SourceDataLine current = line;
        if (current != null) applyVolume(current);
    }

    private static void applyVolume(SourceDataLine output) {
        if (output.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) output.getControl(FloatControl.Type.MASTER_GAIN);
            float db = volume <= 0.001f ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        }
    }

    public static boolean isPaused() { return paused; }
    public static boolean isPlaying() { return playing && !paused; }
    public static Track getActiveTrack() { return activeTrack; }
    public static float getVolume() { return volume; }
    public static double getElapsedSeconds() { return elapsedSeconds; }
    public static double getDurationSeconds() { return durationSeconds; }

    public static boolean consumeFinished() {
        if (!finished) return false;
        finished = false;
        return true;
    }
}
