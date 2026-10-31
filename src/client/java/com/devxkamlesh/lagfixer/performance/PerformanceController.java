package com.devxkamlesh.lagfixer.performance;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import net.minecraft.client.Minecraft;

/** Adaptive quality control and rate-limited local performance alerts. */
public final class PerformanceController {
    private static int adaptiveParticles = Integer.MAX_VALUE;
    private static int adaptiveDistance = Integer.MAX_VALUE;
    private static long nextAdjustment;
    private static long nextAlert;
    private static volatile String latestAlert = "No active alerts";

    private PerformanceController() {}

    public static void tick(Minecraft minecraft) {
        long now = System.nanoTime();
        ClientTelemetry.Snapshot frame = ClientTelemetry.snapshot();
        if (LagFixerConfig.isAdaptiveEnabled() && frame.samples() > 120 && now >= nextAdjustment) {
            int configuredParticles = LagFixerConfig.getMaxParticles();
            int configuredDistance = LagFixerConfig.getRenderDistCap();
            if (adaptiveParticles == Integer.MAX_VALUE) adaptiveParticles = configuredParticles;
            if (adaptiveDistance == Integer.MAX_VALUE) adaptiveDistance = configuredDistance;
            if (frame.p95Ms() > 33.3) {
                adaptiveParticles = Math.max(50, (int) (adaptiveParticles * .8));
                if (adaptiveParticles <= 150) adaptiveDistance = Math.max(4, adaptiveDistance - 1);
                nextAdjustment = now + 5_000_000_000L;
            } else if (frame.p95Ms() < 20.0) {
                adaptiveParticles = Math.min(configuredParticles, adaptiveParticles + Math.max(25, configuredParticles / 10));
                adaptiveDistance = Math.min(configuredDistance, adaptiveDistance + 1);
                nextAdjustment = now + 15_000_000_000L;
            }
        } else if (!LagFixerConfig.isAdaptiveEnabled()) {
            adaptiveParticles = Integer.MAX_VALUE;
            adaptiveDistance = Integer.MAX_VALUE;
        }

        if (!LagFixerConfig.areAlertsEnabled() || minecraft.player == null) {
            latestAlert = "No active alerts";
            return;
        }

        if (now < nextAlert) {
            return;
        }

        TickProfiler.Snapshot tick = TickProfiler.snapshot();
        if (frame.p95Ms() > 50.0) {
            latestAlert = "Client rendering pressure: frame p95 "
                + String.format(java.util.Locale.ROOT, "%.1f ms", frame.p95Ms());
            nextAlert = now + 30_000_000_000L;
        } else if (tick.samples() > 20 && tick.p95Ms() > 50.0) {
            latestAlert = "Integrated server pressure: tick p95 "
                + String.format(java.util.Locale.ROOT, "%.1f ms", tick.p95Ms());
            nextAlert = now + 30_000_000_000L;
        } else if (frame.heapPercent() > 90.0) {
            latestAlert = "High heap pressure: "
                + String.format(java.util.Locale.ROOT, "%.0f%%", frame.heapPercent());
            nextAlert = now + 60_000_000_000L;
        } else {
            latestAlert = "No active alerts";
        }
    }

    public static int effectiveParticleCap() {
        return Math.min(LagFixerConfig.getMaxParticles(), adaptiveParticles);
    }

    public static int effectiveRenderDistance() {
        return Math.min(LagFixerConfig.getRenderDistCap(), adaptiveDistance);
    }

    public static String status() {
        return LagFixerConfig.isAdaptiveEnabled()
            ? "Adaptive • " + effectiveParticleCap() + " particles • " + effectiveRenderDistance() + " chunks"
            : "Manual limits";
    }

    public static String latestAlert() { return latestAlert; }
}
