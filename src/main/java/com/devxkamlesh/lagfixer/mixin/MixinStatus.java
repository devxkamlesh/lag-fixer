package com.devxkamlesh.lagfixer.mixin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable-at-runtime registry of mixin decisions made during startup. */
public final class MixinStatus {
    private static final Map<String, State> STATES = new LinkedHashMap<>();

    private MixinStatus() {}

    static synchronized void record(String mixinClassName, boolean applied, String reason) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        STATES.put(simpleName, new State(applied, reason));
    }

    public static synchronized Map<String, State> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(STATES));
    }

    public record State(boolean applied, String reason) {}
}