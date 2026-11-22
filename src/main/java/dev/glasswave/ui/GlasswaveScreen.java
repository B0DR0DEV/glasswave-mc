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

        double t = Math.min(1.0, (System.currentTimeMillis() - openedAt) / 360.0);
        float ease = (float) (1.0 - Math.pow(1.0 - t, 3.0));
        int textAlpha = (int) (255 * ease);
        int y = panelY + (int) ((1.0f - ease) * 10.0f);

        // Frosted surface: the game scene remains visible through the panel.
        GlasswaveWidget.rounded(context, panelX - 5, y + 7, panelW + 10, panelH + 6, 29,
                fade(0x030713, (int) (52 * ease)));
        GlasswaveWidget.rounded(context, panelX - 1, y - 1, panelW + 2, panelH + 2, 27,
                fade(0xD7F0F3, (int) (44 * ease)));
        GlasswaveWidget.rounded(context, panelX, y, panelW, panelH, 26,
                fade(0x0B1728, (int) (82 * ease)));
        GlasswaveWidget.rounded(context, panelX + 1, y + 1, panelW - 2, panelH - 2, 25,
                fade(0x25435A, (int) (38 * ease)));

        // Soft reflections on the upper edge and the right side of the glass.
        GlasswaveWidget.rounded(context, panelX + 3, y + 2, panelW - 6, 1, 1,
                fade(0xFFFFFF, (int) (66 * ease)));
        GlasswaveWidget.rounded(context, panelX + 18, y + 3, 245, 2, 1,
                fade(0x9FE4E0, (int) (30 * ease)));
        GlasswaveWidget.rounded(context, panelX + panelW - 98, y + 28, 54, panelH - 56, 24,
                fade(0x9ECAD8, (int) (8 * ease)));

        drawHeader(context, y, textAlpha);
        drawSidebar(context, y, mouseX, mouseY, textAlpha);
        drawContent(context, y, mouseX, mouseY, textAlpha);
        drawPlayerBar(context, y, mouseX, mouseY, textAlpha);

        super.render(context, mouseX, mouseY, delta);
    }

    private void calculateLayout() {
        panelW = Math.min(790, Math.max(360, width - 32));
        panelH = Math.min(500, Math.max(320, height - 28));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sideX = panelX + 22;
        sideY = panelY + 88;
        sideW = Math.min(150, panelW / 4);
        contentX = sideX + sideW + 22;
        contentY = sideY;
        contentW = panelX + panelW - 24 - contentX;
        playerY = panelY + panelH - 78;
        listY = contentY + 105;
        listBottom = playerY - 18;
    }

    private void drawHeader(DrawContext context, int y, int alpha) {
        int markX = panelX + 22;
        int markY = y + 22;
        GlasswaveWidget.rounded(context, markX, markY, 38, 38, 14, fade(0x8CDAD6, (int) (42 * alpha / 255.0)));
        GlasswaveWidget.rounded(context, markX + 1, markY + 1, 36, 36, 13, fade(0x122A3A, (int) (110 * alpha / 255.0)));
        GlasswaveWidget.drawWaveMark(context, markX + 9, markY + 9, fade(0xB6F1E8, alpha));

        drawText(context, "GLASSWAVE", panelX + 72, y + 25, fade(0xF4FAFF, alpha), true);
        drawText(context, "MUSIC PLAYER", panelX + 73, y + 42, fade(0x91AEBE, alpha), false);

        int closeX = panelX + panelW - 51;
        boolean hovered = isInside(mouseX(), mouseY(), closeX, y + 23, 29, 29);
        GlasswaveWidget.rounded(context, closeX, y + 23, 29, 29, 15,
                fade(hovered ? 0xFFFFFF : 0xDDEAF0, hovered ? 46 : 20));
        GlasswaveWidget.drawCloseIcon(context, closeX + 9, y + 32, fade(0xEAF4F8, alpha));
    }

    private void drawSidebar(DrawContext context, int y, int mouseX, int mouseY, int alpha) {
        int offset = y - panelY;
        int navX = sideX + 5;
        int navW = sideW - 10;

        // A divider instead of another opaque card keeps the whole surface light.
        context.fill(sideX + sideW + 10, sideY + offset - 6, sideX + sideW + 11,
                playerY - 16 + offset, fade(0xFFFFFF, (int) (24 * alpha / 255.0)));
        drawText(context, "РАЗДЕЛЫ", sideX + 11, sideY + 4 + offset,
                fade(0x8EA8B9, alpha), true);

        drawNavItem(context, TAB_LIBRARY, "Медиатека", sideY + 28 + offset, mouseX, mouseY, alpha);
        drawNavItem(context, TAB_FAVORITES, "Избранное", sideY + 70 + offset, mouseX, mouseY, alpha);
        drawNavItem(context, TAB_DISCOVER, "Онлайн-поиск", sideY + 112 + offset, mouseX, mouseY, alpha);

        int addY = playerY - 62 + offset;
        boolean addHover = isInside(mouseX, mouseY, navX, addY, navW, 38);
        GlasswaveWidget.rounded(context, navX, addY, navW, 38, 13,
                fade(addHover ? 0x80DCD9 : 0xB7E6E4, addHover ? 54 : 34));
        GlasswaveWidget.rounded(context, navX + 1, addY + 1, navW - 2, 1, 1,
                fade(0xFFFFFF, 58));
        GlasswaveWidget.drawFolderIcon(context, sideX + 15, addY + 12, fade(0xC9F5EF, alpha));
        drawText(context, "Добавить папку", sideX + 38, addY + 14,
                fade(0xE5F3F6, alpha), true);

        drawText(context, "ЛОКАЛЬНАЯ МУЗЫКА", sideX + 11, playerY - 17 + offset,
                fade(0x718B9E, alpha), true);
    }

    private void drawNavItem(DrawContext context, int itemTab, String label, int itemY,
                             int mouseX, int mouseY, int alpha) {
        boolean selected = tab == itemTab;
        int itemX = sideX + 5;
        int itemW = sideW - 10;
        boolean hovered = isInside(mouseX, mouseY, itemX, itemY, itemW, 36);

        if (selected) {
            GlasswaveWidget.rounded(context, itemX, itemY, itemW, 36, 13,
                    fade(0x91DCD8, (int) (43 * alpha / 255.0)));
            GlasswaveWidget.rounded(context, itemX + 1, itemY + 1, 2, 34, 1,
                    fade(0xA7F2E8, alpha));
            GlasswaveWidget.rounded(context, itemX + 8, itemY + 1, itemW - 16, 1, 1,
                    fade(0xFFFFFF, (int) (26 * alpha / 255.0)));
        } else if (hovered) {
            GlasswaveWidget.rounded(context, itemX, itemY, itemW, 36, 13,
                    fade(0xFFFFFF, (int) (20 * alpha / 255.0)));
        }

        int iconX = sideX + 16;
        int iconY = itemY + 11;
        int iconColor = fade(selected ? 0xB8F4E9 : 0x9FB5C3, alpha);
        if (itemTab == TAB_LIBRARY) GlasswaveWidget.drawLibraryIcon(context, iconX, iconY, iconColor);
        else if (itemTab == TAB_FAVORITES) GlasswaveWidget.drawHeartIcon(context, iconX, iconY, iconColor);
        else GlasswaveWidget.drawSearchIcon(context, iconX, iconY, iconColor);

        drawText(context, label, sideX + 43, itemY + 12,
                fade(selected ? 0xF4FBFF : 0xB6C7D2, alpha), selected);
    }

    private void drawContent(DrawContext context, int y, int mouseX, int mouseY, int alpha) {
        int offset = y - panelY;
        String heading = switch (tab) {
            case TAB_FAVORITES -> "Избранное";
            case TAB_DISCOVER -> "Найти музыку";
            default -> "Медиатека";
        };
        String subtitle = switch (tab) {
            case TAB_FAVORITES -> "Треки, которые ты сохранил";
            case TAB_DISCOVER -> "Поиск коротких превью";
            default -> "Музыка с твоего устройства";
        };

        drawText(context, heading, contentX + 2, contentY + 3 + offset,
                fade(0xF4F9FD, alpha), true);
        drawText(context, subtitle, contentX + 2, contentY + 21 + offset,
                fade(0x9AB0BF, alpha), false);

        int searchY = contentY + 51 + offset;
        int searchW = contentW - (tab == TAB_DISCOVER ? 86 : 0);
        GlasswaveWidget.rounded(context, contentX, searchY, searchW, 33, 14,
                fade(0x071321, (int) (74 * alpha / 255.0)));
        GlasswaveWidget.rounded(context, contentX + 1, searchY + 1, searchW - 2, 1, 1,
                fade(0xFFFFFF, (int) (44 * alpha / 255.0)));
        GlasswaveWidget.rounded(context, contentX, searchY, searchW, 33, 14,
                fade(0xD5EFF2, (int) (22 * alpha / 255.0)));
        GlasswaveWidget.drawSearchIcon(context, contentX + 12, searchY + 10, fade(0x8FAFBF, alpha));

        String placeholder = tab == TAB_DISCOVER ? "Артист или название трека…" : "Поиск в коллекции…";
        String shownQuery = searchInput.isEmpty() ? placeholder : searchInput;
        drawText(context, trim(shownQuery, searchW - 48), contentX + 34, searchY + 12,
                fade(searchInput.isEmpty() ? 0x8299A9 : 0xE8F2F7, alpha), false);

        if (tab == TAB_DISCOVER) {
            int buttonX = contentX + contentW - 76;
            boolean hovered = isInside(mouseX, mouseY, buttonX, searchY, 76, 33);
            GlasswaveWidget.rounded(context, buttonX, searchY, 76, 33, 14,
                    fade(hovered ? 0x76CCC6 : 0x5DADAD, (int) (145 * alpha / 255.0)));
            GlasswaveWidget.rounded(context, buttonX + 1, searchY + 1, 74, 1, 1,
                    fade(0xFFFFFF, (int) (66 * alpha / 255.0)));
            drawText(context, MusicLibrary.isSearching() ? "…" : "Найти",
                    buttonX + 22, searchY + 12, fade(0xF4FFFF, alpha), true);
        }

        int listTop = listY + offset;
        int bottom = listBottom + offset;
        GlasswaveWidget.rounded(context, contentX, listTop - 7, contentW,
                Math.max(48, bottom - listTop + 7), 17,
                fade(0x081522, (int) (22 * alpha / 255.0)));

        List<Track> visible = visibleTracks();
        int rowHeight = 46;
        int maxRows = Math.max(0, (bottom - listTop) / rowHeight);
        if (visible.isEmpty()) {
            String empty = tab == TAB_DISCOVER
                    ? (MusicLibrary.isSearching() ? "Ищем треки…" : "Введи запрос и нажми «Найти»")
                    : (tab == TAB_FAVORITES ? "Пока нет избранных треков" : "Добавь папку с музыкой");
            drawText(context, empty, contentX + 18, listTop + 18,
                    fade(0x91A7B7, alpha), false);
        } else {
            int start = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - maxRows)));
            for (int index = start; index < Math.min(visible.size(), start + maxRows); index++) {
                Track track = visible.get(index);
                int rowIndex = index - start;
                drawTrackRow(context, track, index, listTop + rowIndex * rowHeight,
                        mouseX, mouseY, alpha);
            }
        }

        String footer = MusicLibrary.status();
        drawText(context, trim(footer, contentW - 8), contentX + 4, playerY - 15 + offset,
                fade(0x8299A9, alpha), false);
    }

    private void drawTrackRow(DrawContext context, Track track, int index, int rowY,
                              int mouseX, int mouseY, int alpha) {
        boolean hovered = isInside(mouseX, mouseY, contentX + 3, rowY, contentW - 6, 42);
        if (hovered) {
            GlasswaveWidget.rounded(context, contentX + 3, rowY, contentW - 6, 42, 13,
                    fade(0xD9F3F2, (int) (24 * alpha / 255.0)));
            GlasswaveWidget.rounded(context, contentX + 12, rowY + 1, contentW - 24, 1, 1,
                    fade(0xFFFFFF, (int) (22 * alpha / 255.0)));
        }

        CoverArtManager.draw(context, track, contentX + 12, rowY + 6, 30);
        GlasswaveWidget.rounded(context, contentX + 11, rowY + 5, 32, 32, 10,
                fade(0xFFFFFF, (int) (34 * alpha / 255.0)));

        int textX = contentX + 53;
        int textWidth = Math.max(55, contentW - 132);
        drawText(context, trim(track.title, textWidth), textX, rowY + 8,
                fade(0xEDF5FA, alpha), true);
        String metadata = "deezer".equals(track.source) ? track.artist + " · превью" : track.artist;
        drawText(context, trim(metadata, textWidth), textX, rowY + 24,
                fade(0x91A7B6, alpha), false);

        int heartX = contentX + contentW - 35;
        GlasswaveWidget.drawHeartIcon(context, heartX, rowY + 14,
                fade(isFavorite(track) ? 0xE8A9C4 : 0x91A9B8, alpha));

        Track active = MusicLibrary.currentTrack();
        boolean thisTrackPlaying = AudioEngine.isPlaying() && active != null && active.id.equals(track.id);
        if (thisTrackPlaying) {
            CoverArtManager.drawEqualizer(context, contentX + contentW - 70,
                    rowY + 18, 4, 13, true, fade(0xA2E8DF, alpha));
        }
    }

    private void drawPlayerBar(DrawContext context, int y, int mouseX, int mouseY, int alpha) {
        int barX = panelX + 17;
        int barY = playerY + (y - panelY);
        int barW = panelW - 34;

        GlasswaveWidget.rounded(context, barX - 1, barY - 1, barW + 2, 60, 19,
                fade(0xD8F1F2, (int) (36 * alpha / 255.0)));
        GlasswaveWidget.rounded(context, barX, barY, barW, 58, 18,
                fade(0x091625, (int) (112 * alpha / 255.0)));
        GlasswaveWidget.rounded(context, barX + 1, barY + 1, barW - 2, 1, 1,
                fade(0xFFFFFF, (int) (68 * alpha / 255.0)));
        GlasswaveWidget.rounded(context, barX + 12, barY + 2, 130, 1, 1,
                fade(0x9FE4DF, (int) (34 * alpha / 255.0)));

        Track current = MusicLibrary.currentTrack();
        CoverArtManager.draw(context, current, barX + 12, barY + 12, 34);
        GlasswaveWidget.rounded(context, barX + 11, barY + 11, 36, 36, 12,
                fade(0xFFFFFF, (int) (38 * alpha / 255.0)));

        String currentTitle = current == null ? "Ничего не играет" : current.title;
        String currentArtist = current == null ? "Выбери трек в медиатеке" : current.artist;
        drawText(context, trim(currentTitle, Math.max(70, barW / 3 - 44)), barX + 58, barY + 15,
                fade(0xF2F8FC, alpha), true);
        drawText(context, trim(currentArtist, Math.max(70, barW / 3 - 44)), barX + 58, barY + 31,
                fade(0x9CB2C0, alpha), false);
        CoverArtManager.drawEqualizer(context, barX + 58, barY + 46, 6, 7,
                AudioEngine.isPlaying(), fade(0x93DCD5, alpha));

        int controlsCenter = panelX + panelW / 2 + 10;
        drawControl(context, 0, controlsCenter - 48, barY + 16, 26, mouseX, mouseY, alpha);
        drawControl(context, 1, controlsCenter - 13, barY + 13, 32, mouseX, mouseY, alpha);
        drawControl(context, 2, controlsCenter + 28, barY + 16, 26, mouseX, mouseY, alpha);

        if (panelW >= 560) {
            int sliderX = panelX + panelW - 143;
            int sliderY = barY + 29;
            GlasswaveWidget.drawVolumeIcon(context, sliderX - 22, barY + 22, fade(0x90A8B8, alpha));
            GlasswaveWidget.rounded(context, sliderX, sliderY, 84, 4, 2,
                    fade(0xFFFFFF, (int) (62 * alpha / 255.0)));
            int fill = (int) (84 * AudioEngine.getVolume());
            GlasswaveWidget.rounded(context, sliderX, sliderY, Math.max(3, fill), 4, 2,
                    fade(0x9AE0D8, alpha));
            GlasswaveWidget.rounded(context, sliderX + fill - 4, sliderY - 4, 11, 11, 6,
                    fade(0xE9FAF7, alpha));
        }
    }

    private void drawControl(DrawContext context, int action, int x, int y, int size,
                             int mouseX, int mouseY, int alpha) {
        boolean hovered = isInside(mouseX, mouseY, x, y, size, size);
        GlasswaveWidget.rounded(context, x, y, size, size, size / 2,
                fade(hovered ? 0xA3E8E0 : 0xD6EAF0, hovered ? 64 : 30));
        if (action == 0) {
            GlasswaveWidget.drawSkipIcon(context, x + 7, y + 8, false, fade(0xF0FAFC, alpha));
        } else if (action == 1) {
            if (AudioEngine.isPlaying()) GlasswaveWidget.drawPauseIcon(context, x + 11, y + 10, fade(0xF4FCFF, alpha));
            else GlasswaveWidget.drawPlayIcon(context, x + 11, y + 10, fade(0xF4FCFF, alpha));
        } else {
            GlasswaveWidget.drawSkipIcon(context, x + 8, y + 8, true, fade(0xF0FAFC, alpha));
        }
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

        int navY = sideY + 28;
        if (isInside(mouseX, mouseY, sideX + 7, navY, sideW - 10, 36)) { tab = TAB_LIBRARY; scroll = 0; searchFocused = false; return true; }
        if (isInside(mouseX, mouseY, sideX + 7, navY + 40, sideW - 10, 36)) { tab = TAB_FAVORITES; scroll = 0; searchFocused = false; return true; }
        if (isInside(mouseX, mouseY, sideX + 7, navY + 80, sideW - 10, 36)) { tab = TAB_DISCOVER; scroll = 0; searchFocused = false; return true; }

        int addY = playerY - 62;
        if (isInside(mouseX, mouseY, sideX + 8, addY, sideW - 16, 34)) {
            searchFocused = false;
            MusicLibrary.chooseFolder(client);
            return true;
        }

        int searchY = contentY + 51;
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
