# Glasswave — floating glass music player for Minecraft

A client-side Fabric music player for **Minecraft 1.21.4**. Glasswave is designed as a soft, translucent floating glass widget, not a square vanilla-style menu.

## Version 0.2.0
- Floating mini-player with layered glass, ambient glow, reflective edge and animated progress line.
- Drag the thin top edge of the mini-player to move it. Its coordinates persist in `.minecraft/glasswave/hud.json`.
- Album art for Deezer search results, loaded asynchronously and cached as Minecraft dynamic textures. Local tracks get generated cover art and are never uploaded.
- Animated equalizer bars in the mini-player, track list and playback bar.
- Full glass library window with local library, favorites and online catalog tabs.
- Scan a folder recursively or use `.minecraft/glasswave/music`.
- MP3 and WAV playback (AIFF/AIF/AU where Java Sound providers support them).
- Search/filter local tracks, favorites, previous/next, play/pause and volume control.
- Deezer search streams only provider-supplied short previews. No full-track downloading.
- Client-side playback only; audio is not sent to servers or other players.

## Target stack
Minecraft 1.21.4 · Java 21 · Fabric Loader 0.16.10 · Fabric API 0.119.4+1.21.4 · Yarn 1.21.4+build.8 · Loom 1.10.5 · Gradle 8.12.

## Install
1. Install Fabric Loader and Fabric API for Minecraft 1.21.4.
2. Download the `glasswave-0.2.0.jar` artifact from **Actions → Build Glasswave**.
3. Put the JAR into the `mods` folder for the 1.21.4 profile.
4. Press **P** to open the full player. Click the floating widget to open it, or click its play button to toggle playback.
5. Drag the widget by its narrow top edge to place it where you like.

Local music goes in `.minecraft/glasswave/music/`, or choose another folder in the UI. Favorites and selected folders are stored in `glasswave/library.json`; the widget position is stored in `glasswave/hud.json`.

## Notes
- Album artwork is loaded only from Deezer's image CDN and limited to 2 MiB per image and 2048×2048 pixels.
- Local filenames and local audio data stay on the computer; no local files are uploaded.
- Online preview/catalog availability depends on the provider and region.
- This is a development build. GitHub Actions verifies compilation and packaging; in-game testing across different screen sizes is still recommended.
