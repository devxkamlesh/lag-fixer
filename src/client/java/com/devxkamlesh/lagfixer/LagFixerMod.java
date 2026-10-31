package com.devxkamlesh.lagfixer;

import com.devxkamlesh.lagfixer.command.ProfileCommand;
import com.devxkamlesh.lagfixer.hud.HudOverlay;
import com.devxkamlesh.lagfixer.gui.LagFixerDashboardScreen;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import com.devxkamlesh.lagfixer.performance.PerformanceController;
import com.devxkamlesh.lagfixer.benchmark.BenchmarkManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side mod initializer for Lag Fixer.
 *
 * Registers:
 *   - F7 keybind to toggle the HUD overlay
 *   - HudElement for the overlay
 *   - ClientTickEvents for TickProfiler begin/end
 *   - /lagfixer client command
 *
 * Config is loaded earlier by LagFixerMixinPlugin.onLoad() — no need to
 * load it here. This class only wires up the runtime UI/events.
 */
public final class LagFixerMod implements ClientModInitializer {

    public static final String MOD_ID = "lagfixer";
    public static final Logger LOGGER = LoggerFactory.getLogger("LagFixer");
    public static KeyMapping hudKey;
    public static KeyMapping dashboardKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[LagFixer] Initializing client…");

        // ── HUD overlay ───────────────────────────────────────────────────
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(MOD_ID, "hud_overlay"),
            new HudOverlay()
        );

        // ── Keybinds ──────────────────────────────────────────────────────
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "category")
        );
        hudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "lagfixer.key.toggleHud",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_F7,
            category
        ));
        dashboardKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "lagfixer.key.openDashboard",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_F8,
            category
        ));

        // Poll keybind state each client tick
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (hudKey.consumeClick()) {
                HudOverlay.toggle();
            }
            while (dashboardKey.consumeClick()) {
                if (mc.gui.screen() instanceof LagFixerDashboardScreen) {
                    mc.gui.setScreen(null);
                } else {
                    mc.gui.setScreen(new LagFixerDashboardScreen(mc.gui.screen()));
                }
            }
            PerformanceController.tick(mc);
            BenchmarkManager.tick();
        });

        // ── Tick profiler hooks ───────────────────────────────────────────
        // We hook the server tick (runs inside the integrated server on the
        // client thread in singleplayer / LAN).
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK
            .register(server -> TickProfiler.beginTick());

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
            .register(server -> TickProfiler.endTick());

        // ── /lagfixer command ─────────────────────────────────────────────
        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) -> ProfileCommand.register(dispatcher)
        );

        LOGGER.info("[LagFixer] Client ready.");
    }
}
