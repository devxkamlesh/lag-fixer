# Lag Fixer — Build Plan
> Minecraft 26.3 · Fabric · devxkamlesh
> Created from full technical research. Ready to build from next session.

---

## Verified Toolchain

| Component | Version | Notes |
|---|---|---|
| Minecraft | `26.3` | Latest stable as of Sep 2026 |
| Fabric Loader | `0.19.5` | Latest stable |
| Fabric Loom | `1.18.2` | Gradle plugin |
| Mappings | Mojang official | **No Yarn for 26.x** — use `loom.officialMojangMappings()` |
| Fabric API | `0.161.0+26.3` | Hard dependency |
| Cloth Config | `26.3.159+fabric` | Soft dep — `modCompileOnly` |
| Mod Menu | `21.0.0` | Soft dep — `modCompileOnly` |
| Java | `25` | Minimum for 26.x |
| Gradle Wrapper | `9.6.0` | |

> **26.x is fully unobfuscated.** No refmap, no obfuscation step. Class and method names you write are the exact runtime names. Verify any mixin target against [mcsrc.dev](https://mcsrc.dev) for the 26.3 build before shipping.

---

## Module/Package Layout

```
lag-fixer/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── src/main/
│   ├── java/com/devxkamlesh/lagfixer/
│   │   ├── LagFixerMod.java                          ← ClientModInitializer
│   │   ├── config/
│   │   │   ├── LagFixerConfig.java                   ← Properties read/write
│   │   │   └── LagFixerConfigScreen.java             ← Cloth Config screen builder
│   │   ├── compat/
│   │   │   └── LagFixerModMenuIntegration.java       ← ModMenuApi entrypoint
│   │   ├── mixin/
│   │   │   ├── LagFixerMixinPlugin.java              ← IMixinConfigPlugin
│   │   │   ├── LagFixerCompat.java                   ← Compat/conflict detection
│   │   │   ├── hopper/
│   │   │   │   └── HopperBlockEntityMixin.java
│   │   │   ├── entity/
│   │   │   │   ├── MobEntityMixin.java
│   │   │   │   └── EntityMixin.java
│   │   │   ├── redstone/
│   │   │   │   └── RedStoneWireBlockMixin.java
│   │   │   ├── collision/
│   │   │   │   └── AbstractBlockStateMixin.java
│   │   │   └── render/
│   │   │       ├── ParticleManagerMixin.java
│   │   │       └── GameRendererMixin.java
│   │   └── profiler/
│   │       └── TickProfiler.java                     ← Rolling FPS/tick logger
│   └── resources/
│       ├── fabric.mod.json
│       ├── lagfixer.mixins.json                      ← Logic mixins (both sides)
│       ├── lagfixer.client.mixins.json               ← Render mixins (client only)
│       └── assets/lagfixer/
│           └── icon.png
```

---

## `gradle.properties`

```properties
# Mod
mod_version=1.0.0
maven_group=com.devxkamlesh
archives_base_name=lagfixer

# Toolchain
minecraft_version=26.3
loader_version=0.19.5
loom_version=1.18.2
fabric_version=0.161.0+26.3

# Soft deps
cloth_config_version=26.3.159+fabric
modmenu_version=21.0.0
```

---

## `build.gradle` skeleton

```groovy
plugins {
    id 'fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

sourceCompatibility = JavaVersion.VERSION_25
targetCompatibility = JavaVersion.VERSION_25

version = project.mod_version
group   = project.maven_group

repositories {
    maven { name = "shedaniel";     url = "https://maven.shedaniel.me/" }
    maven { name = "Terraformers";  url = "https://maven.terraformersmc.com/" }
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_version}"

    // Soft deps — compile-only: users get features when installed, but not required
    modCompileOnly "me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}"
    modCompileOnly "com.terraformersmc:modmenu:${modmenu_version}"
}

processResources {
    inputs.property "version", project.version
    filteringCharset "UTF-8"
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}
```

---

## `fabric.mod.json`

```json
{
  "schemaVersion": 1,
  "id": "lagfixer",
  "version": "${version}",
  "name": "Lag Fixer",
  "description": "Performance optimizations for low-end hardware — hopper, mob AI, redstone, collision, and render capping. No behavior changes.",
  "authors": [ "devxkamlesh" ],
  "contact": {
    "homepage": "https://modrinth.com/mod/lag-fixer",
    "sources":  "https://github.com/devxkamlesh/lag-fixer",
    "issues":   "https://github.com/devxkamlesh/lag-fixer/issues"
  },
  "license": "All Rights Reserved",
  "icon": "assets/lagfixer/icon.png",
  "environment": "client",

  "entrypoints": {
    "client": [
      "com.devxkamlesh.lagfixer.LagFixerMod"
    ],
    "modmenu": [
      "com.devxkamlesh.lagfixer.compat.LagFixerModMenuIntegration"
    ]
  },

  "mixins": [
    "lagfixer.mixins.json",
    "lagfixer.client.mixins.json"
  ],

  "depends": {
    "fabricloader": ">=0.19.5",
    "minecraft":    "~26",
    "java":         ">=25",
    "fabric-api":   "*"
  },
  "recommends": {
    "cloth-config": "*"
  },
  "suggests": {
    "modmenu": "*"
  },

  "custom": {
    "modmenu": {
      "update_checker": true,
      "links": {
        "modmenu.source": "https://github.com/devxkamlesh/lag-fixer",
        "modmenu.issues": "https://github.com/devxkamlesh/lag-fixer/issues"
      }
    }
  }
}
```

> `update_checker: true` is all that's needed. Mod Menu hashes the installed JAR, finds it on Modrinth automatically, and shows an outdated badge — no URL to configure.

---

## Mixin Config Files

### `lagfixer.mixins.json` (logic — loads on both sides)

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.devxkamlesh.lagfixer.mixin",
  "compatibilityLevel": "JAVA_21",
  "plugin": "com.devxkamlesh.lagfixer.mixin.LagFixerMixinPlugin",
  "mixins": [
    "hopper.HopperBlockEntityMixin",
    "entity.MobEntityMixin",
    "entity.EntityMixin",
    "redstone.RedStoneWireBlockMixin",
    "collision.AbstractBlockStateMixin"
  ],
  "client": [],
  "server": [],
  "injectors": { "defaultRequire": 1 }
}
```

### `lagfixer.client.mixins.json` (render — client only)

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.devxkamlesh.lagfixer.mixin",
  "compatibilityLevel": "JAVA_21",
  "plugin": "com.devxkamlesh.lagfixer.mixin.LagFixerMixinPlugin",
  "mixins": [],
  "client": [
    "render.ParticleManagerMixin",
    "render.GameRendererMixin"
  ],
  "server": [],
  "injectors": { "defaultRequire": 1 }
}
```

---

## Conditional Mixin Architecture

### How it works

The `LagFixerMixinPlugin` implements `IMixinConfigPlugin`. Its `shouldApplyMixin()` is called for every mixin before class loading — the only window to gate an injection. It derives the category from the mixin's package path (`.mixin.hopper.X` → `hopper`) then checks two things:

1. Is the category toggled on in `lagfixer.properties`?
2. Is a conflicting mod loaded that already covers this category?

Both checks run in `onLoad()` / `shouldApplyMixin()` — **not** in `ModInitializer` (too late).

### Conflict map

| Category | Auto-disabled if installed |
|---|---|
| `hopper` | lithium |
| `entity` | lithium |
| `redstone` | lithium |
| `collision` | ferritecore |
| `render` | sodium (partial — check specific targets) |

Disabling is logged via SLF4J so the player can see it in `latest.log`.

### Config file (`lagfixer.properties`)

```properties
# All categories default ON. Set to false to disable.
mixin.hopper=true
mixin.entity=true
mixin.redstone=true
mixin.collision=true
mixin.render=true
```

The Cloth Config screen writes to this same file. The MixinPlugin reads it at JVM startup — changes take effect on next launch (not hot-reloadable, which is correct for mixin-level toggles).

---

## Mixin Targets (Mojang Mappings, verified for 26.x)

> **Always verify against [mcsrc.dev](https://mcsrc.dev) for the exact 26.3 build before writing the descriptor.** Names below are confirmed stable since 1.21 but Mojang can rename between minor versions.

| Category | Target Class | Target Method | Injection Strategy |
|---|---|---|---|
| Hopper | `net.minecraft.world.level.block.entity.HopperBlockEntity` | `tick(Level, BlockPos, BlockState, HopperBlockEntity)` — static | `@At("HEAD")`, cancellable — skip transfer poll if cooldown not expired |
| Entity AI | `net.minecraft.world.entity.Mob` | `customServerAiStep(ServerLevel)` | `@At("HEAD")`, cancellable — skip AI for idle/distant mobs |
| Redstone | `net.minecraft.world.level.block.RedStoneWireBlock` | `updatePowerStrength(Level, BlockPos, BlockState)` | `@At("HEAD")` — cache last computed power level, skip if input unchanged |
| Collision | `net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase` | `getShape(BlockGetter, BlockPos)` | `@Redirect` or `@ModifyReturnValue` — cache VoxelShape per (state, pos) |
| Particles | `net.minecraft.client.particle.ParticleEngine` | `tick()` | `@At("HEAD")` — enforce particle count cap from config |
| Render dist. | `net.minecraft.client.renderer.GameRenderer` or Options | Render distance field access | Intercept video option write to enforce cap |

> **Note on Entity naming:** Yarn called this class `MobEntity`; Mojang mappings name it `Mob`. Since 26.x uses Mojang mappings, use `Mob`.

---

## Feature Implementation Notes

### Hopper Optimization
Replace per-tick polling with a dirty-flag: add a `boolean lagfixer$dirty` field via `@Unique` to `HopperBlockEntity`. When a neighboring container changes (via a separate Mixin on container inventory change events), set the flag. The tick Mixin skips `tryMoveItems` when the flag is false. Reset flag after a successful transfer.

### Entity/AI Optimization
Track a `lagfixer$aiSkipTick` counter per mob via `@Unique`. Skip `customServerAiStep` every N ticks for mobs that are:
- Not in a loaded chunk near a player
- Not currently in a path/goal that requires per-tick resolution
- `ponytail:` N is configurable (default 4, meaning AI runs at 5 tps instead of 20 for idle mobs). Ceiling: breaks mobs that need frame-perfect AI. Upgrade path: use distance-to-nearest-player bucketing instead of flat N.

### Redstone Optimization
Cache the `int powerLevel` computed by `updatePowerStrength` per `BlockPos`. Invalidate on neighbor update. Skip recomputation if the cached value is fresh.
`ponytail:` simple HashMap cache with no LRU eviction — leaks on large worlds with many redstone blocks. Upgrade path: bound the cache size or use a ChunkPos-scoped map.

### Collision/Shape Caching
`VoxelShape` is expensive to compute. Cache `getShape()` results in a `HashMap<BlockState, VoxelShape>`. BlockState is effectively immutable, so this is safe.

### Particle/Render Capping
Client-side only. Config has two sliders: `maxParticles` (default 500, min 50, max 4000) and `renderDistanceCap` (default 12, min 2, max 32). Both enforced in the relevant Mixin methods.

### Tick/FPS Profiler + HUD Overlay
- `TickProfiler` maintains a circular buffer (rolling 200-tick window) of tick times.
- A keybind (default `F7`) toggles the HUD overlay: renders FPS, avg/min/max tick time, active categories.
- `/lagfixer profile` command dumps the last window's stats to `config/lagfixer-profile.txt` in CSV format.
- Uses `ScheduledRenderMatrixStack` / the Fabric HUD rendering event to draw the overlay.

### Config Screen
Cloth Config with one `ConfigCategory` per optimization category, each containing its toggle boolean. Render category adds the two sliders. Bottom section: three preset buttons (Potato / Balanced / Quality) that set all values and trigger a save. Screen is registered via `ModMenuApi`.

### Presets

| Preset | Hopper | Entity | Redstone | Collision | Particles | Render Dist. |
|---|---|---|---|---|---|---|
| Potato | ✓ | ✓ | ✓ | ✓ | 100 | 6 |
| Balanced | ✓ | ✓ | ✓ | ✓ | 500 | 12 |
| Quality | ✓ | off | off | ✓ | 2000 | 20 |

---

## Build Order (Implementation Sequence)

1. **Scaffolding** — `build.gradle`, `gradle.properties`, `fabric.mod.json`, both mixin JSONs, folder structure, `gradlew wrapper`
2. **Config layer** — `LagFixerConfig` (read/write `lagfixer.properties`), `LagFixerMixinPlugin`, `LagFixerCompat` — the foundation everything else gates on
3. **Hopper mixin** — highest-impact, simplest target; use as the proof-of-concept that the mixin plugin wiring works
4. **Entity/AI mixin**
5. **Redstone mixin**
6. **Collision mixin**
7. **Particle + render-distance mixin** (client)
8. **Tick profiler + HUD overlay + `/lagfixer profile` command**
9. **Cloth Config screen + preset buttons**
10. **Mod Menu integration** (`LagFixerModMenuIntegration`)
11. **README + Modrinth page copy**
12. **Test build**: launch with Fabric API + Cloth Config + Mod Menu, confirm no crash, confirm config screen opens, confirm profile command works

---

## Explicit Non-Goals (do not add)
- No new blocks / items / mobs / biomes
- No behavior changes — every optimization must produce identical results to vanilla
- No shader support or rendering-engine replacement (stay compatible with Sodium/Iris)
- No server admin tooling
- No telemetry
- No duplicate mixin targets where Lithium/FerriteCore already patches (auto-detect and disable instead)

---

## Modrinth Page Copy Draft

**Title:** Lag Fixer

**Summary:**
Lag Fixer reduces tick time and improves FPS on low-end and mid-range hardware — no modpack required, no vanilla behavior changed. It optimizes hopper polling, mob AI ticking, redstone recalculation, and collision shape lookups on the logic side, and caps particles and render distance on the client side. A built-in HUD overlay shows FPS, tick time, and a per-category status line in real time.

**Feature list:**
- Hopper optimization — event-driven transfer checks instead of per-tick polling
- Entity/AI optimization — skip idle-mob AI, cache pathfinding lookups
- Redstone optimization — avoid redundant power-level recalculation on large contraptions
- Collision/shape caching — cache VoxelShape lookups instead of recomputing every check
- Particle and render-distance capping — direct FPS lever, configurable via in-game sliders
- Tick + FPS profiler with HUD overlay and `/lagfixer profile` command
- Preset buttons: Potato, Balanced, Quality
- Auto-disables any category already covered by Lithium, FerriteCore, Sodium, or Iris

**Scope note (required):**
On singleplayer and LAN worlds, all optimizations apply — the integrated server runs in the same process. On a remote dedicated server you do not administer, only the particle and render-distance caps apply. To get the full tick-side benefit on a dedicated server, the server operator must also install Lag Fixer on the server.

---

## Maven Repositories Reference

```
Fabric:         https://maven.fabricmc.net/
Shedaniel:      https://maven.shedaniel.me/
Terraformers:   https://maven.terraformersmc.com/
```

---

*Sources: fabricmc.net, meta.fabricmc.net, modrinth.com API, maven.shedaniel.me, maven.terraformersmc.com, github.com/TerraformersMC/ModMenu, github.com/SpongePowered/Mixin/wiki, mcsrc.dev*
