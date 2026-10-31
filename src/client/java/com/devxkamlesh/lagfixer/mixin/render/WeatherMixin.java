package com.devxkamlesh.lagfixer.mixin.render;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reduces cosmetic weather effects only during measured client pressure. */
@Mixin(ClientLevel.class)
public abstract class WeatherMixin {
    @Inject(method = "tickWeatherEffects()V", at = @At("HEAD"), cancellable = true)
    private void lagfixer$reduceWeather(CallbackInfo ci) {
        if (LagFixerConfig.isWeatherReductionEnabled() && ClientTelemetry.snapshot().p95Ms() > 33.3) {
            ci.cancel();
        }
    }
}
