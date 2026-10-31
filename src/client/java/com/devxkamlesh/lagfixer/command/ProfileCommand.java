package com.devxkamlesh.lagfixer.command;

import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import com.devxkamlesh.lagfixer.mixin.MixinStatus;
import com.devxkamlesh.lagfixer.gui.LagFixerDashboardScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * /lagfixer profile — dumps the tick profiler window to a timestamped CSV
 * in .minecraft/config/lagfixer-profiles/ and prints a summary to chat.
 *
 * /lagfixer hud — toggles the HUD overlay.
 *
 * Registered as a client-side command in LagFixerMod.onInitializeClient().
 */
public final class ProfileCommand {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ProfileCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommands.literal("lagfixer")
                .then(ClientCommands.literal("profile")
                    .executes(ProfileCommand::runProfile))
                .then(ClientCommands.literal("hud")
                    .executes(ProfileCommand::runToggleHud))
                .then(ClientCommands.literal("status")
                    .executes(ProfileCommand::runStatus))
                .then(ClientCommands.literal("dashboard")
                    .executes(ProfileCommand::runDashboard))
        );
    }

    private static int runProfile(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();

        if (TickProfiler.sampleCount() == 0) {
            source.sendFeedback(Component.literal("§c[LagFixer] No tick data recorded yet."));
            return 0;
        }

        String timestamp = LocalDateTime.now().format(TS);
        Path outputDir   = Paths.get("config", "lagfixer-profiles");
        Path outputFile  = outputDir.resolve("profile-" + timestamp + ".csv");

        try {
            TickProfiler.dumpCsv(outputFile);
        } catch (IOException e) {
            source.sendFeedback(Component.literal("§c[LagFixer] Failed to write profile: " + e.getMessage()));
            return 0;
        }

        source.sendFeedback(Component.literal(
            "§a[LagFixer] " + TickProfiler.summary()));
        source.sendFeedback(Component.literal(
            "§7Saved to: §f" + outputFile));
        return 1;
    }

    private static int runToggleHud(CommandContext<FabricClientCommandSource> ctx) {
        com.devxkamlesh.lagfixer.hud.HudOverlay.toggle();
        boolean on = com.devxkamlesh.lagfixer.hud.HudOverlay.isVisible();
        ctx.getSource().sendFeedback(Component.literal(
            "§a[LagFixer] HUD overlay " + (on ? "§aenabled" : "§cdisabled")));
        return 1;
    }

    private static int runStatus(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        source.sendFeedback(Component.literal("§a[LagFixer] Minecraft target: §f26.3 only"));
        source.sendFeedback(Component.literal("§7Profiler: §f" + TickProfiler.summary()));
        MixinStatus.snapshot().forEach((name, state) -> source.sendFeedback(Component.literal(
            (state.applied() ? "§a✔ " : "§c✘ ") + name + " §7— " + state.reason())));
        return 1;
    }

    private static int runDashboard(CommandContext<FabricClientCommandSource> ctx) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        minecraft.gui.setScreen(new LagFixerDashboardScreen(minecraft.gui.screen()));
        return 1;
    }
}
