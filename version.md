# Lag Fixer 1.0.0-beta (Minecraft 26.3)

Release date: October 25, 2026  
Target environment: Minecraft 26.3, Fabric Loader >= 0.19.5, Java 25  
File: `lagfixer-1.0.0.jar` (73937 bytes)

## Changes in this beta release

- Added native F8 dashboard screen featuring a six-tab layout for Overview, Frames, Ticks, Status, Settings, and Compatibility.
- Added keybinding toggle logic so pressing F8 while inside the dashboard screen closes it immediately.
- Corrected HUD overlay text alpha rendering in Minecraft 26.3 by supplying full opacity (`0xFFFFFFFF`) to `GuiGraphicsExtractor.text()`.
- Implemented client frame telemetry and rolling integrated-server tick profiling with percentile metrics.
- Added live particle limits, render distance caps, and adaptive weather particle reduction during frame pressure.
- Added Potato, Balanced, and Quality presets with one-click buttons on the settings panel.
- Added CSV diagnostic report exports through `/lagfixer profile` and the dashboard interface.
- Added companion mod detection for Sodium, Lithium, FerriteCore, Iris, Mod Menu, and Cloth Config.
