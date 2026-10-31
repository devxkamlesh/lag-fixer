package com.devxkamlesh.lagfixer.profiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/**
 * Thread-safe rolling server tick profiler with percentile reporting.
 */
public final class TickProfiler {

    private static final int WINDOW = 200;

    private static final long[] buffer   = new long[WINDOW];
    private static int           head     = 0;   // next write index
    private static int           count    = 0;   // samples recorded so far (≤ WINDOW)
    private static long          tickStart = 0L; // nanosecond timestamp of current tick start

    private TickProfiler() {}

    /** Call at the start of each server tick. */
    public static synchronized void beginTick() {
        tickStart = System.nanoTime();
    }

    /** Call at the end of each server tick. Stores elapsed time in buffer. */
    public static synchronized void endTick() {
        if (tickStart == 0L) return;
        long elapsed = System.nanoTime() - tickStart;
        tickStart = 0L;

        buffer[head] = elapsed;
        head = (head + 1) % WINDOW;
        if (count < WINDOW) count++;
    }

    public static synchronized Snapshot snapshot() {
        long[] chronological = new long[count];
        for (int i = 0; i < count; i++) {
            chronological[i] = buffer[(head - count + i + WINDOW) % WINDOW];
        }
        return Snapshot.from(chronological);
    }

    public static double avgMs() { return snapshot().avgMs(); }
    public static double minMs() { return snapshot().minMs(); }
    public static double maxMs() { return snapshot().maxMs(); }
    public static int sampleCount() { return snapshot().samples(); }

    /**
     * Dumps the current window to a CSV file at the given path.
     * Format: tick_index,ms
     */
    public static void dumpCsv(Path outputPath) throws IOException {
        Snapshot data = snapshot();
        Files.createDirectories(outputPath.getParent());
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            pw.println("tick_index,ms,avg_ms,min_ms,max_ms,p50_ms,p95_ms,p99_ms");
            for (int i = 0; i < data.nanos().length; i++) {
                double ms = data.nanos()[i] / 1_000_000.0;
                pw.printf(Locale.ROOT, "%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                    i, ms, data.avgMs(), data.minMs(), data.maxMs(),
                    data.p50Ms(), data.p95Ms(), data.p99Ms());
            }
        }
    }

    /**
     * Returns a single-line summary string suitable for pasting into
     * Modrinth or chat: "avg 2.1 ms | min 1.4 ms | max 8.7 ms (200 ticks)"
     */
    public static String summary() {
        return snapshot().summary();
    }

    public record Snapshot(long[] nanos, int samples, double avgMs, double minMs, double maxMs,
                           double p50Ms, double p95Ms, double p99Ms) {
        private static Snapshot from(long[] chronological) {
            if (chronological.length == 0) {
                return new Snapshot(chronological, 0, 0, 0, 0, 0, 0, 0);
            }
            long sum = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (long value : chronological) {
                sum += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            long[] sorted = chronological.clone();
            Arrays.sort(sorted);
            return new Snapshot(chronological, chronological.length,
                sum / (double) chronological.length / 1_000_000.0,
                min / 1_000_000.0, max / 1_000_000.0,
                percentile(sorted, 0.50), percentile(sorted, 0.95), percentile(sorted, 0.99));
        }

        private static double percentile(long[] sorted, double percentile) {
            int index = Math.min(sorted.length - 1, (int) Math.ceil(percentile * sorted.length) - 1);
            return sorted[index] / 1_000_000.0;
        }

        public String summary() {
            return String.format(Locale.ROOT,
                "avg %.2f ms | min %.2f | max %.2f | p95 %.2f | p99 %.2f (%d ticks)",
                avgMs, minMs, maxMs, p95Ms, p99Ms, samples);
        }
    }
}
