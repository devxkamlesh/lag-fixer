# Modrinth project details

- **Type:** Mod
- **Name:** Lag Fixer
- **URL slug:** `lag-fixer`
- **Project URL:** https://modrinth.com/mod/lag-fixer
- **Owner:** `devxkamlesh`
- **Visibility:** Public after approval
- **License:** All Rights Reserved (ARR)
- **Summary:** Client-side Fabric performance tools for Minecraft 26.3: FPS and tick-time monitoring, particle and render-distance limits, adaptive controls, and local profiling.

## Modrinth publishing checklist

- [ ] **Project information:** Create a project of type **Mod**, name it **Lag Fixer**, use the `lag-fixer` slug if available, verify the owner is `devxkamlesh`, and choose public visibility when ready for review.
- [ ] **Summary:** Use the concise summary above. Keep the distinction between client FPS/frame-time and server tick-time clear.
- [ ] **Description:** Paste the full description below into the Description field. Preview the page and check headings, lists, and links after saving.
- [ ] **License:** Select **All Rights Reserved** in Modrinth's license selector. Confirm it agrees with the included `LICENSE` file; do not select MIT.
- [ ] **Version:** Upload `build/libs/lagfixer-1.0.0.jar`. Set version number `1.0.0`, game version `26.3`, loader **Fabric**, and release channel **Release** only if this is your intended stable release. Add **Fabric API** as required; list **Mod Menu** and **Cloth Config** as optional.
- [ ] **Icon and gallery:** Upload `src/main/resources/assets/lagfixer/icon.png` as the project icon. Add genuine screenshots of the HUD and dashboard if available; do not use mockups as screenshots.
- [ ] **Tags and categories:** Choose only supported, accurate tags, such as **Fabric**, **Client-side**, **Utility**, **Performance**, and **Optimization**, according to the choices offered in the form.
- [ ] **Disclosures:** Answer every disclosure question truthfully for the actual uploaded files and project. Do not mark unrelated content or functionality just to improve discoverability.
- [ ] **Links:** Add the source repository and issue tracker from the project metadata; verify both links open correctly.
- [ ] **Members and ownership:** Confirm the owner and invited project members have the intended permissions.
- [ ] **Review and submit:** Verify the jar, supported version, dependency declarations, license, icon, and description before submitting. Public visibility and approval depend on Modrinth's review process.
- [ ] **After publishing:** Check the project page and analytics when available, respond to user reports, and publish updates only after building and testing the corresponding release.

# Lag Fixer

**A client-side Fabric performance toolkit for Minecraft 26.3.** Monitor frame-time and integrated-server tick-time, limit selected rendering workloads, and investigate stutters with an in-game HUD, dashboard, and local CSV reports.

Lag Fixer is for players who want a practical Minecraft lag fix that starts with measurement rather than promises. It gives you visibility into performance, lets you tune a few client-side quality limits, and keeps experimental gameplay-side optimizations separate from the safer rendering and profiling features. It is designed with low-end and mid-range PCs in mind, but every computer and modpack is different: results depend on your hardware, settings, world, and other installed mods.

## What Lag Fixer does

### Monitor FPS and frame-time

Press **F7** to toggle the compact HUD. It shows current FPS, recent integrated-server tick timing, and active optimization flags. FPS is useful, but it does not tell the whole story: a game can report a high average while still feeling choppy because of occasional long frames. Lag Fixer records client frame times and makes percentile information available in its dashboard so you can look for those spikes.

Press **F8** to open the built-in performance dashboard, or run `/lagfixer dashboard`. The dashboard includes live overview metrics, historical frame and tick views, runtime optimization status, settings, and compatibility information. The frame-history views cover approximately 30 seconds, 2 minutes, and 5 minutes. The tick view helps distinguish client rendering pressure from integrated-server tick pressure in singleplayer or when hosting a LAN world. A 50 ms tick is a useful stall reference, not a promise that every tick will stay below it.

### Tune client rendering limits

The particle cap limits how many pending particle effects Lag Fixer admits. This can help reduce visual workload in particle-heavy scenes, though it may also make some effects less visible. Counters in the dashboard help show how many particle requests were accepted or dropped.

The render-distance cap prevents the in-game render-distance option from exceeding your configured limit. Lower view distance can reduce the amount of scenery the client needs to draw and process, at the cost of seeing less of the world. The Potato, Balanced, and Quality presets use different particle and render-distance limits as convenient starting points; they are not hardware benchmarks and should be adjusted to your preference.

Lag Fixer also includes render-only distance controls for selected entity groups and an option to reduce cosmetic weather effects during measured frame pressure. These are quality trade-offs. Use the dashboard and change one setting at a time so you can tell whether a change actually helps your setup.

### Adaptive controls and alerts

Optional adaptive mode watches recent frame-time percentiles and conservatively reduces particle allowance first when frames remain under pressure. It uses thresholds and cooldowns to avoid changing limits every frame. When performance improves, it can gradually restore settings up to your configured values. Adaptive mode is optional; turn it off if you prefer fixed settings or want to compare repeatable tests.

Optional alerts report measured client-rendering pressure, integrated-server tick pressure, or high JVM heap use. These are diagnostic signals, not proof of a specific cause. For example, high heap use alone does not mean Minecraft needs a forced garbage collection. Lag Fixer observes JVM memory and garbage-collection statistics without forcing garbage collection.

### Profile and benchmark

Use `/lagfixer profile` to save a CSV containing the most recent integrated-server tick samples and summary statistics under `config/lagfixer-profiles/`. The dashboard also provides local diagnostic export with runtime, memory, mod, configuration, and compatibility information. Reports are written locally; review them before sharing because diagnostic reports can reveal details about your runtime and installed mods.

The benchmark feature can capture a timed performance sample and compare it with a saved baseline. For a useful comparison, repeat the same route or scene, keep the same resource packs and graphics settings, and avoid changing several options between runs. A short benchmark is a comparison aid, not a universal score or a guarantee of higher FPS.

## Experimental optimizations

Lag Fixer includes opt-in mixins for hopper checks, distant mob AI evaluation, redstone wire updates, and collision-shape lookups. These can affect timing or context-sensitive gameplay, so they are **disabled by default**. Enable them only if you understand the trade-offs, test them in a copy of your world, and keep a backup. Changing a mixin setting requires restarting Minecraft.

Compatibility checks avoid enabling selected Lag Fixer logic mixins when Lithium or FerriteCore is detected. This is intended to prevent overlapping implementations, not to promise compatibility with every mod or modpack. Review the dashboard's compatibility status and report reproducible conflicts with your mod list and logs.

## Installation and requirements

1. Install **Minecraft Java Edition 26.3**.
2. Install **Fabric Loader 0.19.5 or newer** for that game version.
3. Install **Fabric API** for Minecraft 26.3.
4. Put the Lag Fixer jar in your Minecraft `mods` folder and launch the Fabric profile.
5. Optionally install **Mod Menu** and **Cloth Config** for the menu integration and detailed configuration screen.

The mod is client-side. It is intended for the Fabric client; do not install it as a dedicated-server mod. Client rendering limits can affect what you see while playing multiplayer, but they do not optimize a remote server or change its simulation.

## Configuration and quick start

Start with the Balanced preset, then open the dashboard and observe a normal gameplay scene. If frame pacing is poor, try lowering particles or render distance. If the integrated server is slow in a singleplayer world, rendering changes may not solve the cause; check tick timings and test the world without optional experimental logic mixins. Use the Potato preset only if its reduced visual range and particle count suit you. Quality raises the limits and may increase workload rather than improve performance on a slower PC.

Settings are stored in `config/lagfixer.properties`. Render limits and most client controls are applied live; experimental mixin switches take effect after a restart. Mod Menu and Cloth Config are optional, not required to launch the mod. Keep a copy of your configuration if you are testing changes.

## What this mod can and cannot fix

If you are searching for a **Minecraft lag fix**, **Minecraft FPS boost**, **FPS boost in Minecraft**, or an **FPS booster Minecraft mod**, Lag Fixer provides tools for measuring and managing selected client workloads. It may help make Minecraft feel smoother when particle load, visual range, or frame-time spikes are a meaningful part of the problem. It cannot guarantee a particular FPS increase, and it does not replace graphics drivers, sensible video settings, or troubleshooting a heavily modded installation.

Lag Fixer does **not** improve internet connection quality, reduce ping, fix packet loss, or repair a lagging remote Minecraft server. If your issue is high ping, disconnects, rubber-banding, or server-side TPS, check your network, server status, and route to the host. A client FPS boost cannot correct network latency.

This project targets **Minecraft Java Edition 26.3 on Fabric**. It is not a Bedrock FPS booster, Minecraft PE or mobile lag reducer, Forge FPS booster, OptiFine replacement, or version-specific fix for Minecraft 1.8.9, 1.12.2, 1.16.5, 1.18, or 1.19. Do not install it on those platforms or versions unless a compatible release is explicitly published.

## FAQ

**Will this make Minecraft less laggy?**  
It can help you identify whether frame-time, particles, render distance, or integrated-server ticks are contributing to the experience, and lets you adjust supported client settings. The result depends on the cause of the lag and your system.

**Does Lag Fixer increase FPS?**  
Reducing rendering work can improve FPS or frame consistency in some situations. No fixed FPS increase is guaranteed. Use the dashboard and compare the same scene before and after a change.

**Does it fix server lag or ping?**  
No. The client can report integrated-server timings in singleplayer, but it cannot change the performance or network latency of a remote server.

**Does it work with Sodium?**  
The project is designed to coexist with common client performance mods, but compatibility depends on versions and other installed mods. Check the in-game compatibility information and report issues with logs.

**Are the gameplay optimizations safe to enable?**  
They are experimental and off by default. Back up worlds and test carefully before enabling them.

## Useful search topics

This page covers client-side Minecraft performance, Minecraft lag troubleshooting, Minecraft FPS improvement, particle limits, render distance, frame-time stutters, Fabric performance tools, integrated-server tick monitoring, and how to reduce rendering load in Minecraft Java Edition. These terms describe the mod's actual scope; the project does not claim support for unrelated platforms, versions, ping fixes, or guaranteed FPS gains.
