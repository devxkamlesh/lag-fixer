package com.devxkamlesh.lagfixer.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Reads and writes lagfixer.properties in the Fabric config directory.
 *
 * Loaded at mixin-application time (inside LagFixerMixinPlugin.onLoad),
 * which runs before ModInitializer — keep this class dependency-free
 * (no Minecraft classes, no Fabric API classes beyond FabricLoader).
 *
 * The Cloth Config screen calls save() after the player clicks Done.
 * Changes take effect on next launch (mixin gates are set at class-load time).
 */
public final class LagFixerConfig {

    private static final Logger LOG = LoggerFactory.getLogger("LagFixer/Config");
    private static final String FILE_NAME = "lagfixer.properties";
    private static final String KEY_CONFIG_VERSION = "config.version";
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final Properties PROPS  = new Properties();
    private static Path configFile;

    // ── category keys ──────────────────────────────────────────────────────
    public static final String KEY_HOPPER    = "mixin.hopper";
    public static final String KEY_ENTITY    = "mixin.entity";
    public static final String KEY_REDSTONE  = "mixin.redstone";
    public static final String KEY_COLLISION = "mixin.collision";
    public static final String KEY_RENDER    = "mixin.render";

    // ── slider keys ────────────────────────────────────────────────────────
    public static final String KEY_MAX_PARTICLES   = "render.maxParticles";
    public static final String KEY_RENDER_DIST_CAP = "render.distanceCap";

    // ── entity AI skip interval ────────────────────────────────────────────
    public static final String KEY_AI_SKIP_TICKS = "entity.aiSkipTicks";
    public static final String KEY_ADAPTIVE = "client.adaptive";
    public static final String KEY_ALERTS = "client.alerts";
    public static final String KEY_REDUCE_WEATHER = "client.reduceWeather";
    public static final String KEY_PAUSED_OPTIMIZATION = "client.pausedOptimization";
    public static final String KEY_ANIMAL_DISTANCE = "render.entityDistance.animals";
    public static final String KEY_MONSTER_DISTANCE = "render.entityDistance.monsters";
    public static final String KEY_ITEM_DISTANCE = "render.entityDistance.items";
    public static final String KEY_DECORATION_DISTANCE = "render.entityDistance.decorations";

    private LagFixerConfig() {}

    /** Called from LagFixerMixinPlugin.onLoad(). Safe to call multiple times. */
    public static synchronized void load() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        PROPS.clear();
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                PROPS.load(in);
            } catch (IOException e) {
                LOG.error("Could not read {}. Safe defaults will be used.", configFile, e);
            }
        }
        migrateConfig();
    }

    // ── boolean getters (default ON) ──────────────────────────────────────

    public static boolean isHopperEnabled()    { return getBool(KEY_HOPPER,    false); }
    public static boolean isEntityEnabled()    { return getBool(KEY_ENTITY,    false); }
    public static boolean isRedstoneEnabled()  { return getBool(KEY_REDSTONE,  false); }
    public static boolean isCollisionEnabled() { return getBool(KEY_COLLISION, false); }
    public static boolean isRenderEnabled()    { return getBool(KEY_RENDER,    true); }

    // ── slider getters ────────────────────────────────────────────────────

    public static int getMaxParticles()   { return getInt(KEY_MAX_PARTICLES,   500,  50, 4000); }
    public static int getRenderDistCap()  { return getInt(KEY_RENDER_DIST_CAP,  12,   2,   32); }
    public static int getAiSkipTicks()    { return getInt(KEY_AI_SKIP_TICKS,     4,   1,   20); }
    public static boolean isAdaptiveEnabled() { return getBool(KEY_ADAPTIVE, false); }
    public static boolean areAlertsEnabled() { return getBool(KEY_ALERTS, true); }
    public static boolean isWeatherReductionEnabled() { return getBool(KEY_REDUCE_WEATHER, true); }
    public static boolean isPausedOptimizationEnabled() { return getBool(KEY_PAUSED_OPTIMIZATION, true); }
    public static int getAnimalRenderDistance() { return getInt(KEY_ANIMAL_DISTANCE, 64, 16, 256); }
    public static int getMonsterRenderDistance() { return getInt(KEY_MONSTER_DISTANCE, 96, 16, 256); }
    public static int getItemRenderDistance() { return getInt(KEY_ITEM_DISTANCE, 48, 16, 256); }
    public static int getDecorationRenderDistance() { return getInt(KEY_DECORATION_DISTANCE, 64, 16, 256); }

    // ── setters (called by config screen) ────────────────────────────────

    public static void setHopper(boolean v)    { PROPS.setProperty(KEY_HOPPER,    String.valueOf(v)); }
    public static void setEntity(boolean v)    { PROPS.setProperty(KEY_ENTITY,    String.valueOf(v)); }
    public static void setRedstone(boolean v)  { PROPS.setProperty(KEY_REDSTONE,  String.valueOf(v)); }
    public static void setCollision(boolean v) { PROPS.setProperty(KEY_COLLISION, String.valueOf(v)); }
    public static void setRender(boolean v)    { PROPS.setProperty(KEY_RENDER,    String.valueOf(v)); }
    public static void setMaxParticles(int v)  { PROPS.setProperty(KEY_MAX_PARTICLES,   String.valueOf(clamp(v, 50, 4000))); }
    public static void setRenderDistCap(int v) { PROPS.setProperty(KEY_RENDER_DIST_CAP, String.valueOf(clamp(v, 2, 32))); }
    public static void setAiSkipTicks(int v)   { PROPS.setProperty(KEY_AI_SKIP_TICKS,   String.valueOf(clamp(v, 1, 20))); }
    public static void setAdaptive(boolean v) { PROPS.setProperty(KEY_ADAPTIVE, String.valueOf(v)); }
    public static void setAlerts(boolean v) { PROPS.setProperty(KEY_ALERTS, String.valueOf(v)); }
    public static void setReduceWeather(boolean v) { PROPS.setProperty(KEY_REDUCE_WEATHER, String.valueOf(v)); }
    public static void setPausedOptimization(boolean v) { PROPS.setProperty(KEY_PAUSED_OPTIMIZATION, String.valueOf(v)); }
    public static void setAnimalRenderDistance(int v) { PROPS.setProperty(KEY_ANIMAL_DISTANCE, String.valueOf(clamp(v, 16, 256))); }
    public static void setMonsterRenderDistance(int v) { PROPS.setProperty(KEY_MONSTER_DISTANCE, String.valueOf(clamp(v, 16, 256))); }
    public static void setItemRenderDistance(int v) { PROPS.setProperty(KEY_ITEM_DISTANCE, String.valueOf(clamp(v, 16, 256))); }
    public static void setDecorationRenderDistance(int v) { PROPS.setProperty(KEY_DECORATION_DISTANCE, String.valueOf(clamp(v, 16, 256))); }

    /** Apply a preset combination in one call. */
    public static void applyPreset(Preset preset) {
        switch (preset) {
            case POTATO -> {
                applySafeLogicDefaults(); setRender(true);
                setMaxParticles(100); setRenderDistCap(6); setAiSkipTicks(8);
            }
            case BALANCED -> {
                applySafeLogicDefaults(); setRender(true);
                setMaxParticles(500); setRenderDistCap(12); setAiSkipTicks(4);
            }
            case QUALITY -> {
                applySafeLogicDefaults(); setRender(true);
                setMaxParticles(2000); setRenderDistCap(20); setAiSkipTicks(1);
            }
        }
    }

    public enum Preset { POTATO, BALANCED, QUALITY }

    /** Persist all current values to disk. Called by the config screen. */
    public static synchronized void save() {
        if (configFile == null) return;
        Path temp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream out = Files.newOutputStream(temp)) {
                PROPS.store(out, "Lag Fixer 26.3 config; mixin switches require a restart");
            }
            try {
                Files.move(temp, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.error("Could not save {}.", configFile, e);
        }
    }

    /**
     * Used by LagFixerMixinPlugin to map a mixin's fully-qualified class name
     * to its category key (e.g. "...mixin.hopper.FooMixin" → "mixin.hopper").
     */
    public static boolean isMixinCategoryEnabled(String mixinClassName) {
        String category = extractCategory(mixinClassName);
        return switch (category) {
            case "hopper"    -> isHopperEnabled();
            case "entity"    -> isEntityEnabled();
            case "redstone"  -> isRedstoneEnabled();
            case "collision" -> isCollisionEnabled();
            case "render"    -> isRenderEnabled();
            default          -> true; // unknown category: allow
        };
    }

    /** Extracts the sub-package segment after "mixin." from a class name. */
    public static String extractCategory(String mixinClassName) {
        String[] parts = mixinClassName.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("mixin".equals(parts[i])) return parts[i + 1];
        }
        return "unknown";
    }

    // ── private helpers ───────────────────────────────────────────────────

    private static boolean getBool(String key, boolean def) {
        String v = PROPS.getProperty(key);
        if (v == null) return def;
        return !"false".equalsIgnoreCase(v.trim());
    }

    private static int getInt(String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(PROPS.getProperty(key, String.valueOf(def)).trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void applySafeLogicDefaults() {
        setHopper(false);
        setEntity(false);
        setRedstone(false);
        setCollision(false);
    }

    private static void applySafeDefaults() {
        applySafeLogicDefaults();
        setRender(true);
        setMaxParticles(500);
        setRenderDistCap(12);
        setAiSkipTicks(4);
        setAdaptive(false);
        setAlerts(true);
        setReduceWeather(true);
        setPausedOptimization(true);
        setAnimalRenderDistance(64);
        setMonsterRenderDistance(96);
        setItemRenderDistance(48);
        setDecorationRenderDistance(64);
    }

    private static void migrateConfig() {
        int version = parseInt(PROPS.getProperty(KEY_CONFIG_VERSION), 0);
        boolean changed = false;
        if (version == 0 && PROPS.isEmpty()) {
            applySafeDefaults();
            changed = true;
        }

        if (!PROPS.containsKey(KEY_HOPPER)) {
            PROPS.setProperty(KEY_HOPPER, String.valueOf(false)); changed = true;
        }
        if (!PROPS.containsKey(KEY_ENTITY)) {
            PROPS.setProperty(KEY_ENTITY, String.valueOf(false)); changed = true;
        }
        if (!PROPS.containsKey(KEY_REDSTONE)) {
            PROPS.setProperty(KEY_REDSTONE, String.valueOf(false)); changed = true;
        }
        if (!PROPS.containsKey(KEY_COLLISION)) {
            PROPS.setProperty(KEY_COLLISION, String.valueOf(false)); changed = true;
        }
        if (!PROPS.containsKey(KEY_RENDER)) {
            PROPS.setProperty(KEY_RENDER, String.valueOf(true)); changed = true;
        }
        if (!PROPS.containsKey(KEY_MAX_PARTICLES)) {
            PROPS.setProperty(KEY_MAX_PARTICLES, String.valueOf(500)); changed = true;
        }
        if (!PROPS.containsKey(KEY_RENDER_DIST_CAP)) {
            PROPS.setProperty(KEY_RENDER_DIST_CAP, String.valueOf(12)); changed = true;
        }
        if (!PROPS.containsKey(KEY_AI_SKIP_TICKS)) {
            PROPS.setProperty(KEY_AI_SKIP_TICKS, String.valueOf(4)); changed = true;
        }
        if (!PROPS.containsKey(KEY_ADAPTIVE)) {
            PROPS.setProperty(KEY_ADAPTIVE, String.valueOf(false)); changed = true;
        }
        if (!PROPS.containsKey(KEY_ALERTS)) {
            PROPS.setProperty(KEY_ALERTS, String.valueOf(true)); changed = true;
        }
        if (!PROPS.containsKey(KEY_REDUCE_WEATHER)) {
            PROPS.setProperty(KEY_REDUCE_WEATHER, String.valueOf(true)); changed = true;
        }
        if (!PROPS.containsKey(KEY_PAUSED_OPTIMIZATION)) {
            PROPS.setProperty(KEY_PAUSED_OPTIMIZATION, String.valueOf(true)); changed = true;
        }
        if (!PROPS.containsKey(KEY_ANIMAL_DISTANCE)) {
            PROPS.setProperty(KEY_ANIMAL_DISTANCE, String.valueOf(64)); changed = true;
        }
        if (!PROPS.containsKey(KEY_MONSTER_DISTANCE)) {
            PROPS.setProperty(KEY_MONSTER_DISTANCE, String.valueOf(96)); changed = true;
        }
        if (!PROPS.containsKey(KEY_ITEM_DISTANCE)) {
            PROPS.setProperty(KEY_ITEM_DISTANCE, String.valueOf(48)); changed = true;
        }
        if (!PROPS.containsKey(KEY_DECORATION_DISTANCE)) {
            PROPS.setProperty(KEY_DECORATION_DISTANCE, String.valueOf(64)); changed = true;
        }

        PROPS.setProperty(KEY_CONFIG_VERSION, String.valueOf(CURRENT_CONFIG_VERSION));
        if (changed) {
            save();
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }
}
