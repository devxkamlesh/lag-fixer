package com.devxkamlesh.lagfixer.mixin.entity;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity/AI optimization — skip idle-mob AI every N ticks.
 *
 * Injects into Mob#customServerAiStep, which is where per-tick goal/brain
 * evaluation happens. For mobs that are far from all players and have no
 * active navigation target, we skip the method every aiSkipTicks ticks.
 *
 * Target class  : net.minecraft.world.entity.Mob
 * Target method : customServerAiStep(ServerLevel)
 * Mapping       : Mojang official (26.3) — verified via mcsrc.dev
 *
 * ponytail: flat N-tick skip for all idle mobs. Ceiling: mobs that need
 *   frame-perfect reaction (e.g. raid waves) may feel slightly sluggish.
 *   Upgrade path: bucket by distance-to-nearest-player; closer = smaller N.
 */
@Mixin(Mob.class)
public abstract class MobEntityMixin {

    /** Counts down to next permitted AI evaluation. */
    @Unique
    private int lagfixer$aiSkipCounter = 0;

    /**
     * Skip AI evaluation for idle mobs that are distant from all players.
     * Verified target: Mob#customServerAiStep(ServerLevel)
     */
    @Inject(
        method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lagfixer$maybeSkipAi(ServerLevel level, CallbackInfo ci) {
        Mob self = (Mob)(Object) this;

        // Always run AI for mobs within 24 blocks of a player
        double nearestPlayerDist = level.getNearestPlayer(self, -1.0) instanceof Player p
            ? self.distanceTo(p) : Double.MAX_VALUE;

        if (nearestPlayerDist < 24.0) {
            lagfixer$aiSkipCounter = 0;
            return; // don't cancel — let vanilla AI run
        }

        // Mob is distant — apply skip interval
        if (lagfixer$aiSkipCounter > 0) {
            lagfixer$aiSkipCounter--;
            ci.cancel();
            return;
        }

        // Reset counter from config (default 4 → AI runs every 5th tick)
        lagfixer$aiSkipCounter = LagFixerConfig.getAiSkipTicks();
    }
}
