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

    private GlasswaveWidget() {
    }

    public static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        Track track = MusicLibrary.currentTrack();
        if (client.player == null || client.world == null || client.options.hudHidden
                || client.currentScreen != null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = HudPosition.getX(screenWidth);
        int y = HudPosition.getY(screenHeight);
        long time = System.currentTimeMillis();
        int auraAlpha = 18 + (int) ((Math.sin(time / 720.0) + 1.0) * 7.0);
        int aura = (auraAlpha << 24) | 0x002BC8C7;

        // Layered glass: soft ambient glow, smoked body, inner reflection and fine edge.
        rounded(context, x - 4, y - 4, WIDTH + 8, HEIGHT + 8, 21, aura);
        rounded(context, x - 1, y + 2, WIDTH + 2, HEIGHT + 2, 19, 0xC807101A);
        rounded(context, x, y, WIDTH, HEIGHT, 18, 0xDB101C2B);
        rounded(context, x + 1, y + 1, WIDTH - 2, HEIGHT - 3, 17, 0xC51C2D40);
        rounded(context, x + 3, y + 2, WIDTH - 6, 1, 1, 0x32FFFFFF);
        rounded(context, x + 9, y + 3, 66, 1, 1, 0x16A9ECE6);
        rounded(context, x + WIDTH - 52, y + 2, 35, 1, 1, 0x22FFFFFF);

        CoverArtManager.draw(context, track, x + 11, y + 9, 34);
        rounded(context, x + 10, y + 8, 36, 36, 12, 0x40FFFFFF);
        rounded(context, x + 11, y + 9, 34, 1, 1, 0x58FFFFFF);

        String title = trim(client, track == null ? "Glasswave" : track.title, 112);
        String artist = trim(client, track == null ? "Нажми, чтобы открыть медиатеку" : track.artist, 112);
        context.drawTextWithShadow(client.textRenderer, Text.literal(title), x + 54, y + 12, 0xFFF1F5FA);
        context.drawText(client.textRenderer, Text.literal(artist), x + 54, y + 28, 0xFFA5B6C5, false);

        CoverArtManager.drawEqualizer(context, x + 174, y + 26, 4, 15,
                AudioEngine.isPlaying(), 0xFF8DDED6);

        int buttonX = x + WIDTH - 37;
        rounded(context, buttonX - 1, y + 8, 30, 32, 16, 0x20FFFFFF);
        rounded(context, buttonX, y + 9, 28, 30, 15, 0x5AFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal(AudioEngine.isPlaying() ? "Ⅱ" : "▶"), buttonX + 9, y + 18, 0xFFF4F8FC);

        rounded(context, x + 13, y + HEIGHT - 4, WIDTH - 26, 1, 1, 0x35FFFFFF);
        if (AudioEngine.getDurationSeconds() > 0) {
            float progress = (float) Math.min(1.0, AudioEngine.getElapsedSeconds() / AudioEngine.getDurationSeconds());
            rounded(context, x + 13, y + HEIGHT - 4,
                    Math.max(2, (int) ((WIDTH - 26) * progress)), 1, 1, 0xFF91E0D8);
        }
    }

    public static boolean contains(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int x = HudPosition.getX(screenWidth);
        int y = HudPosition.getY(screenHeight);
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    public static void onClick(int mouseX, int mouseY, MinecraftClient client) {
        int x = HudPosition.getX(client.getWindow().getScaledWidth());
        int y = HudPosition.getY(client.getWindow().getScaledHeight());
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
