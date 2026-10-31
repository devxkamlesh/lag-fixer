package com.devxkamlesh.lagfixer;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LagFixerSanityTest {

    @Test
    void categoryExtractionMatchesRegistryNames() {
        assertEquals("hopper", LagFixerConfig.extractCategory("com.devxkamlesh.lagfixer.mixin.hopper.HopperBlockEntityMixin"));
        assertEquals("render", LagFixerConfig.extractCategory("com.devxkamlesh.lagfixer.mixin.render.ParticleManagerMixin"));
        assertEquals("NotASubPackage", LagFixerConfig.extractCategory("com.devxkamlesh.lagfixer.mixin.NotASubPackage"));
        assertEquals("unknown", LagFixerConfig.extractCategory("com.devxkamlesh.lagfixer.config.LagFixerConfig"));
    }

    @Test
    void tickProfilerSummaryReportsStableMetrics() {
        TickProfiler.Snapshot snapshot = new TickProfiler.Snapshot(
            new long[]{1_000_000L, 2_000_000L, 3_000_000L, 4_000_000L, 5_000_000L},
            5,
            3.0,
            1.0,
            5.0,
            3.0,
            5.0,
            5.0
        );

        assertTrue(snapshot.summary().contains("avg 3.00 ms"));
        assertTrue(snapshot.summary().contains("min 1.00"));
        assertTrue(snapshot.summary().contains("max 5.00"));
        assertTrue(snapshot.summary().contains("(5 ticks)"));
    }
}
