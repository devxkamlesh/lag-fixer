package com.devxkamlesh.lagfixer.benchmark;

import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;

import java.util.Locale;

/** Compact 30-120 second benchmark state machine with baseline comparison. */
public final class BenchmarkManager {
    private static long started;
    private static int duration;
    private static Result baseline;
    private static Result latest;

    private BenchmarkManager() {}

    public static boolean start(int seconds) {
        if (isRunning()) return false;
        duration = Math.max(30, Math.min(120, seconds));
        started = System.nanoTime();
        return true;
    }

    public static boolean isRunning() { return started != 0L; }

    public static double progress() {
        if (!isRunning()) return 0;
        return Math.min(1.0, (System.nanoTime() - started) / (duration * 1_000_000_000.0));
    }

    public static void tick() {
        if (isRunning() && progress() >= 1.0) {
            ClientTelemetry.Snapshot frame = ClientTelemetry.snapshot();
            TickProfiler.Snapshot tick = TickProfiler.snapshot();
            latest = new Result(duration, frame.avgMs(), frame.p95Ms(), frame.p99Ms(), frame.lowOnePercentFps(),
                frame.stutters(), tick.avgMs(), tick.p95Ms(), frame.heapUsed());
            if (baseline == null) baseline = latest;
            started = 0L;
        }
    }

    public static void cancel() { started = 0L; }
    public static Result baseline() { return baseline; }
    public static Result latest() { return latest; }
    public static void useLatestAsBaseline() { if (latest != null) baseline = latest; }

    public static String comparison() {
        if (baseline == null || latest == null) return "Run a benchmark to create comparison data.";
        return String.format(Locale.ROOT, "Frame p95 %+.1f%% • 1%% low %+.1f%% • Tick p95 %+.1f%%",
            delta(baseline.frameP95, latest.frameP95), delta(latest.lowFps, baseline.lowFps),
            delta(baseline.tickP95, latest.tickP95));
    }

    private static double delta(double before, double after) {
        return before == 0 ? 0 : (before - after) * 100.0 / before;
    }

    public record Result(int seconds, double frameAvg, double frameP95, double frameP99, double lowFps,
                         int stutters, double tickAvg, double tickP95, long heapUsed) {}
}
