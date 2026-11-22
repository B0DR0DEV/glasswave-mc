# Glasswave — floating glass music player for Minecraft

A client-side Fabric mod for **Minecraft 1.21.4**. The idea is a soft, floating glass music surface rather than another blocky Minecraft settings screen.

## Features
- Floating mini-player in the HUD. Click its glass surface to open the full player; the small play control toggles playback.
- Full-screen animated glass interface with rounded panels, soft highlights, low-contrast surfaces and a compact playback bar.
- Local music library: scan a folder (including subfolders) or use `.minecraft/glasswave/music`.
- MP3 and WAV playback (AIFF/AIF/AU where Java Sound providers support them).
- Search/filter local tracks, favorites, previous/next, play/pause and volume control.
- Optional Deezer catalog search. Plays only provider-supplied short previews; no full-track downloader.
- Client-side playback only; audio is not sent to servers or other players.
- GitHub Actions builds the remapped Fabric JAR for Minecraft 1.21.4.

## Target stack
Minecraft 1.21.4 · Java 21 · Fabric Loader 0.16.10 · Fabric API 0.119.4+1.21.4 · Yarn 1.21.4+build.8 · Loom 1.10.5 · Gradle 8.12.

## Install
1. Install Fabric Loader and Fabric API for Minecraft 1.21.4.
2. Download the `glasswave-*.jar` artifact from **Actions → Build Glasswave**.
3. Put the JAR into the `mods` folder for the 1.21.4 profile.
4. Press **P** to open the player; click the floating HUD player to open it.

Local music goes in `.minecraft/glasswave/music/`, or choose another folder in the UI. Favorites and folder settings are stored in `glasswave/library.json`.

This is an initial development build. In-game QA is still required; see the Actions log for build status. Deezer search depends on provider API availability. No YouTube scraping or downloading of full copyrighted tracks is included.
