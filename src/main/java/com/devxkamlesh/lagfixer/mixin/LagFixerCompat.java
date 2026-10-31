package com.devxkamlesh.lagfixer.mixin;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detects conflicting mods at Mixin-application time and builds a set of
 * categories that should be auto-disabled.
 *
 * Must be initialised from LagFixerMixinPlugin.onLoad() — NOT from
 * ModInitializer, which fires far too late to gate class-load-time injections.
 */
public final class LagFixerCompat {

    private static final Logger LOG = LoggerFactory.getLogger("LagFixer/Compat");

    /**
     * Category → mod-id that already covers it.
     * If that mod is present, every mixin in that category is skipped.
     */
    private static final Map<String, String> CONFLICTS = Map.of(
        "hopper",    "lithium",
        "entity",    "lithium",
        "redstone",  "lithium",
        "collision", "ferritecore"
    );

    private static Set<String> disabledCategories;

    private LagFixerCompat() {}

    /** Called once from LagFixerMixinPlugin.onLoad(). */
    public static void init() {
        disabledCategories = new HashSet<>();
        for (Map.Entry<String, String> e : CONFLICTS.entrySet()) {
            String category = e.getKey();
            String modId    = e.getValue();
            if (FabricLoader.getInstance().isModLoaded(modId)) {
                disabledCategories.add(category);
                LOG.info("[LagFixer] '{}' optimisation disabled — {} is installed.", category, modId);
            }
        }
    }

    /**
     * Returns true if the mixin's category is covered by another mod and
     * should therefore be skipped.
     */
    public static boolean isDisabledByConflict(String mixinClassName) {
        if (disabledCategories == null) return false;
        String category = LagFixerConfig.extractCategory(mixinClassName);
        return disabledCategories.contains(category);
    }
}
