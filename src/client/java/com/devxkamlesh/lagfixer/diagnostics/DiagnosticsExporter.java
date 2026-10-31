package com.devxkamlesh.lagfixer.diagnostics;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.mixin.MixinStatus;
import com.devxkamlesh.lagfixer.performance.PerformanceController;
import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Writes a local-only support report; no network or telemetry upload. */
public final class DiagnosticsExporter {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private DiagnosticsExporter() {}

    public static Path export() throws IOException {
        ClientTelemetry.Snapshot frame = ClientTelemetry.snapshot();
        TickProfiler.Snapshot tick = TickProfiler.snapshot();
        StringBuilder report = new StringBuilder("Lag Fixer diagnostic report (local only)\n")
            .append("Minecraft: 26.3\nJava: ").append(System.getProperty("java.version"))
            .append("\nOS: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.arch"))
            .append("\nProcessors: ").append(Runtime.getRuntime().availableProcessors())
            .append("\nMax heap MiB: ").append(frame.heapMax() / 1_048_576)
            .append("\nUsed heap MiB: ").append(frame.heapUsed() / 1_048_576)
            .append(String.format(Locale.ROOT, "\nFrame ms: avg %.2f p95 %.2f p99 %.2f max %.2f freezes %d stutters %d",
                frame.avgMs(), frame.p95Ms(), frame.p99Ms(), frame.maxMs(), frame.freezes(), frame.stutters()))
            .append("\nTick: ").append(tick.summary())
            .append("\nAdaptive: ").append(PerformanceController.status())
            .append("\nConfigured particles: ").append(LagFixerConfig.getMaxParticles())
            .append("\nConfigured distance: ").append(LagFixerConfig.getRenderDistCap())
            .append("\nParticles accepted/dropped: ").append(frame.acceptedParticles()).append('/').append(frame.droppedParticles())
            .append("\n\nMixin status:\n");
        MixinStatus.snapshot().forEach((name, state) -> report.append(name).append(": ")
            .append(state.applied() ? "active" : "inactive").append(" (").append(state.reason()).append(")\n"));
        report.append("\nLoaded mods:\n");
        FabricLoader.getInstance().getAllMods().stream()
            .sorted(java.util.Comparator.comparing(mod -> mod.getMetadata().getId()))
            .forEach(mod -> report.append(mod.getMetadata().getId()).append(' ')
                .append(mod.getMetadata().getVersion().getFriendlyString()).append('\n'));
        Path output = FabricLoader.getInstance().getConfigDir().resolve("lagfixer-profiles")
            .resolve("diagnostic-" + LocalDateTime.now().format(TS) + ".txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report);
        return output;
    }
}
