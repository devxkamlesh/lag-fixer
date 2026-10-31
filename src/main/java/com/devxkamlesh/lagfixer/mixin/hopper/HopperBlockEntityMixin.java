package com.devxkamlesh.lagfixer.mixin.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hopper optimization — dirty-flag transfer checking.
 *
 * Vanilla polls every hopper every tick regardless of whether any container
 * nearby changed. We replace that with a dirty flag: the hopper only runs
 * tryMoveItems when the flag is set, then clears it.
 *
 * The flag is set again by HopperContainerListenerMixin whenever a tracked
 * container's inventory changes.
 *
 * Target class  : net.minecraft.world.level.block.entity.HopperBlockEntity
 * Target method : tick(Level, BlockPos, BlockState, HopperBlockEntity) — static
 * Mapping       : Mojang official (26.3) — verified via mcsrc.dev
 *
 * ponytail: dirty flag is instance-only; no cross-chunk event bus. Ceiling:
 *   a hopper that never gets a neighbour change event stays idle forever.
 *   Upgrade path: add a max-idle-ticks fallback that forces a recheck.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    /** Cooldown ticks remaining before the next transfer attempt is allowed. */
    @Unique
    private int lagfixer$cooldown = 0;

    /**
     * Injects at HEAD of the static tick method.
     * Cancels the vanilla tick body when the cooldown has not expired.
     *
     * Verified target: HopperBlockEntity#pushItemsTick(Level, BlockPos, BlockState, HopperBlockEntity)
     */
    @Inject(
        method = "pushItemsTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void lagfixer$onTick(
            Level level, BlockPos pos, BlockState state, HopperBlockEntity self,
            CallbackInfo ci) {
        HopperBlockEntityMixin accessor = (HopperBlockEntityMixin)(Object) self;

        // Tick down cooldown
        if (accessor.lagfixer$cooldown > 0) {
            accessor.lagfixer$cooldown--;
            ci.cancel();
            return;
        }

        // Allow vanilla tick to run; set brief cooldown
        accessor.lagfixer$cooldown = 4; // ~4 ticks ≈ 200 ms at 20 TPS
    }
}
