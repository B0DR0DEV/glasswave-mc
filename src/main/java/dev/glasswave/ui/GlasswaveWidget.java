package dev.glasswave.ui;

import dev.glasswave.audio.AudioEngine;
import dev.glasswave.library.MusicLibrary;
import dev.glasswave.library.Track;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class GlasswaveWidget {
    public static final int WIDTH = 236;
    public static final int HEIGHT = 52;
    private static final int MARGIN = 12;

    private GlasswaveWidget() {
    }

    public static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        Track track = MusicLibrary.currentTrack();
        if (client.player == null || client.world == null || client.options.hudHidden
                || client.currentScreen != null) return;

        int x = client.getWindow().getScaledWidth() - WIDTH - MARGIN;
        int y = client.getWindow().getScaledHeight() - HEIGHT - 76;
        long time = System.currentTimeMillis();
        int glow = 0x1827C6C9 + ((int) ((Math.sin(time / 750.0) + 1) * 7) << 24);

        rounded(context, x - 3, y - 3, WIDTH + 6, HEIGHT + 6, 18, glow);
        rounded(context, x, y + 2, WIDTH, HEIGHT, 17, 0xD90A111D);
        rounded(context, x + 1, y, WIDTH - 2, HEIGHT - 3, 16, 0xC8192535);
        rounded(context, x + 1, y, WIDTH - 2, 1, 16, 0x35FFFFFF);
        rounded(context, x + 13, y + 10, 32, 32, 11, 0xD8325267);
        rounded(context, x + 17, y + 14, 24, 24, 8, 0xFF86BFC6);
        rounded(context, x + 23, y + 22, 2, 8, 1, 0xFF17303A);
        rounded(context, x + 27, y + 18, 2, 16, 1, 0xFF17303A);
        rounded(context, x + 31, y + 20, 2, 12, 1, 0xFF17303A);
        rounded(context, x + 35, y + 24, 2, 5, 1, 0xFF17303A);

        String title = trim(client, track == null ? "Glasswave" : track.title, 116);
        String artist = trim(client, track == null ? "Нажми, чтобы открыть медиатеку" : track.artist, 116);
        context.drawTextWithShadow(client.textRenderer, Text.literal(title), x + 54, y + 13, 0xFFF1F5FA);
        context.drawText(client.textRenderer, Text.literal(artist), x + 54, y + 29, 0xFF9DAFC0, false);

        int buttonX = x + WIDTH - 39;
        rounded(context, buttonX, y + 10, 28, 28, 14, 0x4BFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal(AudioEngine.isPlaying() ? "Ⅱ" : "▶"), buttonX + 9, y + 17, 0xFFF4F8FC);
        rounded(context, x + 14, y + HEIGHT - 4, WIDTH - 28, 1, 1, 0x30FFFFFF);
        if (AudioEngine.getDurationSeconds() > 0) {
            float progress = (float) Math.min(1.0, AudioEngine.getElapsedSeconds() / AudioEngine.getDurationSeconds());
            rounded(context, x + 14, y + HEIGHT - 4, Math.max(2, (int) ((WIDTH - 28) * progress)), 1, 1, 0xFF8BD7D2);
        }
    }

    public static boolean contains(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int x = screenWidth - WIDTH - MARGIN;
        int y = screenHeight - HEIGHT - 76;
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    public static void onClick(int mouseX, int mouseY, MinecraftClient client) {
        int x = client.getWindow().getScaledWidth() - WIDTH - MARGIN;
        int y = client.getWindow().getScaledHeight() - HEIGHT - 76;
        if (mouseX >= x + WIDTH - 47 && mouseY >= y + 6 && mouseY <= y + HEIGHT - 5) {
            MusicLibrary.togglePlayback();
        } else {
            client.setScreen(new GlasswaveScreen());
        }
    }

    private static String trim(MinecraftClient client, String value, int maxWidth) {
        return client.textRenderer.trimToWidth(value == null ? "" : value, maxWidth);
    }

    public static void rounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int row = 0; row < height; row++) {
            int inset = 0;
            if (row < r) {
                double dy = r - row - 0.5;
                inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            } else if (row >= height - r) {
                double dy = row - (height - r) + 0.5;
                inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }
}
