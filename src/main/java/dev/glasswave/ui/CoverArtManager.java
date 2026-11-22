package dev.glasswave.ui;

import dev.glasswave.library.Track;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Loads provider artwork off-thread; generated covers keep local music fully offline. */
public final class CoverArtManager {
    private static final int MAX_DOWNLOAD_BYTES = 2 * 1024 * 1024;
    private static final ConcurrentHashMap<String, Identifier> READY = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();

    private CoverArtManager() {
    }

    public static void draw(DrawContext context, Track track, int x, int y, int size) {
        Identifier texture = track == null ? null : getTexture(track);
        if (texture != null) {
            context.drawTexture(RenderLayer::getGuiTextured, texture, x, y,
                    0.0f, 0.0f, size, size, size, size, 0xFFFFFFFF);
            GlasswaveWidget.rounded(context, x, y, size, 1, 1, 0x48FFFFFF);
            GlasswaveWidget.rounded(context, x, y, 1, size, 1, 0x20FFFFFF);
            return;
        }
        drawGenerated(context, track, x, y, size);
    }

    private static Identifier getTexture(Track track) {
        if (track.coverUrl == null || track.coverUrl.isBlank()) return null;
        Identifier ready = READY.get(track.id);
        if (ready != null) return ready;
        if (REQUESTED.add(track.id)) request(track);
        return null;
    }

    private static void request(Track track) {
        String trackId = track.id;
        String imageUrl = track.coverUrl;
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = URI.create(imageUrl);
                String host = uri.getHost();
                if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                        || !(host.equals("cdns-images.dzcdn.net") || host.endsWith(".dzcdn.net"))) {
                    throw new IllegalArgumentException("Artwork host is not allowed");
                }

                URLConnection connection = uri.toURL().openConnection();
                connection.setConnectTimeout(7_000);
                connection.setReadTimeout(10_000);
                connection.setRequestProperty("User-Agent", "Glasswave-Minecraft/0.2.0");
                byte[] bytes;
                try (InputStream input = connection.getInputStream();
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("Artwork is too large");
                        output.write(buffer, 0, read);
                    }
                    bytes = output.toByteArray();
                }

                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    NativeImage image = null;
                    try {
                        image = NativeImage.read(new ByteArrayInputStream(bytes));
                        if (image.getWidth() > 2048 || image.getHeight() > 2048
                                || image.getWidth() < 1 || image.getHeight() < 1) {
                            image.close();
                            return;
                        }
                        Identifier identifier = Identifier.of("glasswave",
                                "covers/" + Integer.toUnsignedString(trackId.hashCode(), 16));
                        client.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(image));
                        READY.put(trackId, identifier);
                    } catch (Exception exception) {
                        if (image != null) image.close();
                        System.err.println("[Glasswave] Artwork could not be decoded: " + exception.getMessage());
                    }
                });
            } catch (Exception exception) {
                System.err.println("[Glasswave] Artwork unavailable: " + exception.getMessage());
            }
        });
    }

    private static void drawGenerated(DrawContext context, Track track, int x, int y, int size) {
        int seed = track == null ? 0x4A8390 : track.id.hashCode();
        int[] colors = {
                0xFF31596B, 0xFF66517B, 0xFF386D68, 0xFF665B45,
                0xFF455D83, 0xFF704B64, 0xFF3D657C, 0xFF65527A
        };
        int base = colors[Math.floorMod(seed, colors.length)];
        int glow = 0xFF000000 | ((base & 0x00FEFEFE) + 0x001A1A1A & 0x00FFFFFF);
        GlasswaveWidget.rounded(context, x, y, size, size, Math.max(5, size / 4), base);
        GlasswaveWidget.rounded(context, x + size / 5, y - size / 5, size, size, size / 2, 0x183DE6DA);
        GlasswaveWidget.rounded(context, x + size / 3, y + size / 3, size / 2, size / 2, size / 4, glow);
        GlasswaveWidget.rounded(context, x + 2, y + 2, size - 4, 1, 1, 0x48FFFFFF);

        String glyph = track == null || track.title == null || track.title.isBlank()
                ? "♪" : track.title.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        MinecraftClient client = MinecraftClient.getInstance();
        int textWidth = client.textRenderer.getWidth(glyph);
        context.drawTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal(glyph),
                x + (size - textWidth) / 2, y + (size - 9) / 2, 0xFFF0F7FA);
    }

    public static void drawEqualizer(DrawContext context, int x, int centerY, int bars, int maxHeight,
                                     boolean active, int color) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < bars; i++) {
            double wave = active
                    ? 0.5 + 0.5 * Math.sin(now / 135.0 + i * 0.86) * Math.cos(now / 470.0 + i * 0.29)
                    : 0.08 + 0.05 * (i % 3);
            int barHeight = Math.max(2, (int) (2 + wave * (maxHeight - 2)));
            GlasswaveWidget.rounded(context, x + i * 4, centerY - barHeight / 2, 2, barHeight, 1, color);
        }
    }
}
