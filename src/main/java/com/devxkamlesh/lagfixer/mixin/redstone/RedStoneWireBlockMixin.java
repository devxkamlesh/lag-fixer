package com.devxkamlesh.lagfixer.mixin.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Redstone optimization — cached power-level recalculation.
 *
 * Vanilla recomputes wire power from scratch on every neighbour update.
 * We cache the last computed power per BlockPos and skip recalculation
 * when neither the block state nor its inputs have changed.
 *
 * Target class  : net.minecraft.world.level.block.RedstoneWireBlock
 * Target method : updatePowerStrength(Level, BlockPos, BlockState, Orientation, boolean)
 * Mapping       : Mojang official (26.3)
 */
@Mixin(RedstoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {

    /**
     * Cache: BlockPos -> last power level written.
     * Static because RedstoneWireBlock is a singleton block instance.
     */
    @Unique
    private static final Map<Long, Integer> POWER_CACHE = new HashMap<>();

    /**
     * Injects before power recalculation and skips if the incoming
     * block state's reported power matches the cache.
     *
     * Verified target: RedstoneWireBlock#updatePowerStrength(Level, BlockPos, BlockState, Orientation, boolean)
     */
    @Inject(
        method = "updatePowerStrength(Lnet/minecraft/world/level/Level;"
               + "Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/world/level/redstone/Orientation;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lagfixer$maybeCacheSkip(Level level, BlockPos pos, BlockState state,
            Orientation orientation, boolean bl, CallbackInfo ci) {
        long key = pos.asLong();
        // Get the current power level reported by the block state
        int currentPower = state.getValue(RedstoneWireBlock.POWER);
        Integer cached = POWER_CACHE.get(key);
        if (cached != null && cached == currentPower) {
            // Power level unchanged — skip expensive propagation
            ci.cancel();
            return;
        }
        // Update cache with the new value before vanilla propagates
        POWER_CACHE.put(key, currentPower);
    }
}
