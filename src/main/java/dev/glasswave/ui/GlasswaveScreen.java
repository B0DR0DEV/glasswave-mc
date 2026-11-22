package dev.glasswave.ui;

import dev.glasswave.audio.AudioEngine;
import dev.glasswave.library.MusicLibrary;
import dev.glasswave.library.Track;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GlasswaveScreen extends Screen {
    private static final int TAB_LIBRARY = 0;
    private static final int TAB_FAVORITES = 1;
    private static final int TAB_DISCOVER = 2;

    private int tab = TAB_LIBRARY;
    private int scroll;
    private String searchInput = "";
    private long openedAt;
    private boolean draggingVolume;
    private boolean searchFocused;

    private int panelX, panelY, panelW, panelH;
    private int sideX, sideY, sideW;
    private int contentX, contentY, contentW;
    private int listY, listBottom;
    private int playerY;

    public GlasswaveScreen() {
        super(Text.literal("Glasswave"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        calculateLayout();

        double t = Math.min(1.0, (System.currentTimeMillis() - openedAt) / 320.0);
        float ease = (float) (1.0 - Math.pow(1.0 - t, 3.0));
        int slide = (int) ((1.0f - ease) * 12.0f);
        int shellAlpha = (int) (0xD8 * ease);
        int textAlpha = (int) (255 * ease);
        int y = panelY + slide;

        // A layered translucent shell, rounded by scanlines rather than square widgets.
        GlasswaveWidget.rounded(context, panelX - 4, y + 5, panelW + 8, panelH, 25, fade(0x090E18, (int) (95 * ease)));
        GlasswaveWidget.rounded(context, panelX, y, panelW, panelH, 23, fade(0x101A29, shellAlpha));
        GlasswaveWidget.rounded(context, panelX + 1, y + 1, panelW - 2, panelH - 2, 22, fade(0x19283A, (int) (0xD0 * ease)));
        GlasswaveWidget.rounded(context, panelX + 2, y + 1, panelW - 4, 1, 1, fade(0xFFFFFF, (int) (42 * ease)));
        GlasswaveWidget.rounded(context, panelX + panelW - 210, y + 2, 150, 2, 2, fade(0x7FCBC8, (int) (35 * ease)));

        drawHeader(context, y, textAlpha);
        drawSidebar(context, y, mouseX, mouseY, textAlpha);
        drawContent(context, y, mouseX, mouseY, textAlpha);
        drawPlayerBar(context, y, mouseX, mouseY, textAlpha);

        super.render(context, mouseX, mouseY, delta);
    }

    private void calculateLayout() {
        panelW = Math.min(770, Math.max(330, width - 28));
        panelH = Math.min(472, Math.max(290, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sideX = panelX + 16;
        sideY = panelY + 72;
        sideW = Math.min(150, panelW / 4);
        contentX = sideX + sideW + 16;
        contentY = sideY;
        contentW = panelX + panelW - 18 - contentX;
        playerY = panelY + panelH - 66;
        listY = contentY + 106;
        listBottom = playerY - 12;
    }

    private void drawHeader(DrawContext context, int y, int alpha) {
        GlasswaveWidget.rounded(context, panelX + 20, y + 18, 35, 35, 12, fade(0x416E7D, alpha));
        GlasswaveWidget.rounded(context, panelX + 24, y + 22, 27, 27, 9, fade(0x9BD9D5, alpha));
        drawText(context, "g", panelX + 33, y + 29, fade(0x13242E, alpha), true);
        drawText(context, "GLASSWAVE", panelX + 66, y + 20, fade(0xF3F7FC, alpha), true);
        drawText(context, "музыка, которая просто рядом", panelX + 66, y + 36, fade(0x9DADBE, alpha), false);

        int closeX = panelX + panelW - 42;
        GlasswaveWidget.rounded(context, closeX, y + 22, 24, 24, 12,
                fade(0xFFFFFF, isInside(mouseX(), mouseY(), closeX, y + 22, 24, 24) ? 48 : 24));
        drawText(context, "×", closeX + 8, y + 27, fade(0xE5EDF5, alpha), false);
    }

    private void drawSidebar(DrawContext context, int y, int mouseX, int mouseY, int alpha) {
        GlasswaveWidget.rounded(context, sideX, sideY + (y - panelY), sideW, playerY - sideY - 10, 18,
                fade(0x111D2A, (int) (0xA8 * alpha / 255.0)));
        drawText(context, "РАЗДЕЛЫ", sideX + 14, sideY + 14 + (y - panelY), fade(0x7F93A7, alpha), true);

        drawNavItem(context, "◫", "Медиатека", TAB_LIBRARY, sideY + 35 + (y - panelY), mouseX, mouseY, alpha);
        drawNavItem(context, "♡", "Избранное", TAB_FAVORITES, sideY + 75 + (y - panelY), mouseX, mouseY, alpha);
        drawNavItem(context, "⌕", "Онлайн-поиск", TAB_DISCOVER, sideY + 115 + (y - panelY), mouseX, mouseY, alpha);

        int addY = playerY - 57 + (y - panelY);
        GlasswaveWidget.rounded(context, sideX + 8, addY, sideW - 16, 34, 12,
                fade(isInside(mouseX, mouseY, sideX + 8, addY, sideW - 16, 34) ? 0x38566A : 0x263E51, alpha));
        drawText(context, "+", sideX + 18, addY + 10, fade(0xA9E1DE, alpha), true);
        drawText(context, "Добавить папку", sideX + 36, addY + 11, fade(0xDCE7F0, alpha), false);

        drawText(context, "LOCAL FIRST", sideX + 14, playerY - 18 + (y - panelY), fade(0x62798D, alpha), true);
    }

    private void drawNavItem(DrawContext context, String icon, String label, int itemTab, int itemY,
                             int mouseX, int mouseY, int alpha) {
        boolean selected = tab == itemTab;
        boolean hovered = isInside(mouseX, mouseY, sideX + 7, itemY, sideW - 14, 34);
        if (selected || hovered) {
            GlasswaveWidget.rounded(context, sideX + 7, itemY, sideW - 14, 34, 11,
                    fade(selected ? 0x3D6576 : 0x263B4D, alpha));
        }
        drawText(context, icon, sideX + 18, itemY + 10, fade(selected ? 0x9FE0D9 : 0x91A6B8, alpha), true);
        drawText(context, label, sideX + 42, itemY + 11, fade(selected ? 0xF0F7FA : 0xAFC0CE, alpha), selected);
    }

    private void drawContent(DrawContext context, int y, int mouseX, int mouseY, int alpha) {
        int offset = y - panelY;
        String heading = switch (tab) {
            case TAB_FAVORITES -> "Избранное";
            case TAB_DISCOVER -> "Найти музыку";
            default -> "Твоя медиатека";
        };
        String subtitle = switch (tab) {
            case TAB_FAVORITES -> "Всё, что хочется оставить рядом";
            case TAB_DISCOVER -> "Найди трек и послушай короткое превью";
            default -> "Локальные треки в одном спокойном месте";
        };
        drawText(context, heading, contentX + 3, contentY + 5 + offset, fade(0xF2F6FB, alpha), true);
        drawText(context, subtitle, contentX + 3, contentY + 22 + offset, fade(0x8EA2B5, alpha), false);

        int searchY = contentY + 49 + offset;
        GlasswaveWidget.rounded(context, contentX, searchY, contentW - (tab == TAB_DISCOVER ? 82 : 0), 31, 13,
                fade(0x0C1724, alpha));
        drawText(context, "⌕", contentX + 12, searchY + 9, fade(0x87A9B6, alpha), true);
        String placeholder = tab == TAB_DISCOVER ? "Артист или название трека…" : "Фильтр по названию…";
        String shownQuery = searchInput.isEmpty() ? placeholder : searchInput;
        drawText(context, trim(shownQuery, contentW - (tab == TAB_DISCOVER ? 126 : 40)),
                contentX + 31, searchY + 10, fade(searchInput.isEmpty() ? 0x657D90 : 0xE3EDF4, alpha), false);

        if (tab == TAB_DISCOVER) {
            int buttonX = contentX + contentW - 72;
            boolean hovered = isInside(mouseX, mouseY, buttonX, searchY, 72, 31);
            GlasswaveWidget.rounded(context, buttonX, searchY, 72, 31, 13,
                    fade(hovered ? 0x568D93 : 0x3F747D, alpha));
            drawText(context, MusicLibrary.isSearching() ? "…" : "Найти", buttonX + 19, searchY + 10,
                    fade(0xF1FBFC, alpha), true);
        }

        int listTop = listY + offset;
        int bottom = listBottom + offset;
        GlasswaveWidget.rounded(context, contentX, listTop - 10, contentW, Math.max(46, bottom - listTop + 10), 16,
                fade(0x0D1723, (int) (0x80 * alpha / 255.0)));

        List<Track> visible = visibleTracks();
        int rowHeight = 46;
        int maxRows = Math.max(0, (bottom - listTop) / rowHeight);
        if (visible.isEmpty()) {
            String empty = tab == TAB_DISCOVER
                    ? (MusicLibrary.isSearching() ? "Ищем треки…" : "Введи запрос и нажми «Найти»")
                    : (tab == TAB_FAVORITES ? "Здесь пока пусто — отмечай треки сердечком" : "Добавь папку с MP3 или WAV-файлами");
            drawText(context, empty, contentX + 18, listTop + 18, fade(0x8297A9, alpha), false);
        } else {
            int start = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - maxRows)));
            for (int index = start; index < Math.min(visible.size(), start + maxRows); index++) {
                Track track = visible.get(index);
                int rowIndex = index - start;
                drawTrackRow(context, track, index, listTop + rowIndex * rowHeight, mouseX, mouseY, alpha);
            }
        }

        String footer = MusicLibrary.status();
        drawText(context, trim(footer, contentW), contentX + 7, playerY - 13 + offset,
                fade(0x7890A3, alpha), false);
    }

    private void drawTrackRow(DrawContext context, Track track, int index, int rowY,
                              int mouseX, int mouseY, int alpha) {
        boolean hovered = isInside(mouseX, mouseY, contentX + 5, rowY, contentW - 10, 41);
        if (hovered) {
            GlasswaveWidget.rounded(context, contentX + 5, rowY, contentW - 10, 41, 12,
                    fade(0x293E50, alpha));
        }

        CoverArtManager.draw(context, track, contentX + 13, rowY + 7, 27);
        GlasswaveWidget.rounded(context, contentX + 12, rowY + 6, 29, 29, 10, fade(0x42FFFFFF, alpha));
        GlasswaveWidget.rounded(context, contentX + 14, rowY + 8, 25, 1, 1, fade(0x48FFFFFF, alpha));

        int textX = contentX + 50;
        int textWidth = Math.max(55, contentW - 126);
        drawText(context, trim(track.title, textWidth), textX, rowY + 8, fade(0xE7EFF6, alpha), true);
        String metadata = "deezer".equals(track.source) ? track.artist + " · превью" : track.artist;
        drawText(context, trim(metadata, textWidth), textX, rowY + 23, fade(0x8399AC, alpha), false);

        String heart = isFavorite(track) ? "♥" : "♡";
        drawText(context, heart, contentX + contentW - 36, rowY + 13,
                fade(isFavorite(track) ? 0xE9A6C0 : 0x8298AA, alpha), true);
        Track active = MusicLibrary.currentTrack();
        boolean thisTrackPlaying = AudioEngine.isPlaying() && active != null && active.id.equals(track.id);
        if (thisTrackPlaying) {
            CoverArtManager.drawEqualizer(context, contentX + contentW - 76, rowY + 20, 4, 13, true, fade(0x9BE1D9, alpha));
        } else {
            drawText(context, "···", contentX + contentW - 70, rowY + 13, fade(0x71899C, alpha), false);
        }
    }

    private void drawPlayerBar(DrawContext context, int y, int mouseX, int mouseY, int alpha) {
        int barX = panelX + 16;
        int barY = playerY + (y - panelY);
        int barW = panelW - 32;
        GlasswaveWidget.rounded(context, barX, barY, barW, 50, 17, fade(0x223447, alpha));
        GlasswaveWidget.rounded(context, barX + 1, barY + 1, barW - 2, 1, 1, fade(0x4EFFFFFF, alpha));

        Track current = MusicLibrary.currentTrack();
        CoverArtManager.draw(context, current, barX + 10, barY + 9, 32);
        GlasswaveWidget.rounded(context, barX + 9, barY + 8, 34, 34, 12, fade(0x42FFFFFF, alpha));
        GlasswaveWidget.rounded(context, barX + 11, barY + 10, 30, 1, 1, fade(0x4BFFFFFF, alpha));
        String currentTitle = current == null ? "Ничего не играет" : current.title;
        String currentArtist = current == null ? "Выбери трек в медиатеке" : current.artist;
        drawText(context, trim(currentTitle, Math.max(70, barW / 3 - 20)), barX + 51, barY + 12,
                fade(0xF0F5F9, alpha), true);
        drawText(context, trim(currentArtist, Math.max(70, barW / 3 - 20)), barX + 51, barY + 28,
                fade(0x91A6B8, alpha), false);
        CoverArtManager.drawEqualizer(context, barX + 51, barY + 43, 6, 8,
                AudioEngine.isPlaying(), fade(0x8BCFCB, alpha));

        int controlsCenter = panelX + panelW / 2 + 10;
        drawControl(context, "‹", controlsCenter - 48, barY + 13, 26, mouseX, mouseY, alpha);
        drawControl(context, AudioEngine.isPlaying() ? "Ⅱ" : "▶", controlsCenter - 13, barY + 10,
                32, mouseX, mouseY, alpha);
        drawControl(context, "›", controlsCenter + 28, barY + 13, 26, mouseX, mouseY, alpha);

        if (panelW >= 540) {
            int sliderX = panelX + panelW - 145;
            int sliderY = barY + 24;
            drawText(context, "VOL", sliderX - 29, barY + 20, fade(0x718A9E, alpha), true);
            GlasswaveWidget.rounded(context, sliderX, sliderY, 84, 4, 2, fade(0x607589, alpha));
            int fill = (int) (84 * AudioEngine.getVolume());
            GlasswaveWidget.rounded(context, sliderX, sliderY, Math.max(3, fill), 4, 2, fade(0x9BD9D5, alpha));
            GlasswaveWidget.rounded(context, sliderX + fill - 4, sliderY - 3, 10, 10, 5, fade(0xE9F6F4, alpha));
        }
    }

    private void drawControl(DrawContext context, String label, int x, int y, int size,
                             int mouseX, int mouseY, int alpha) {
        boolean hovered = isInside(mouseX, mouseY, x, y, size, size);
        GlasswaveWidget.rounded(context, x, y, size, size, size / 2,
                fade(hovered ? 0x5A8B91 : 0x354B5C, alpha));
        drawText(context, label, x + (size - textRenderer.getWidth(label)) / 2,
                y + (size - 9) / 2, fade(0xEFF8F8, alpha), true);
    }

    private List<Track> visibleTracks() {
        List<Track> source = switch (tab) {
            case TAB_FAVORITES -> MusicLibrary.favorites();
            case TAB_DISCOVER -> MusicLibrary.searchResults();
            default -> MusicLibrary.localTracks();
        };
        if (searchInput.isBlank() || tab == TAB_DISCOVER) return source;
        String query = searchInput.toLowerCase(Locale.ROOT);
        List<Track> filtered = new ArrayList<>();
        for (Track track : source) {
            if (track.title.toLowerCase(Locale.ROOT).contains(query)
                    || track.artist.toLowerCase(Locale.ROOT).contains(query)) filtered.add(track);
        }
        return filtered;
    }

    private boolean isFavorite(Track track) {
        Track stored = MusicLibrary.findStored(track.id);
        return stored != null && stored.favorite || track.favorite;
    }

    private void drawText(DrawContext context, String value, int x, int y, int color, boolean shadow) {
        if (shadow) context.drawTextWithShadow(textRenderer, Text.literal(value), x, y, color);
        else context.drawText(textRenderer, Text.literal(value), x, y, color, false);
    }

    private String trim(String value, int width) {
        return textRenderer.trimToWidth(value == null ? "" : value, Math.max(0, width));
    }

    private static int fade(int rgb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private boolean isInside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    private int mouseX() { return (int) client.mouse.getX() * width / client.getWindow().getWidth(); }
    private int mouseY() { return (int) client.mouse.getY() * height / client.getWindow().getHeight(); }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        calculateLayout();

        if (isInside(mouseX, mouseY, panelX + panelW - 46, panelY + 18, 32, 32)) {
            close();
            return true;
        }

        int navY = sideY + 35;
        if (isInside(mouseX, mouseY, sideX + 7, navY, sideW - 14, 34)) { tab = TAB_LIBRARY; scroll = 0; searchFocused = false; return true; }
        if (isInside(mouseX, mouseY, sideX + 7, navY + 40, sideW - 14, 34)) { tab = TAB_FAVORITES; scroll = 0; searchFocused = false; return true; }
        if (isInside(mouseX, mouseY, sideX + 7, navY + 80, sideW - 14, 34)) { tab = TAB_DISCOVER; scroll = 0; searchFocused = false; return true; }

        int addY = playerY - 57;
        if (isInside(mouseX, mouseY, sideX + 8, addY, sideW - 16, 34)) {
            searchFocused = false;
            MusicLibrary.chooseFolder(client);
            return true;
        }

        int searchY = contentY + 49;
        if (tab == TAB_DISCOVER && isInside(mouseX, mouseY, contentX + contentW - 72, searchY, 72, 31)) {
            MusicLibrary.searchOnline(searchInput, client);
            return true;
        }
        if (isInside(mouseX, mouseY, contentX, searchY, contentW - (tab == TAB_DISCOVER ? 82 : 0), 31)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        List<Track> visible = visibleTracks();
        int rowHeight = 46;
        int start = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - Math.max(0, (listBottom - listY) / rowHeight))));
        int rowIndex = (int) ((mouseY - listY) / rowHeight);
        int index = start + rowIndex;
        if (mouseY >= listY && mouseY < listBottom && index >= start && index < visible.size()) {
            Track track = visible.get(index);
            if (mouseX >= contentX + contentW - 50) {
                MusicLibrary.toggleFavorite(track);
            } else {
                MusicLibrary.playFrom(visible, index);
            }
            return true;
        }

        int controlsCenter = panelX + panelW / 2 + 10;
        int barY = playerY;
        if (isInside(mouseX, mouseY, controlsCenter - 50, barY + 9, 30, 34)) {
            MusicLibrary.previous();
            return true;
        }
        if (isInside(mouseX, mouseY, controlsCenter - 16, barY + 8, 38, 36)) {
            MusicLibrary.togglePlayback();
            return true;
        }
        if (isInside(mouseX, mouseY, controlsCenter + 25, barY + 9, 32, 34)) {
            MusicLibrary.next();
            return true;
        }
        if (panelW >= 540) {
            int sliderX = panelX + panelW - 145;
            if (isInside(mouseX, mouseY, sliderX - 3, barY + 13, 92, 24)) {
                updateVolume(mouseX, sliderX);
                draggingVolume = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingVolume && button == 0) {
            int sliderX = panelX + panelW - 145;
            updateVolume(mouseX, sliderX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingVolume = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateVolume(double mouseX, int sliderX) {
        AudioEngine.setVolume((float) Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / 84.0)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= listY && mouseY <= listBottom) {
            List<Track> visible = visibleTracks();
            int maxRows = Math.max(1, (listBottom - listY) / 46);
            scroll = Math.max(0, Math.min(Math.max(0, visible.size() - maxRows), scroll + (verticalAmount < 0 ? 2 : -2)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && searchFocused && tab == TAB_DISCOVER) {
            MusicLibrary.searchOnline(searchInput, client);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && searchFocused && !searchInput.isEmpty()) {
            searchInput = searchInput.substring(0, searchInput.length() - 1);
            scroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr) && searchInput.length() < 80) {
            searchInput += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
