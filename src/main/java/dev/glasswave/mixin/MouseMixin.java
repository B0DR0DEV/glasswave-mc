package dev.glasswave.mixin;

import dev.glasswave.ui.GlasswaveWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void glasswave$openFloatingPlayer(long window, int button, int action, int mods, CallbackInfo ci) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_1 || action != GLFW.GLFW_PRESS) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) return;

        Window gameWindow = client.getWindow();
        Mouse mouse = (Mouse) (Object) this;
        int x = (int) (mouse.getX() * gameWindow.getScaledWidth() / gameWindow.getWidth());
        int y = (int) (mouse.getY() * gameWindow.getScaledHeight() / gameWindow.getHeight());
        if (GlasswaveWidget.contains(x, y, gameWindow.getScaledWidth(), gameWindow.getScaledHeight())) {
            GlasswaveWidget.onClick(x, y, client);
            ci.cancel();
        }
    }
}
