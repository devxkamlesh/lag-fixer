package com.devxkamlesh.lagfixer.hud;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Keybind-toggled HUD overlay.
 *
 * Renders in the top-left corner:
 *   FPS | Tick avg/min/max ms | active category flags
 *
 * Registered in LagFixerMod.onInitializeClient() via HudElementRegistry.
 * Toggle keybind (F7) is also registered there.
 */
public final class HudOverlay implements HudElement {

    private static boolean visible = false;

    public static void toggle() { visible = !visible; }
    public static boolean isVisible() { return visible; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen()) return; // don't overlap F3 screen

        int fps    = mc.getFps();
        TickProfiler.Snapshot stats = TickProfiler.snapshot();
        String avg = String.format("%.2f", stats.avgMs());
        String p95 = String.format("%.2f", stats.p95Ms());
        String max = String.format("%.2f", stats.maxMs());

        String categories = buildCategoryLine();

        String line1 = String.format("§bLagFixer §7| §fFPS: §a%d", fps);
        String line2 = String.format("§7Tick: avg §e%s§7 p95 §6%s§7 max §c%s §7ms", avg, p95, max);
        String line3 = String.format("§7Active: %s", categories);

        int x = 6, y = 6, lineHeight = 11;
        int textW = Math.max(mc.font.width(line1), Math.max(mc.font.width(line2), mc.font.width(line3)));
        int panelW = textW + 14;
        int panelH = lineHeight * 3 + 8;

        // Modern background panel with accent indicator
        graphics.fill(x - 4, y - 4, x + panelW, y + panelH, 0xD00D1117);
        graphics.outline(x - 4, y - 4, panelW + 4, panelH + 4, 0xFF30363D);
        graphics.fill(x - 4, y - 4, x - 2, y + panelH, 0xFF58A6FF);

        // Lines rendered with 0xFFFFFFFF (full alpha) and drop shadow
        graphics.text(mc.font, line1, x + 4, y, 0xFFFFFFFF, true);
        graphics.text(mc.font, line2, x + 4, y + lineHeight, 0xFFFFFFFF, true);
        graphics.text(mc.font, line3, x + 4, y + lineHeight * 2, 0xFFFFFFFF, true);
    }

    private String buildCategoryLine() {
        StringBuilder sb = new StringBuilder();
        java.util.Map<String, com.devxkamlesh.lagfixer.mixin.MixinStatus.State> states =
            com.devxkamlesh.lagfixer.mixin.MixinStatus.snapshot();
        appendCat(sb, "HOP", isApplied(states, "HopperBlockEntityMixin"));
        appendCat(sb, "ENT", isApplied(states, "MobEntityMixin"));
        appendCat(sb, "RED", isApplied(states, "RedStoneWireBlockMixin"));
        appendCat(sb, "COL", isApplied(states, "AbstractBlockStateMixin"));
        appendCat(sb, "RND", LagFixerConfig.isRenderEnabled());
        return sb.toString();
    }

    private boolean isApplied(java.util.Map<String, com.devxkamlesh.lagfixer.mixin.MixinStatus.State> states,
                              String name) {
        com.devxkamlesh.lagfixer.mixin.MixinStatus.State state = states.get(name);
        return state != null && state.applied();
    }

    private void appendCat(StringBuilder sb, String label, boolean on) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(on ? "§a" : "§c").append(label);
    }
}
