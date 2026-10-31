package com.devxkamlesh.lagfixer.mixin.render;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.performance.PerformanceController;
import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

/**
 * Render optimization — particle count cap.
 *
 * Injects into ParticleEngine to enforce the configured particle limit.
 * Incoming particles are capped via add() and pending queues trimmed during tick().
 *
 * Target class : net.minecraft.client.particle.ParticleEngine
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

    @Shadow
    private Queue<Particle> particlesToAdd;

    /**
     * Drop incoming particles if the pending queue exceeds the configured cap.
     */
    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void lagfixer$capParticlesOnAdd(Particle particle, CallbackInfo ci) {
        int cap = PerformanceController.effectiveParticleCap();
        if (particlesToAdd != null && particlesToAdd.size() >= cap) {
            // Vanilla-limited particle groups represent important/controlled effects.
            if (particle.getParticleLimit().isPresent() && particlesToAdd.size() < cap + 64) {
                ClientTelemetry.particleAccepted();
                return;
            }
            ClientTelemetry.particleDropped();
            ci.cancel();
        } else {
            ClientTelemetry.particleAccepted();
        }
    }

    /**
     * Trim the pending particle queue to the configured cap before ticking.
     */
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void lagfixer$capParticles(CallbackInfo ci) {
        int cap = PerformanceController.effectiveParticleCap();
        while (particlesToAdd != null && particlesToAdd.size() > cap) {
            particlesToAdd.poll();
        }
    }
}
