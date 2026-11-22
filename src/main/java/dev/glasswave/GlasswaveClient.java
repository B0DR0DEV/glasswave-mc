package dev.glasswave;

import dev.glasswave.library.MusicLibrary;
import dev.glasswave.ui.GlasswaveScreen;
import dev.glasswave.ui.GlasswaveWidget;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class GlasswaveClient implements ClientModInitializer {
    public static final String MOD_ID = "glasswave";
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        MusicLibrary.initialize();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.glasswave.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyBinding.MISC_CATEGORY
        ));

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
                layeredDrawer.attachLayerAfter(
                        IdentifiedLayer.MISC_OVERLAYS,
                        Identifier.of(MOD_ID, "floating_player"),
                        GlasswaveWidget::render
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new GlasswaveScreen());
                }
            }
            MusicLibrary.onClientTick(client);
        });
    }
}
