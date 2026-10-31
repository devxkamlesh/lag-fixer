package com.devxkamlesh.lagfixer.gui;

import com.devxkamlesh.lagfixer.LagFixerMod;
import com.devxkamlesh.lagfixer.benchmark.BenchmarkManager;
import com.devxkamlesh.lagfixer.config.LagFixerConfig;
import com.devxkamlesh.lagfixer.diagnostics.DiagnosticsExporter;
import com.devxkamlesh.lagfixer.hud.HudOverlay;
import com.devxkamlesh.lagfixer.mixin.MixinStatus;
import com.devxkamlesh.lagfixer.performance.PerformanceController;
import com.devxkamlesh.lagfixer.profiler.ClientTelemetry;
import com.devxkamlesh.lagfixer.profiler.TickProfiler;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Modern high-performance dashboard for Lag Fixer on Minecraft 26.3.
 */
public final class LagFixerDashboardScreen extends Screen {

    private enum Tab { OVERVIEW, FRAMES, TICKS, STATUS, SETTINGS, COMPATIBILITY }

    private final Screen parent;
    private Tab tab = Tab.OVERVIEW;
    private int historySeconds = 30;
    private String notice = "";

    public LagFixerDashboardScreen(Screen parent) {
        super(Component.translatable("lagfixer.dashboard.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((LagFixerMod.dashboardKey != null && LagFixerMod.dashboardKey.matches(event))
            || event.key() == InputConstants.KEY_F8) {
            onClose();
            return true;
        }
        if ((LagFixerMod.hudKey != null && LagFixerMod.hudKey.matches(event))
            || event.key() == InputConstants.KEY_F7) {
            HudOverlay.toggle();
            notice = HudOverlay.isVisible() ? "HUD overlay enabled" : "HUD overlay disabled";
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private void rebuildButtons() {
        clearWidgets();

        // ── Tab Bar (Top) ────────────────────────────────────────────────────
        int margin = 12;
        int gap = 4;
        int tabWidth = (width - margin * 2 - gap * (Tab.values().length - 1)) / Tab.values().length;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab target = Tab.values()[i];
            addRenderableWidget(new DashboardTabButton(margin + i * (tabWidth + gap), 44, tabWidth, 20, target));
        }

        // ── Tab Specific Widgets ─────────────────────────────────────────────
        switch (tab) {
            case OVERVIEW -> overviewButtons();
            case FRAMES -> historyButtons();
            case TICKS -> tickButtons();
            case STATUS -> statusButtons();
            case SETTINGS -> settingsButtons();
            case COMPATIBILITY -> compatibilityButtons();
        }

        // ── Close Button (Right Footer) ──────────────────────────────────────
        addRenderableWidget(new ModernButton(width - 96, height - 28, 84, 20,
            Component.literal("Close [F8]"), this::onClose, true));
    }

    private void overviewButtons() {
        footerButton("HUD: " + onOff(HudOverlay.isVisible()), 12, () -> {
            HudOverlay.toggle();
            notice = HudOverlay.isVisible() ? "HUD overlay enabled" : "HUD overlay disabled";
            rebuildButtons();
        }, false);
        footerButton("Benchmark 30s", 116, () -> {
            BenchmarkManager.start(30);
            notice = "Benchmark started";
        }, false);
        footerButton("Use baseline", 228, () -> {
            BenchmarkManager.useLatestAsBaseline();
            notice = "Latest result is baseline";
        }, false);
        footerButton("Export report", 336, this::exportDiagnostics, false);
    }

    private void historyButtons() {
        footerButton("30s", 12, () -> { historySeconds = 30; rebuildButtons(); }, historySeconds == 30);
        footerButton("2m", 78, () -> { historySeconds = 120; rebuildButtons(); }, historySeconds == 120);
        footerButton("5m", 144, () -> { historySeconds = 300; rebuildButtons(); }, historySeconds == 300);
    }

    private void tickButtons() {
        footerButton("Export report", 12, this::exportDiagnostics, false);
    }

    private void statusButtons() {
        footerButton("Export report", 12, this::exportDiagnostics, false);
    }

    private void compatibilityButtons() {
        footerButton("Export report", 12, this::exportDiagnostics, false);
    }

    private void settingsButtons() {
        int colW = (width - 24 - 16) / 3;

        // Toggle buttons embedded directly on the cards
        addRenderableWidget(new ModernButton(12 + colW - 68, 70 + 38, 60, 20,
            Component.literal(LagFixerConfig.isAdaptiveEnabled() ? "ON" : "OFF"),
            () -> {
                LagFixerConfig.setAdaptive(!LagFixerConfig.isAdaptiveEnabled());
                LagFixerConfig.save();
                rebuildButtons();
            }, LagFixerConfig.isAdaptiveEnabled()));

        addRenderableWidget(new ModernButton(12 + colW + 8 + colW - 68, 70 + 38, 60, 20,
            Component.literal(LagFixerConfig.areAlertsEnabled() ? "ON" : "OFF"),
            () -> {
                LagFixerConfig.setAlerts(!LagFixerConfig.areAlertsEnabled());
                LagFixerConfig.save();
                rebuildButtons();
            }, LagFixerConfig.areAlertsEnabled()));

        addRenderableWidget(new ModernButton(12 + (colW + 8) * 2 + colW - 68, 70 + 38, 60, 20,
            Component.literal(LagFixerConfig.isWeatherReductionEnabled() ? "ON" : "OFF"),
            () -> {
                LagFixerConfig.setReduceWeather(!LagFixerConfig.isWeatherReductionEnabled());
                LagFixerConfig.save();
                rebuildButtons();
            }, LagFixerConfig.isWeatherReductionEnabled()));

        // Preset buttons inside the preset card
        int lowerY = 70 + 74;
        addRenderableWidget(new ModernButton(22, lowerY + 28, 74, 20, Component.literal("Potato"),
            () -> applyPreset(LagFixerConfig.Preset.POTATO), false));
        addRenderableWidget(new ModernButton(102, lowerY + 28, 84, 20, Component.literal("Balanced"),
            () -> applyPreset(LagFixerConfig.Preset.BALANCED), false));
        addRenderableWidget(new ModernButton(192, lowerY + 28, 74, 20, Component.literal("Quality"),
            () -> applyPreset(LagFixerConfig.Preset.QUALITY), false));

        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            addRenderableWidget(new ModernButton(272, lowerY + 28, 88, 20, Component.literal("Detailed..."),
                () -> minecraft.gui.setScreen(com.devxkamlesh.lagfixer.config.LagFixerConfigScreen.build(this)), false));
        }

        footerButton("Export report", 12, this::exportDiagnostics, false);
    }

    private void footerButton(String label, int x, Runnable action, boolean primary) {
        int available = width - x - 104;
        if (available < 30) return;
        int btnW = Math.min(108, available);
        addRenderableWidget(new ModernButton(x, height - 28, btnW, 20, Component.literal(label), action, primary));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // 1. Solid modern dark backdrop
        g.fill(0, 0, width, height, 0xFF0D1117);

        // 2. Top header bar
        g.fill(0, 0, width, 40, 0xFF161B22);
        g.horizontalLine(0, width, 40, 0xFF30363D);

        // Header logo pill badge
        g.fill(12, 10, 78, 30, 0xFF21262D);
        g.outline(12, 10, 66, 20, 0xFF30363D);
        g.centeredText(font, Component.literal("LAG FIXER"), 45, 16, 0xFF58A6FF);

        // Header title & engine status
        g.text(font, Component.literal("Performance Dashboard"), 84, 12, 0xFFF0F6FC, false);
        g.text(font, Component.literal("26.3 • " + PerformanceController.status()), 84, 24, 0xFF8B949E, false);

        // Header right-side key hint
        g.text(font, Component.literal("[F8 / ESC] Close"), width - 96, 16, 0xFF6E7681, false);

        // 3. Tab track bar
        g.fill(0, 41, width, 66, 0xFF161B22);
        g.horizontalLine(0, width, 66, 0xFF30363D);

        // 4. Tab contents
        switch (tab) {
            case OVERVIEW -> drawOverview(g);
            case FRAMES -> drawFrames(g);
            case TICKS -> drawTicks(g);
            case STATUS -> drawStatus(g);
            case SETTINGS -> drawSettings(g);
            case COMPATIBILITY -> drawCompatibility(g);
        }

        // 5. Footer bar
        g.fill(0, height - 34, width, height, 0xFF161B22);
        g.horizontalLine(0, width, height - 34, 0xFF30363D);

        // Notice toast in footer center
        if (!notice.isEmpty()) {
            int nw = font.width(notice) + 24;
            int nx = width / 2 - nw / 2;
            int ny = height - 27;
            g.fill(nx, ny, nx + nw, ny + 18, 0xFF194426);
            g.outline(nx, ny, nw, 18, 0xFF2EA043);
            g.centeredText(font, Component.literal(notice), width / 2, ny + 5, 0xFF7EE787);
        }

        // 6. Render widgets ON TOP of all backgrounds and panels!
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawOverview(GuiGraphicsExtractor g) {
        ClientTelemetry.Snapshot f = ClientTelemetry.snapshot();
        TickProfiler.Snapshot t = TickProfiler.snapshot();

        int topY = 70;
        int gap = 8;
        int cardWidth = (width - 24 - gap * 3) / 4;
        int cardHeight = 36;

        metricCard(g, 12, topY, cardWidth, cardHeight, "CURRENT FPS",
            Integer.toString(Minecraft.getInstance().getFps()),
            fpsColor(Minecraft.getInstance().getFps()));

        metricCard(g, 12 + cardWidth + gap, topY, cardWidth, cardHeight, "FRAME P95",
            ms(f.p95Ms()), frameColor(f.p95Ms()));

        metricCard(g, 12 + (cardWidth + gap) * 2, topY, cardWidth, cardHeight, "TICK P95",
            ms(t.p95Ms()), tickColor(t.p95Ms()));

        metricCard(g, 12 + (cardWidth + gap) * 3, topY, cardWidth, cardHeight, "HEAP USAGE",
            String.format(Locale.ROOT, "%.0f%% (%d MiB)", f.heapPercent(), f.heapUsed() / 1_048_576),
            heapColor(f.heapPercent()));

        // Two side-by-side graphs
        int graphY = topY + cardHeight + 8;
        int graphH = height - graphY - 66;
        int graphW = (width - 32) / 2;

        drawFrameGraph(g, 12, graphY, graphW, graphH, ClientTelemetry.history(30, graphW - 14), "Frame Time (30s)", true);
        drawTickGraph(g, 20 + graphW, graphY, graphW, graphH, t.nanos(), "Integrated Server Ticks", true);

        // Status & Benchmark strip
        int stripY = height - 54;
        g.text(font, Component.literal("● Alert: " + PerformanceController.latestAlert()), 12, stripY, 0xFFD29922, false);

        String benchText = BenchmarkManager.isRunning()
            ? String.format(Locale.ROOT, "Benchmark: %.0f%% running", BenchmarkManager.progress() * 100)
            : BenchmarkManager.comparison();
        int bw = font.width(benchText);
        g.text(font, Component.literal(benchText), width - bw - 12, stripY, BenchmarkManager.isRunning() ? 0xFF58A6FF : 0xFF8B949E, false);
    }

    private void drawFrames(GuiGraphicsExtractor g) {
        ClientTelemetry.Snapshot f = ClientTelemetry.snapshot();
        int topY = 70;
        int graphH = height - 172;
        drawFrameGraph(g, 12, topY, width - 24, graphH,
            ClientTelemetry.history(historySeconds, width - 36),
            "Frame History (" + historySeconds + "s timeline)", false);

        int statY = topY + graphH + 8;
        int chipW = (width - 24 - 25) / 6;
        int chipH = 32;

        statChip(g, 12, statY, chipW, chipH, "AVG FRAME", String.format(Locale.ROOT, "%.2f ms", f.avgMs()), 0xFF58A6FF);
        statChip(g, 12 + (chipW + 5), statY, chipW, chipH, "P95 FRAME", String.format(Locale.ROOT, "%.2f ms", f.p95Ms()), frameColor(f.p95Ms()));
        statChip(g, 12 + (chipW + 5) * 2, statY, chipW, chipH, "P99 FRAME", String.format(Locale.ROOT, "%.2f ms", f.p99Ms()), frameColor(f.p99Ms()));
        statChip(g, 12 + (chipW + 5) * 3, statY, chipW, chipH, "1% LOW FPS", String.format(Locale.ROOT, "%.1f FPS", f.lowOnePercentFps()), fpsColor((int) f.lowOnePercentFps()));
        statChip(g, 12 + (chipW + 5) * 4, statY, chipW, chipH, "STUTTERS >50ms", Integer.toString(f.stutters()), f.stutters() > 0 ? 0xFFD29922 : 0xFF2EA043);
        statChip(g, 12 + (chipW + 5) * 5, statY, chipW, chipH, "FREEZES >250ms", Integer.toString(f.freezes()), f.freezes() > 0 ? 0xFFF85149 : 0xFF2EA043);
    }

    private void drawTicks(GuiGraphicsExtractor g) {
        TickProfiler.Snapshot t = TickProfiler.snapshot();
        int topY = 70;
        int graphH = height - 172;
        drawTickGraph(g, 12, topY, width - 24, graphH, t.nanos(), "Integrated Server Tick History", false);

        int statY = topY + graphH + 8;
        int chipW = (width - 24 - 20) / 5;
        int chipH = 32;

        statChip(g, 12, statY, chipW, chipH, "AVG TICK", String.format(Locale.ROOT, "%.2f ms", t.avgMs()), tickColor(t.avgMs()));
        statChip(g, 12 + (chipW + 5), statY, chipW, chipH, "P95 TICK", String.format(Locale.ROOT, "%.2f ms", t.p95Ms()), tickColor(t.p95Ms()));
        statChip(g, 12 + (chipW + 5) * 2, statY, chipW, chipH, "MIN TICK", String.format(Locale.ROOT, "%.2f ms", t.minMs()), 0xFF2EA043);
        statChip(g, 12 + (chipW + 5) * 3, statY, chipW, chipH, "MAX TICK", String.format(Locale.ROOT, "%.2f ms", t.maxMs()), tickColor(t.maxMs()));
        statChip(g, 12 + (chipW + 5) * 4, statY, chipW, chipH, "SAMPLED TICKS", Integer.toString(t.samples()), 0xFF58A6FF);
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        ClientTelemetry.Snapshot f = ClientTelemetry.snapshot();
        int topY = 70;
        int panelH = height - 116;
        int colW = (width - 24 - 16) / 3;

        // Card 1: Engine & Client
        drawPanelCard(g, 12, topY, colW, panelH, "Engine & Client Status", new String[]{
            "Client Status: §f" + PerformanceController.status(),
            "Adaptive Optimization: " + statusPill(LagFixerConfig.isAdaptiveEnabled()),
            "Live Particle Cap: §f" + PerformanceController.effectiveParticleCap(),
            "Live Render Distance: §f" + PerformanceController.effectiveRenderDistance() + " chunks",
            "Weather Reduction: " + statusPill(LagFixerConfig.isWeatherReductionEnabled()),
            "Performance Alerts: " + statusPill(LagFixerConfig.areAlertsEnabled())
        });

        // Card 2: Memory & Garbage Collection
        int col2X = 12 + colW + 8;
        g.fill(col2X, topY, col2X + colW, topY + panelH, 0xFF161B22);
        g.outline(col2X, topY, colW, panelH, 0xFF30363D);
        g.text(font, Component.literal("Memory & Garbage Collection"), col2X + 10, topY + 8, 0xFF58A6FF, false);

        int memY = topY + 28;
        g.text(font, Component.literal(String.format(Locale.ROOT, "Heap: %d / %d MiB (%.1f%%)",
            f.heapUsed() / 1_048_576, f.heapMax() / 1_048_576, f.heapPercent())), col2X + 10, memY, 0xFFF0F6FC, false);

        int barW = colW - 20;
        int fillW = (int) (Math.min(100.0, f.heapPercent()) / 100.0 * barW);
        g.fill(col2X + 10, memY + 14, col2X + 10 + barW, memY + 22, 0xFF21262D);
        g.fill(col2X + 10, memY + 14, col2X + 10 + fillW, memY + 22, heapColor(f.heapPercent()));
        g.outline(col2X + 10, memY + 14, barW, 8, 0xFF30363D);

        g.text(font, Component.literal("GC Collections: §f" + f.gcCount()), col2X + 10, memY + 36, 0xFF8B949E, false);
        g.text(font, Component.literal("GC Total Pause: §f" + f.gcTimeMs() + " ms"), col2X + 10, memY + 52, 0xFF8B949E, false);
        g.text(font, Component.literal("Runtime: §fJava " + System.getProperty("java.version")), col2X + 10, memY + 68, 0xFF8B949E, false);

        // Card 3: Particles & Alerts
        int col3X = 12 + (colW + 8) * 2;
        drawPanelCard(g, col3X, topY, colW, panelH, "Particles & Diagnostics", new String[]{
            "Accepted Particles: §a" + f.acceptedParticles(),
            "Dropped Particles: §c" + f.droppedParticles(),
            "Drop Ratio: §f" + String.format(Locale.ROOT, "%.1f%%",
                (f.acceptedParticles() + f.droppedParticles() == 0) ? 0.0 :
                (f.droppedParticles() * 100.0 / (f.acceptedParticles() + f.droppedParticles()))),
            "Latest Alert: §e" + PerformanceController.latestAlert(),
            "Note: Dynamic live caps apply",
            "instantly without restart."
        });
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int topY = 70;
        int colW = (width - 24 - 16) / 3;

        drawToggleCard(g, 12, topY, colW, 66, "Adaptive Optimization",
            "Live particle & distance throttling", LagFixerConfig.isAdaptiveEnabled());

        drawToggleCard(g, 12 + colW + 8, topY, colW, 66, "Lag Alerts",
            "In-game notifications for tick drops", LagFixerConfig.areAlertsEnabled());

        drawToggleCard(g, 12 + (colW + 8) * 2, topY, colW, 66, "Weather Reduction",
            "Reduces rain & snow particle load", LagFixerConfig.isWeatherReductionEnabled());

        int lowerY = topY + 74;
        int lowerH = height - lowerY - 42;
        g.fill(12, lowerY, width - 12, lowerY + lowerH, 0xFF161B22);
        g.outline(12, lowerY, width - 24, lowerH, 0xFF30363D);

        g.text(font, Component.literal("Active Configuration & Preset Levels"), 22, lowerY + 10, 0xFF58A6FF, false);

        int cy = lowerY + 56;
        g.text(font, Component.literal("Particle Cap: §f" + LagFixerConfig.getMaxParticles() + " §7(Live effective: §a" + PerformanceController.effectiveParticleCap() + "§7)"), 22, cy, 0xFFF0F6FC, false);
        cy += 14;
        g.text(font, Component.literal("Render Distance Cap: §f" + LagFixerConfig.getRenderDistCap() + " chunks §7(Live effective: §a" + PerformanceController.effectiveRenderDistance() + "§7)"), 22, cy, 0xFFF0F6FC, false);
        cy += 14;
        g.text(font, Component.literal("Entity Distances: §7Animals §f" + LagFixerConfig.getAnimalRenderDistance() + "m§7 • Monsters §f" + LagFixerConfig.getMonsterRenderDistance() + "m§7 • Items §f" + LagFixerConfig.getItemRenderDistance() + "m§7 • Deco §f" + LagFixerConfig.getDecorationRenderDistance() + "m"), 22, cy, 0xFFF0F6FC, false);
        cy += 16;
        g.text(font, Component.literal("§6Notice: Logic mixins require game restart. Live particle & distance limits take effect immediately."), 22, cy, 0xFFD29922, false);
    }

    private void drawCompatibility(GuiGraphicsExtractor g) {
        int topY = 70;
        int panelH = height - topY - 44;
        int colW = (width - 32) / 2;

        // Left Panel: Core Mixins
        int col1X = 12;
        g.fill(col1X, topY, col1X + colW, topY + panelH, 0xFF161B22);
        g.outline(col1X, topY, colW, panelH, 0xFF30363D);
        g.text(font, Component.literal("LagFixer Core Optimizations (Mixins)"), col1X + 10, topY + 8, 0xFF58A6FF, false);

        int cy = topY + 26;
        for (Map.Entry<String, MixinStatus.State> entry : MixinStatus.snapshot().entrySet()) {
            MixinStatus.State state = entry.getValue();
            boolean applied = state.applied();
            String badge = applied ? "§a[ ACTIVE ] " : "§6[ INACTIVE ] ";
            g.text(font, Component.literal(badge + entry.getKey()), col1X + 10, cy, 0xFFF0F6FC, false);
            g.text(font, Component.literal("  " + state.reason()), col1X + 10, cy + 11, 0xFF8B949E, false);
            cy += 24;
            if (cy > topY + panelH - 20) break;
        }

        // Right Panel: Companion Mods
        int col2X = 20 + colW;
        g.fill(col2X, topY, col2X + colW, topY + panelH, 0xFF161B22);
        g.outline(col2X, topY, colW, panelH, 0xFF30363D);
        g.text(font, Component.literal("Detected Companion Performance Mods"), col2X + 10, topY + 8, 0xFF58A6FF, false);

        cy = topY + 26;
        String[] mods = {"sodium", "lithium", "ferritecore", "iris", "modmenu", "cloth-config"};
        String[] descriptions = {
            "Modern rendering engine & chunk meshing",
            "General-purpose server & physics optimizations",
            "Memory usage & model allocation reductions",
            "Modern shader pack pipeline",
            "Mod list interface & configuration screen",
            "Rich configuration GUI library"
        };
        for (int i = 0; i < mods.length; i++) {
            String mod = mods[i];
            boolean loaded = FabricLoader.getInstance().isModLoaded(mod);
            String badge = loaded ? "§a[ INSTALLED ] " : "§7[ NOT FOUND ] ";
            g.text(font, Component.literal(badge + mod.toUpperCase(Locale.ROOT)), col2X + 10, cy, 0xFFF0F6FC, false);
            g.text(font, Component.literal("  " + descriptions[i]), col2X + 10, cy + 11, 0xFF8B949E, false);
            cy += 24;
        }
    }

    private void drawFrameGraph(GuiGraphicsExtractor g, int x, int y, int w, int h, long[] values, String title, boolean compact) {
        g.fill(x, y, x + w, y + h, 0xFF161B22);
        g.outline(x, y, w, h, 0xFF30363D);
        g.text(font, Component.literal(title), x + 8, y + 6, 0xFFF0F6FC, false);

        int bottom = y + h - 8;
        int areaH = Math.max(1, h - 28);
        int usableW = Math.max(1, w - 14);

        int y60 = bottom - (int) (16.66 / 100.0 * areaH);
        g.horizontalLine(x + 6, x + w - 6, y60, 0x602EA043);
        if (!compact || w > 180) {
            g.text(font, Component.literal("16.6ms (60 FPS)"), x + w - font.width("16.6ms (60 FPS)") - 8, y60 - 4, 0x802EA043, false);
        }

        int y30 = bottom - (int) (33.33 / 100.0 * areaH);
        g.horizontalLine(x + 6, x + w - 6, y30, 0x60D29922);

        if (values != null && values.length > 0) {
            int num = values.length;
            for (int i = 0; i < num; i++) {
                double ms = values[i] / 1_000_000.0;
                int px = x + 7 + i * usableW / num;
                int barH = (int) (Math.min(100.0, ms) / 100.0 * areaH);
                if (barH < 1) barH = 1;
                g.verticalLine(px, bottom - barH, bottom, frameColor(ms));
            }
        }
    }

    private void drawTickGraph(GuiGraphicsExtractor g, int x, int y, int w, int h, long[] values, String title, boolean compact) {
        g.fill(x, y, x + w, y + h, 0xFF161B22);
        g.outline(x, y, w, h, 0xFF30363D);
        g.text(font, Component.literal(title), x + 8, y + 6, 0xFFF0F6FC, false);

        int bottom = y + h - 8;
        int areaH = Math.max(1, h - 28);
        int usableW = Math.max(1, w - 14);

        int y50 = bottom - (int) (50.0 / 100.0 * areaH);
        g.horizontalLine(x + 6, x + w - 6, y50, 0x60D29922);
        if (!compact || w > 180) {
            g.text(font, Component.literal("50ms (20 TPS)"), x + w - font.width("50ms (20 TPS)") - 8, y50 - 4, 0x80D29922, false);
        }

        if (values != null && values.length > 0) {
            int num = values.length;
            for (int i = 0; i < num; i++) {
                double ms = values[i] / 1_000_000.0;
                int px = x + 7 + i * usableW / num;
                int barH = (int) (Math.min(100.0, ms) / 100.0 * areaH);
                if (barH < 1) barH = 1;
                g.verticalLine(px, bottom - barH, bottom, tickColor(ms));
            }
        }
    }

    private void metricCard(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String value, int accent) {
        g.fill(x, y, x + w, y + h, 0xFF161B22);
        g.outline(x, y, w, h, 0xFF30363D);
        g.fill(x, y, x + 3, y + h, accent);

        g.text(font, Component.literal(label), x + 8, y + 5, 0xFF8B949E, false);
        g.text(font, Component.literal(value), x + 8, y + 18, 0xFFF0F6FC, false);
        g.fill(x + w - 10, y + 6, x + w - 5, y + 11, accent);
    }

    private void statChip(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String value, int color) {
        g.fill(x, y, x + w, y + h, 0xFF161B22);
        g.outline(x, y, w, h, 0xFF30363D);
        g.text(font, Component.literal(label), x + 6, y + 4, 0xFF8B949E, false);
        g.text(font, Component.literal(value), x + 6, y + 16, color, false);
    }

    private void drawPanelCard(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, String[] lines) {
        g.fill(x, y, x + w, y + h, 0xFF161B22);
        g.outline(x, y, w, h, 0xFF30363D);
        g.text(font, Component.literal(title), x + 10, y + 8, 0xFF58A6FF, false);

        int cy = y + 26;
        for (String line : lines) {
            g.text(font, Component.literal(line), x + 10, cy, 0xFFC9D1D9, false);
            cy += 15;
        }
    }

    private void drawToggleCard(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, String desc, boolean on) {
        g.fill(x, y, x + w, y + h, 0xFF161B22);
        g.outline(x, y, w, h, 0xFF30363D);
        g.fill(x, y, x + 3, y + h, on ? 0xFF2EA043 : 0xFF484F58);

        g.text(font, Component.literal(title), x + 8, y + 8, 0xFFF0F6FC, false);
        g.text(font, Component.literal(desc), x + 8, y + 22, 0xFF8B949E, false);
        g.text(font, Component.literal(on ? "§a● ACTIVE" : "§7○ DISABLED"), x + 8, y + 44, 0xFFFFFFFF, false);
    }

    private void applyPreset(LagFixerConfig.Preset preset) {
        LagFixerConfig.applyPreset(preset);
        LagFixerConfig.save();
        notice = preset + " preset applied";
    }

    private void exportDiagnostics() {
        try {
            notice = "Saved " + DiagnosticsExporter.export().getFileName();
        } catch (IOException e) {
            notice = "Export failed: " + e.getMessage();
        }
    }

    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }
    private static String statusPill(boolean value) { return value ? "§a[ON]" : "§7[OFF]"; }
    private static String ms(double value) { return String.format(Locale.ROOT, "%.2f ms", value); }
    private static int fpsColor(int fps) { return fps >= 60 ? 0xFF2EA043 : fps >= 30 ? 0xFFD29922 : 0xFFF85149; }
    private static int frameColor(double ms) { return ms <= 16.66 ? 0xFF2EA043 : ms <= 33.33 ? 0xFFD29922 : 0xFFF85149; }
    private static int tickColor(double ms) { return ms <= 50.0 ? 0xFF2EA043 : ms <= 75.0 ? 0xFFD29922 : 0xFFF85149; }
    private static int heapColor(double percent) { return percent < 70 ? 0xFF2EA043 : percent < 85 ? 0xFFD29922 : 0xFFF85149; }

    // ── Custom Modern UI Widgets ─────────────────────────────────────────────

    private final class DashboardTabButton extends AbstractWidget {
        private final Tab targetTab;

        public DashboardTabButton(int x, int y, int width, int height, Tab targetTab) {
            super(x, y, width, height, Component.literal(targetTab.name()));
            this.targetTab = targetTab;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            tab = targetTab;
            rebuildButtons();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            boolean active = (tab == targetTab);
            boolean hovered = isHoveredOrFocused();

            int bg;
            int border;
            int textColor;

            if (active) {
                bg = 0xFF1F6FEB;
                border = 0xFF388BFD;
                textColor = 0xFFFFFFFF;
            } else if (hovered) {
                bg = 0xFF30363D;
                border = 0xFF58A6FF;
                textColor = 0xFFFFFFFF;
            } else {
                bg = 0xFF21262D;
                border = 0xFF30363D;
                textColor = 0xFFC9D1D9;
            }

            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            g.outline(getX(), getY(), getWidth(), getHeight(), border);
            if (active) {
                g.horizontalLine(getX(), getX() + getWidth(), getY() + getHeight() - 2, 0xFF58A6FF);
            }

            Font f = Minecraft.getInstance().font;
            g.centeredText(f, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private static final class ModernButton extends AbstractWidget {
        private final Runnable onPress;
        private final boolean primary;

        public ModernButton(int x, int y, int width, int height, Component message, Runnable onPress, boolean primary) {
            super(x, y, width, height, message);
            this.onPress = onPress;
            this.primary = primary;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            boolean hovered = isHoveredOrFocused();
            int bg;
            int border;
            int textColor;

            if (primary) {
                bg = hovered ? 0xFF2EA043 : 0xFF238636;
                border = hovered ? 0xFF3FB950 : 0xFF2EA043;
                textColor = 0xFFFFFFFF;
            } else {
                bg = hovered ? 0xFF30363D : 0xFF21262D;
                border = hovered ? 0xFF58A6FF : 0xFF30363D;
                textColor = hovered ? 0xFFFFFFFF : 0xFFC9D1D9;
            }

            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            g.outline(getX(), getY(), getWidth(), getHeight(), border);

            Font f = Minecraft.getInstance().font;
            g.centeredText(f, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
