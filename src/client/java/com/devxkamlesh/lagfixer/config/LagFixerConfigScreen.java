package com.devxkamlesh.lagfixer.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config screen for Lag Fixer.
 *
 * Guarded at call-site: only built when cloth-config is loaded
 * (FabricLoader.getInstance().isModLoaded("cloth-config")).
 *
 * Layout:
 *   Category "Optimisations" — 5 boolean toggles
 *   Category "Render"        — maxParticles slider + renderDistCap slider
 *   Category "Presets"       — 3 buttons (Potato / Balanced / Quality)
 *
 * Saving writes to lagfixer.properties via LagFixerConfig.save().
 * Changes take effect on the NEXT launch (mixin gates are set at class-load time).
 */
public final class LagFixerConfigScreen {

    private LagFixerConfigScreen() {}

    /** Build and return the config screen. parent is the screen to return to on close. */
    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("lagfixer.config.title"))
            .setSavingRunnable(LagFixerConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ── Optimisations category ────────────────────────────────────────
        ConfigCategory opts = builder.getOrCreateCategory(
            Component.translatable("lagfixer.config.category.optimisations"));

        opts.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.hopper"), LagFixerConfig.isHopperEnabled())
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.hopper.tooltip"))
            .setSaveConsumer(LagFixerConfig::setHopper)
            .build());

        opts.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.entity"), LagFixerConfig.isEntityEnabled())
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.entity.tooltip"))
            .setSaveConsumer(LagFixerConfig::setEntity)
            .build());

        opts.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.redstone"), LagFixerConfig.isRedstoneEnabled())
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.redstone.tooltip"))
            .setSaveConsumer(LagFixerConfig::setRedstone)
            .build());

        opts.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.collision"), LagFixerConfig.isCollisionEnabled())
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.collision.tooltip"))
            .setSaveConsumer(LagFixerConfig::setCollision)
            .build());

        opts.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.render"), LagFixerConfig.isRenderEnabled())
            .setDefaultValue(true)
            .setTooltip(Component.translatable("lagfixer.config.render.tooltip"))
            .setSaveConsumer(LagFixerConfig::setRender)
            .build());

        opts.addEntry(eb.startIntSlider(
                Component.translatable("lagfixer.config.aiSkipTicks"),
                LagFixerConfig.getAiSkipTicks(), 1, 20)
            .setDefaultValue(4)
            .setTooltip(Component.translatable("lagfixer.config.aiSkipTicks.tooltip"))
            .setSaveConsumer(LagFixerConfig::setAiSkipTicks)
            .build());

        // ── Render category ───────────────────────────────────────────────
        ConfigCategory render = builder.getOrCreateCategory(
            Component.translatable("lagfixer.config.category.render"));

        render.addEntry(eb.startIntSlider(
                Component.translatable("lagfixer.config.maxParticles"),
                LagFixerConfig.getMaxParticles(), 50, 4000)
            .setDefaultValue(500)
            .setTooltip(Component.translatable("lagfixer.config.maxParticles.tooltip"))
            .setSaveConsumer(LagFixerConfig::setMaxParticles)
            .build());

        render.addEntry(eb.startBooleanToggle(Component.literal("Adaptive performance"), LagFixerConfig.isAdaptiveEnabled())
            .setDefaultValue(false).setSaveConsumer(LagFixerConfig::setAdaptive).build());
        render.addEntry(eb.startBooleanToggle(Component.literal("Performance alerts"), LagFixerConfig.areAlertsEnabled())
            .setDefaultValue(true).setSaveConsumer(LagFixerConfig::setAlerts).build());
        render.addEntry(eb.startBooleanToggle(Component.literal("Reduce weather under pressure"), LagFixerConfig.isWeatherReductionEnabled())
            .setDefaultValue(true).setSaveConsumer(LagFixerConfig::setReduceWeather).build());
        render.addEntry(eb.startBooleanToggle(Component.literal("Paused/unfocused optimization"), LagFixerConfig.isPausedOptimizationEnabled())
            .setDefaultValue(true).setSaveConsumer(LagFixerConfig::setPausedOptimization).build());
        render.addEntry(eb.startIntSlider(Component.literal("Animal render distance"), LagFixerConfig.getAnimalRenderDistance(), 16, 256)
            .setDefaultValue(64).setSaveConsumer(LagFixerConfig::setAnimalRenderDistance).build());
        render.addEntry(eb.startIntSlider(Component.literal("Monster render distance"), LagFixerConfig.getMonsterRenderDistance(), 16, 256)
            .setDefaultValue(96).setSaveConsumer(LagFixerConfig::setMonsterRenderDistance).build());
        render.addEntry(eb.startIntSlider(Component.literal("Item render distance"), LagFixerConfig.getItemRenderDistance(), 16, 256)
            .setDefaultValue(48).setSaveConsumer(LagFixerConfig::setItemRenderDistance).build());
        render.addEntry(eb.startIntSlider(Component.literal("Decoration render distance"), LagFixerConfig.getDecorationRenderDistance(), 16, 256)
            .setDefaultValue(64).setSaveConsumer(LagFixerConfig::setDecorationRenderDistance).build());

        render.addEntry(eb.startIntSlider(
                Component.translatable("lagfixer.config.renderDistCap"),
                LagFixerConfig.getRenderDistCap(), 2, 32)
            .setDefaultValue(12)
            .setTooltip(Component.translatable("lagfixer.config.renderDistCap.tooltip"))
            .setSaveConsumer(LagFixerConfig::setRenderDistCap)
            .build());

        // ── Presets category ──────────────────────────────────────────────
        ConfigCategory presets = builder.getOrCreateCategory(
            Component.translatable("lagfixer.config.category.presets"));

        presets.addEntry(eb.startTextDescription(
            Component.translatable("lagfixer.config.presets.note"))
            .build());

        // Each button applies the preset then saves immediately (no need to click Done)
        presets.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.preset.potato"), false)
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.preset.potato.tooltip"))
            .setSaveConsumer(v -> { if (v) { LagFixerConfig.applyPreset(LagFixerConfig.Preset.POTATO); LagFixerConfig.save(); } })
            .build());

        presets.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.preset.balanced"), false)
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.preset.balanced.tooltip"))
            .setSaveConsumer(v -> { if (v) { LagFixerConfig.applyPreset(LagFixerConfig.Preset.BALANCED); LagFixerConfig.save(); } })
            .build());

        presets.addEntry(eb.startBooleanToggle(
                Component.translatable("lagfixer.config.preset.quality"), false)
            .setDefaultValue(false)
            .setTooltip(Component.translatable("lagfixer.config.preset.quality.tooltip"))
            .setSaveConsumer(v -> { if (v) { LagFixerConfig.applyPreset(LagFixerConfig.Preset.QUALITY); LagFixerConfig.save(); } })
            .build());

        return builder.build();
    }
}
