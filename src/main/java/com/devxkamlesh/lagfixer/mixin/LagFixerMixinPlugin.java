package com.devxkamlesh.lagfixer.mixin;

import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Controls which Lag Fixer mixins are applied at class-load time.
 *
 * Two gates in shouldApplyMixin():
 *   1. User config toggle (lagfixer.properties)
 *   2. Conflicting mod detected (Lithium, FerriteCore, Sodium)
 *
 * This is the ONLY correct place for these checks — ModInitializer runs
 * after class loading and cannot block a mixin.
 */
public final class LagFixerMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // Load config first so compat can reference category flags if needed
        LagFixerConfig.load();
        LagFixerCompat.init();
    }

    @Override
    public String getRefMapperConfig() {
        // 26.x is unobfuscated — no refmap required
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Gate 1: user config
        if (!LagFixerConfig.isMixinCategoryEnabled(mixinClassName)) {
            MixinStatus.record(mixinClassName, false, "disabled by config");
            return false;
        }
        // Gate 2: conflicting mod
        if (LagFixerCompat.isDisabledByConflict(mixinClassName)) {
            MixinStatus.record(mixinClassName, false, "disabled by compatibility rule");
            return false;
        }
        MixinStatus.record(mixinClassName, true, "active");
        return true;
    }

    // ── required no-op interface methods ──────────────────────────────────

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
