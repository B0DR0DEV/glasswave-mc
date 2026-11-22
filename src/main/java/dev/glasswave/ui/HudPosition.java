package dev.glasswave.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores the floating player's screen position and drag state. */
public final class HudPosition {
    private static final int DEFAULT_MARGIN = 12;
    private static final int DEFAULT_BOTTOM = 76;
    private static final Path FILE = FabricLoader.getInstance().getGameDir()
            .resolve("glasswave").resolve("hud.json");

    private static int x = Integer.MIN_VALUE;
    private static int y = Integer.MIN_VALUE;
    private static boolean dragging;
    private static int dragOffsetX;
    private static int dragOffsetY;

    private HudPosition() {
    }

    public static void initialize() {
        try {
            if (!Files.isRegularFile(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("x")) x = root.get("x").getAsInt();
            if (root.has("y")) y = root.get("y").getAsInt();
        } catch (Exception exception) {
            System.err.println("[Glasswave] HUD position could not be restored: " + exception.getMessage());
        }
    }

    public static int getX(int screenWidth) {
        int value = x == Integer.MIN_VALUE ? screenWidth - GlasswaveWidget.WIDTH - DEFAULT_MARGIN : x;
        return clamp(value, 0, Math.max(0, screenWidth - GlasswaveWidget.WIDTH));
    }

    public static int getY(int screenHeight) {
        int value = y == Integer.MIN_VALUE ? screenHeight - GlasswaveWidget.HEIGHT - DEFAULT_BOTTOM : y;
        return clamp(value, 0, Math.max(0, screenHeight - GlasswaveWidget.HEIGHT));
    }

    public static boolean beginDrag(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int currentX = getX(screenWidth);
        int currentY = getY(screenHeight);
        // The thin upper glass edge is the grab handle; the rest remains clickable.
        if (mouseY < currentY || mouseY > currentY + 11
                || mouseX < currentX || mouseX > currentX + GlasswaveWidget.WIDTH - 48) {
            return false;
        }
        dragging = true;
        dragOffsetX = mouseX - currentX;
        dragOffsetY = mouseY - currentY;
        return true;
    }

    public static void dragTo(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!dragging) return;
        x = clamp(mouseX - dragOffsetX, 0, Math.max(0, screenWidth - GlasswaveWidget.WIDTH));
        y = clamp(mouseY - dragOffsetY, 0, Math.max(0, screenHeight - GlasswaveWidget.HEIGHT));
    }

    public static boolean endDrag() {
        if (!dragging) return false;
        dragging = false;
        save();
        return true;
    }

    public static boolean isDragging() {
        return dragging;
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("x", x);
            root.addProperty("y", y);
            Files.writeString(FILE, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[Glasswave] HUD position could not be saved: " + exception.getMessage());
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
