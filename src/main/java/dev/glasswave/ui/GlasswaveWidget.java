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
        int auraAlpha = 11 + (int) ((Math.sin(time / 900.0) + 1.0) * 4.0);
        int aura = (auraAlpha << 24) | 0x002BC8C7;

        // Layered glass: soft ambient glow, smoked body, inner reflection and fine edge.
        rounded(context, x - 5, y - 5, WIDTH + 10, HEIGHT + 10, 22, aura);
        rounded(context, x - 1, y + 2, WIDTH + 2, HEIGHT + 2, 19, 0x56030A12);
        rounded(context, x, y, WIDTH, HEIGHT, 18, 0x94101C2B);
        rounded(context, x + 1, y + 1, WIDTH - 2, HEIGHT - 3, 17, 0x702B4A60);
        rounded(context, x + 3, y + 2, WIDTH - 6, 1, 1, 0x65FFFFFF);
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
                Text.literal(AudioEngine.isPlaying() ? "||" : ">"), buttonX + 9, y + 18, 0xFFF4F8FC);

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

    // Small vector glyphs are drawn with Minecraft primitives, avoiding font-dependent
    // Unicode symbols that can turn into boxes or asterisks on some installations.
    public static void drawWaveMark(DrawContext c, int x, int y, int color) {
        rounded(c, x + 1, y + 8, 3, 10, 1, color);
        rounded(c, x + 7, y + 3, 3, 20, 1, color);
        rounded(c, x + 13, y, 3, 26, 1, color);
        rounded(c, x + 19, y + 6, 3, 14, 1, color);
    }

    public static void drawLibraryIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x, y + 1, 13, 12, 2, color);
        rounded(c, x + 2, y + 3, 9, 1, 1, 0xFF243F50);
        rounded(c, x + 2, y + 6, 7, 1, 1, 0xFF243F50);
        rounded(c, x + 2, y + 9, 8, 1, 1, 0xFF243F50);
    }

    public static void drawHeartIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x + 2, y + 1, 4, 4, 2, color);
        rounded(c, x + 7, y + 1, 4, 4, 2, color);
        rounded(c, x + 1, y + 4, 11, 4, 2, color);
        rounded(c, x + 3, y + 7, 7, 3, 2, color);
        rounded(c, x + 5, y + 9, 3, 2, 1, color);
    }

    public static void drawSearchIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x, y, 9, 9, 5, color);
        rounded(c, x + 2, y + 2, 5, 5, 3, 0xFF193346);
        rounded(c, x + 8, y + 8, 3, 6, 1, color);
    }

    public static void drawFolderIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x, y + 3, 15, 10, 3, color);
        rounded(c, x + 1, y + 1, 7, 4, 2, color);
        rounded(c, x + 2, y + 5, 11, 1, 1, 0xFF315264);
    }

    public static void drawCloseIcon(DrawContext c, int x, int y, int color) {
        for (int i = 0; i < 9; i++) {
            rounded(c, x + i, y + i, 2, 2, 1, color);
            rounded(c, x + 8 - i, y + i, 2, 2, 1, color);
        }
    }

    public static void drawPlayIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x, y, 3, 14, 1, color);
        rounded(c, x + 3, y + 2, 3, 10, 1, color);
        rounded(c, x + 6, y + 4, 3, 6, 1, color);
    }

    public static void drawPauseIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x, y, 3, 14, 1, color);
        rounded(c, x + 8, y, 3, 14, 1, color);
    }

    public static void drawSkipIcon(DrawContext c, int x, int y, boolean next, int color) {
        if (!next) {
            rounded(c, x + 9, y, 2, 14, 1, color);
            rounded(c, x + 1, y + 1, 3, 12, 1, color);
            rounded(c, x + 4, y + 3, 3, 8, 1, color);
            rounded(c, x + 7, y + 5, 2, 4, 1, color);
        } else {
            rounded(c, x + 1, y, 2, 14, 1, color);
            rounded(c, x + 8, y + 1, 3, 12, 1, color);
            rounded(c, x + 5, y + 3, 3, 8, 1, color);
            rounded(c, x + 3, y + 5, 2, 4, 1, color);
        }
    }

    public static void drawVolumeIcon(DrawContext c, int x, int y, int color) {
        rounded(c, x, y + 4, 3, 6, 1, color);
        rounded(c, x + 3, y + 2, 3, 10, 1, color);
        rounded(c, x + 6, y, 2, 2, 1, color);
        rounded(c, x + 6, y + 12, 2, 2, 1, color);
    }

}
