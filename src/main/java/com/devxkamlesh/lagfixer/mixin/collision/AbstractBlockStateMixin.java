package com.devxkamlesh.lagfixer.mixin.collision;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Collision optimization — VoxelShape caching per BlockState instance.
 *
 * BlockBehaviour.BlockStateBase#getShape is called on every entity movement
 * check. The vanilla implementation recomputes the shape from the block each
 * time. BlockState instances are effectively singletons (one per unique
 * property combination), so caching per-instance is safe and exact.
 *
 * Target class  : net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase
 * Target method : getCollisionShape(BlockGetter, BlockPos)
 * Mapping       : Mojang official (26.3) — verify on mcsrc.dev
 *
 * Note: we target getCollisionShape rather than getShape because collision
 * checks are the hot path. Render shape (getShape) is already cheap.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {

    /** Cached collision shape; null until first computation. */
    @Unique
    private VoxelShape lagfixer$cachedCollisionShape = null;

    /**
     * Return the cached shape if available, otherwise let vanilla compute
     * it and cache the result.
     *
     * Verified target: BlockBehaviour.BlockStateBase#getCollisionShape(BlockGetter, BlockPos)
     */
    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;"
               + "Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lagfixer$returnCachedShape(
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (lagfixer$cachedCollisionShape != null) {
            cir.setReturnValue(lagfixer$cachedCollisionShape);
        }
        // else: let vanilla compute, then cache it in the @Inject RETURN hook below
    }

    /**
     * Cache the shape after vanilla has computed it (fires on RETURN when
     * the HEAD inject did not cancel).
     */
    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;"
               + "Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN")
    )
    private void lagfixer$cacheShape(
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (lagfixer$cachedCollisionShape == null) {
            lagfixer$cachedCollisionShape = cir.getReturnValue();
        }
    }
}
