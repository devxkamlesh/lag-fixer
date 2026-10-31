package com.devxkamlesh.lagfixer.profiler;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Allocation-light client frame, memory and multi-minute history telemetry. */
public final class ClientTelemetry {
    private static final int CAPACITY = 36_000; // at least several minutes on common frame rates
    private static final long[] FRAMES = new long[CAPACITY];
    private static int head;
    private static int count;
    private static long frameStart;
    private static volatile Snapshot published = Snapshot.empty();
    private static long lastPublish;
    private static long acceptedParticles;
    private static long droppedParticles;

    private ClientTelemetry() {}

    public static void beginFrame() { frameStart = System.nanoTime(); }

    public static void endFrame() {
        if (frameStart == 0L) return;
        long elapsed = System.nanoTime() - frameStart;
        frameStart = 0L;
        if (elapsed <= 0 || elapsed > 2_000_000_000L) return;
        FRAMES[head] = elapsed;
        head = (head + 1) % CAPACITY;
        if (count < CAPACITY) count++;
        long now = System.nanoTime();
        if (now - lastPublish >= 250_000_000L) {
            published = buildSnapshot(120);
            lastPublish = now;
        }
    }

    public static Snapshot snapshot() { return published; }

    public static synchronized long[] history(int seconds, int maxPoints) {
        int wanted = Math.min(count, Math.max(1, seconds * 260));
        int points = Math.min(wanted, Math.max(1, maxPoints));
        long[] output = new long[points];
        for (int i = 0; i < points; i++) {
            int source = (int) ((long) i * wanted / points);
            output[i] = FRAMES[(head - wanted + source + CAPACITY) % CAPACITY];
        }
        return output;
    }

    public static synchronized void particleAccepted() { acceptedParticles++; }
    public static synchronized void particleDropped() { droppedParticles++; }

    private static synchronized Snapshot buildSnapshot(int seconds) {
        int n = Math.min(count, seconds * 260);
        if (n == 0) return Snapshot.empty();
        long[] data = new long[n];
        long sum = 0, max = 0;
        int stutters = 0, freezes = 0;
        for (int i = 0; i < n; i++) {
            long value = FRAMES[(head - n + i + CAPACITY) % CAPACITY];
            data[i] = value;
            sum += value;
            max = Math.max(max, value);
            if (value > 50_000_000L) stutters++;
            if (value > 250_000_000L) freezes++;
        }
        Arrays.sort(data);
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long gcCount = 0, gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() >= 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() >= 0) gcTime += gc.getCollectionTime();
        }
        double average = sum / (double) n / 1_000_000.0;
        return new Snapshot(n, average, percentile(data, .50), percentile(data, .95), percentile(data, .99),
            max / 1_000_000.0, 1000.0 / Math.max(.001, percentile(data, .99)), stutters, freezes,
            used, runtime.maxMemory(), gcCount, gcTime, acceptedParticles, droppedParticles);
    }

    private static double percentile(long[] sorted, double p) {
        return sorted[Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1)] / 1_000_000.0;
    }

    public record Snapshot(int samples, double avgMs, double p50Ms, double p95Ms, double p99Ms,
                           double maxMs, double lowOnePercentFps, int stutters, int freezes,
                           long heapUsed, long heapMax, long gcCount, long gcTimeMs,
                           long acceptedParticles, long droppedParticles) {
        static Snapshot empty() { return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0); }
        public double heapPercent() { return heapUsed * 100.0 / Math.max(1, heapMax); }
    }
}
