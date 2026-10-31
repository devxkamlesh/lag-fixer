package com.devxkamlesh.lagfixer.mixin.render;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.performance.PerformanceController;
import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render optimization — render distance cap.
 *
 * Injects into GameRenderer#renderLevel so we can clamp the render distance
 * option to the configured cap each frame. Writing to options.renderDistance
 * rather than the renderer field means the vanilla Options screen still works
 * (we just prevent the player from going over the cap).
 *
 * Target class  : net.minecraft.client.renderer.GameRenderer
 * Target method : renderLevel(float, long, PoseStack)
 * Mapping       : Mojang official (26.3) — verify on mcsrc.dev
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render()V", at = @At("HEAD"))
    private void lagfixer$beginFrame(CallbackInfo ci) {
        ClientTelemetry.beginFrame();
    }

    @Inject(method = "render()V", at = @At("RETURN"))
    private void lagfixer$endFrame(CallbackInfo ci) {
        ClientTelemetry.endFrame();
    }

    /**
     * Clamp options.renderDistance to the cap before the frame is rendered.
     * Target: GameRenderer#renderLevel()
     */
    @Inject(
        method = "renderLevel()V",
        at = @At("HEAD")
    )
    private void lagfixer$clampRenderDistance(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        int cap = PerformanceController.effectiveRenderDistance();
        if (mc.options.renderDistance().get() > cap) {
            mc.options.renderDistance().set(cap);
        }
    }
}
