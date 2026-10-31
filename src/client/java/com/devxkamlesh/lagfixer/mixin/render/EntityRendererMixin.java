package com.devxkamlesh.lagfixer.mixin.render;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Render-only entity distance controls; simulation and server tracking are untouched. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDDF)Z",
        at = @At("HEAD"), cancellable = true)
    private void lagfixer$distanceCull(Entity entity, Frustum frustum, double cameraX, double cameraY,
                                      double cameraZ, float partialTick, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player || entity == minecraft.getCameraEntity() || entity.hasCustomName()
            || minecraft.shouldEntityAppearGlowing(entity)) return;
        int cap = categoryDistance(entity);
        if (cap == Integer.MAX_VALUE) return;
        double dx = entity.getX() - cameraX, dy = entity.getY() - cameraY, dz = entity.getZ() - cameraZ;
        if (dx * dx + dy * dy + dz * dz > (double) cap * cap) cir.setReturnValue(false);
    }

    private static int categoryDistance(Entity entity) {
        if (entity instanceof ItemEntity) return LagFixerConfig.getItemRenderDistance();
        if (entity instanceof HangingEntity || entity instanceof ArmorStand || entity instanceof Display)
            return LagFixerConfig.getDecorationRenderDistance();
        if (entity instanceof Monster) return LagFixerConfig.getMonsterRenderDistance();
        if (entity instanceof Animal) return LagFixerConfig.getAnimalRenderDistance();
        return Integer.MAX_VALUE;
    }
}
