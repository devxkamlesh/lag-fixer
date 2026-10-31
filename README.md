# Lag Fixer

A client-side Fabric performance toolkit for Minecraft **26.3 only**, targeting low-end and mid-range hardware.  
By [devxkamlesh](https://modrinth.com/user/devxkamlesh) · [Modrinth](https://modrinth.com/mod/lag-fixer) · [Issues](https://github.com/devxkamlesh/lag-fixer/issues)

**License:** All Rights Reserved. See [LICENSE](LICENSE) for reuse restrictions.

---

## What it does

| Optimisation | What it fixes | Auto-disabled if |
|---|---|---|
| Hopper | Replaces per-tick polling with a dirty-flag — hoppers only check when a nearby container changes | Lithium |
| Entity/AI | Skips mob AI evaluation every N ticks for mobs far from players | Lithium |
| Redstone | Caches wire power levels, skips redundant recalculation on large contraptions | Lithium |
| Collision | Caches VoxelShape lookups per block state instead of recomputing each check | FerriteCore |
| Particles | Caps particle count to a configurable limit | Sodium (partial) |
| Render distance | Caps render distance to a configurable limit | — |

The profiler and render limits are the release-safe baseline. Hopper, entity, redstone, and collision mixins are **experimental and off by default** because they can affect timing or context-sensitive behavior.

---

## Important — where it works

| Environment | Hopper / Entity / Redstone / Collision | Particles / Render cap |
|---|---|---|
| Singleplayer | ✅ Full effect | ✅ |
| LAN host | ✅ Full effect | ✅ |
| Dedicated server (you are the admin) | ✅ Install on server too | ✅ |
| Remote server (public SMP etc.) | ❌ Server must install it | ✅ |

The logic-side optimisations (hopper, entity, redstone, collision) require the mod on the machine running the server. On a remote server you don't administer, only the render-side caps apply.

---

## HUD overlay

Press **F7** (rebindable) to toggle a small overlay showing:

```
LagFixer | FPS: 94
Tick: avg 1.84 ms  min 1.12 ms  max 6.20 ms
Active: HOP ENT RED COL RND
```

Run `/lagfixer profile` to dump the last 200 ticks to `config/lagfixer-profiles/profile-TIMESTAMP.csv` and print a summary to chat.

## Performance dashboard

Press **F8**, run `/lagfixer dashboard`, or use the Mod Menu config button to open the native dashboard. It works without Cloth Config and includes:

- Live FPS, average MSPT, p95, p99, and maximum tick-time metrics
- A color-coded 200-tick pressure timeline with 50 ms stall detection
- Runtime mixin status and effective render limits
- One-click safe presets, HUD control, and local CSV export
- A detailed settings button when Cloth Config is installed

### Advanced 26.3 features

- Adaptive p95 frame-time mode with conservative particle-first quality reduction and hysteresis
- Separate frame and integrated-server tick profiling, including p95/p99, 1% low FPS, stutters, and freezes
- Multi-minute frame history with 30-second, 2-minute, and 5-minute views
- Local diagnostic reports containing runtime, memory, mod, config, and compatibility information
- Rate-limited alerts that distinguish client rendering, integrated-server tick, and heap pressure
- 30-second benchmark capture with reusable baseline comparison
- Priority-aware particle admission with accepted/dropped counters
- Render-only animal, monster, item, and decoration distance controls
- Weather-effect reduction during measured frame pressure
- Heap and garbage-collection monitoring without forced garbage collection
- Dashboard status, settings, and compatibility tabs with restart guidance

---

## Config

Requires **Mod Menu** + **Cloth Config** for the in-game config screen.  
Without them, the mod runs on safe defaults (everything enabled).

Manual config: edit `config/lagfixer.properties` in your `.minecraft` folder.

```properties
mixin.hopper=false
mixin.entity=false
mixin.redstone=false
mixin.collision=false
mixin.render=true
render.maxParticles=500
render.distanceCap=12
entity.aiSkipTicks=4
```

Mixin changes take effect on the next launch. Render limits are read live.

### Presets

| Preset | Hopper | Entity | Redstone | Collision | Particles | Render dist |
|---|---|---|---|---|---|---|
| Potato | ❌ | ❌ | ❌ | ❌ | 100 | 6 |
| Balanced | ❌ | ❌ | ❌ | ❌ | 500 | 12 |
| Quality | ❌ | ❌ | ❌ | ❌ | 2000 | 20 |

---

## Building from source

**Requirements:** JDK 25, Git

```bash
git clone https://github.com/devxkamlesh/lag-fixer.git
cd lag-fixer
./gradlew build
```

Output jar: `build/libs/lagfixer-1.0.0.jar`

## Release checklist

Before publishing to Modrinth or CurseForge:

1. Run `./gradlew build` locally or via CI
2. Confirm the jar is generated in `build/libs/`
3. Verify Fabric API and Java 25 requirements are listed clearly
4. Select **All Rights Reserved** on the project page; do not select MIT
5. Upload `lagfixer-1.0.0.jar` with the appropriate platform description
6. Add genuine screenshots and a short changelog summary

---

## Compatibility

| Mod | Notes |
|---|---|
| Lithium | Auto-disables hopper, entity, redstone categories |
| FerriteCore | Auto-disables collision category |
| Sodium | Compatible; render category auto-disabled for overlapping targets |
| Iris | Compatible; no shared mixin targets |
| Mod Menu | Supported — config button in mod list |
| Cloth Config | Supported — in-game config screen |

---

## Other mods by devxkamlesh

- [Smart FPS Booster](https://modrinth.com/mod/smart-fps-booster)
- [AutoInventorySorter](https://modrinth.com/mod/autoinventorysorter)
- [Mod Menu Pro](https://modrinth.com/mod/modmenu-pro)
- [Totem Auto Equip](https://modrinth.com/mod/totem-auto-equip)
