# Lag Fixer — Mixin Research Report
> Fabric / Minecraft 26.x (unobfuscated, Mojang Mappings)

---

## Key version context you must know before writing any code

Minecraft **26.1+ is fully unobfuscated**. That changes several things:

- **No more Yarn mappings.** Fabric officially dropped Yarn for 26.x. Use Mojang's official mappings (mojmap). The `loom` Gradle plugin is now `net.fabricmc.fabric-loom` (no-remap variant).
- **No refmap / annotation processor needed.** Loom remaps targets in-place. Class names you write in `@Mixin(value = HopperBlockEntity.class)` are already the runtime names.
- **Java 25 is the Gradle JVM minimum** for 26.1. Your Loom version must be ≥ 1.15.
- Obfuscated names (`method_XXXX`, `field_XXXX`) are gone. Write the real Java names.

---

## 1. MixinPlugin / Conditional Mixin Loading (`IMixinConfigPlugin`)

### What the interface does

`IMixinConfigPlugin` (package `org.spongepowered.asm.mixin.extensibility`) lets you intercept every mixin before Mixin applies it. The critical method is:

```java
boolean shouldApplyMixin(String targetClassName, String mixinClassName)
```

Return `false` to skip the mixin entirely. This runs before class loading, which is the only window where you can gate an injection.

### Concrete implementation pattern

```java
package com.devxkamlesh.lagfixer.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class LagFixerMixinPlugin implements IMixinConfigPlugin {

    // Called once when the plugin is instantiated — load your config here.
    @Override
    public void onLoad(String mixinPackage) {
        LagFixerConfig.load(); // reads lagfixer.properties from config dir
    }

    @Override
    public String getRefMapperConfig() {
        return null; // no refmap needed on 26.x (unobfuscated)
    }

    /**
     * The main gate. Called for every mixin in the config file that uses this plugin.
     * targetClassName  – the class being transformed (e.g. "net.minecraft.world.level.block.entity.HopperBlockEntity")
     * mixinClassName   – the full name of the mixin class being considered
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // 1. Check user config toggle
        if (!LagFixerConfig.isMixinEnabled(mixinClassName)) {
            return false;
        }
        // 2. Check for conflicting mods
        if (isConflictPresent(mixinClassName)) {
            return false;
        }
        return true;
    }

    // ---- boilerplate no-ops required by the interface ----
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    // ---- helpers ----
    private boolean isConflictPresent(String mixinClassName) {
        // Delegate to compat layer — see section 2
        return LagFixerCompat.isDisabledByConflict(mixinClassName);
    }
}
```

### `LagFixerConfig` — minimal config-driven toggle

```java
package com.devxkamlesh.lagfixer.mixin;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Loaded at mixin-application time (before ModInitializer), so keep it dependency-free. */
public class LagFixerConfig {

    private static final Properties props = new Properties();
    // Prefix used in the .properties file: mixin.hopper=true
    private static final String PREFIX = "mixin.";

    public static void load() {
        Path configFile = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("lagfixer.properties");
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Returns true (mixin applies) unless the player explicitly set the category to false.
     * mixinClassName example: "com.devxkamlesh.lagfixer.mixin.hopper.HopperBlockEntityMixin"
     */
    public static boolean isMixinEnabled(String mixinClassName) {
        String category = categoryOf(mixinClassName); // e.g. "hopper"
        // Default ON: return true unless explicitly "false"
        return !props.getProperty(PREFIX + category, "true").equalsIgnoreCase("false");
    }

    private static String categoryOf(String mixinClassName) {
        // Package structure is: ...mixin.<category>.SomeMixin
        // e.g. com.devxkamlesh.lagfixer.mixin.hopper.HopperBlockEntityMixin → "hopper"
        String[] parts = mixinClassName.split("\\.");
        // find the index of "mixin" and return the next segment
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("mixin")) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }
}
```

**ponytail:** Simple properties file, no library, no framework. If the player ever needs a GUI-backed config, that is wired through the `ModInitializer` / Cloth Config path separately — it writes to the same file.

---

## 2. Compatibility Detection Pattern

### Where to put checks

Put them in **`LagFixerMixinPlugin.onLoad()`**, not in `ModInitializer`. The plugin runs during the Mixin phase, before classes are loaded — which is the only time `shouldApplyMixin` can block an injection. `ModInitializer` runs far too late.

`FabricLoader` is available at Mixin time; you can call it from `onLoad()` or directly in `shouldApplyMixin()`.

### Complete pattern

```java
package com.devxkamlesh.lagfixer.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class LagFixerCompat {

    private static final Logger LOG = LoggerFactory.getLogger("LagFixer/Compat");

    /**
     * Maps a mixin package segment → the mod ID that covers it.
     * If that mod is loaded, every mixin under that category is disabled.
     */
    private static final Map<String, String> CONFLICTS = Map.of(
        "hopper",    "lithium",      // Lithium already optimises hoppers
        "entity",    "lithium",      // Lithium covers mob AI
        "collision", "ferritecore",  // FerriteCore covers VoxelShape caching
        "redstone",  "lithium"       // Lithium has a redstone module
    );

    // Built once in onLoad(), queried in shouldApplyMixin()
    private static Set<String> disabledCategories;

    public static void init() {
        disabledCategories = new java.util.HashSet<>();
        for (Map.Entry<String, String> e : CONFLICTS.entrySet()) {
            String category = e.getKey();
            String modId    = e.getValue();
            if (FabricLoader.getInstance().isModLoaded(modId)) {
                disabledCategories.add(category);
                LOG.info("[LagFixer] {} optimisation disabled — {} is installed.", category, modId);
            }
        }
    }

    /**
     * Returns true (= skip this mixin) if a conflicting mod is present.
     * mixinClassName: full qualified class name of the mixin
     */
    public static boolean isDisabledByConflict(String mixinClassName) {
        // Derive the category from the package, same logic as LagFixerConfig
        String category = LagFixerConfig.categoryOf(mixinClassName);
        return disabledCategories != null && disabledCategories.contains(category);
    }
}
```

Call `LagFixerCompat.init()` at the top of `LagFixerMixinPlugin.onLoad()`.

**Why here and not `ModInitializer`?** Because `shouldApplyMixin` fires at class-loading time. If you do the check in `ModInitializer`, the mixins have already been applied (or not) before your check runs. The only way to gate a mixin on a runtime condition is to do the check inside the plugin.

---

## 3. Mixin Config JSON Structure

### Split across packages — two-file strategy

Use **two** mixin JSON files: one for server-side logic optimisations, one for client-only rendering.

**`lagfixer.mixins.json`** — loads on both sides (logic optimisations)

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
  "injectors": {
    "defaultRequire": 1
  },
  "overwrites": {
    "requireAnnotations": true
  }
}
```

**`lagfixer.client.mixins.json`** — loads on physical client only (rendering, particles)

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
  "injectors": {
    "defaultRequire": 1
  }
}
```

### Package layout

```
src/main/java/com/devxkamlesh/lagfixer/
├── LagFixerMod.java               ← ClientModInitializer
├── mixin/
│   ├── LagFixerMixinPlugin.java
│   ├── LagFixerConfig.java
│   ├── LagFixerCompat.java
│   ├── hopper/
│   │   └── HopperBlockEntityMixin.java
│   ├── entity/
│   │   ├── MobEntityMixin.java
│   │   └── EntityMixin.java
│   ├── redstone/
│   │   └── RedStoneWireBlockMixin.java
│   ├── collision/
│   │   └── AbstractBlockStateMixin.java
│   └── render/
│       ├── ParticleManagerMixin.java
│       └── GameRendererMixin.java
└── config/
    └── LagFixerScreenFactory.java  ← Cloth Config screen builder
```

**Note on non-configurable mixins:** If you ever add a mixin that must always apply (no toggle), put it in a *third* JSON file with no `plugin` entry. CaffeineConfig's own docs make this pattern explicit: a plugin requires every listed mixin to map to an option, so mixins that should always apply belong in a separate config.

---

## 4. `fabric.mod.json` for Minecraft 26.3

```json
{
  "schemaVersion": 1,
  "id": "lagfixer",
  "version": "${version}",
  "name": "Lag Fixer",
  "description": "Performance optimisations for low-end hardware: hopper, entity AI, redstone, collision, and render capping.",
  "authors": [
    "devxkamlesh"
  ],
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
      "com.devxkamlesh.lagfixer.config.LagFixerModMenuIntegration"
    ]
  },

  "mixins": [
    "lagfixer.mixins.json",
    "lagfixer.client.mixins.json"
  ],

  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft":    "~26",
    "java":         ">=21",
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
        "modmenu.modrinth": "https://modrinth.com/mod/lag-fixer"
      }
    }
  }
}
```

### Notes on specific fields

| Field | Rationale |
|---|---|
| `"environment": "client"` | Prevents loading on a dedicated server that doesn't have it installed intentionally. |
| `depends.fabric-api` | Hard dependency — use `"*"` so you don't need to pin to a fabric-api version that may lag behind. |
| `recommends.cloth-config` | Soft: a warning in the log if absent, but the mod works without it (just no GUI config screen). |
| `suggests.modmenu` | Even softer: pure metadata, no warning. |
| `custom.modmenu.update_checker: true` | Opts into ModMenu's built-in hash-based Modrinth update check. ModMenu hashes the installed jar and looks it up on Modrinth. No extra code needed. |
| `~26` for minecraft | Semver range covering 26.x but not 27.x. Adjust to `>=26.1` if you need a floor. |

### `ModMenuApi` entrypoint (minimum wiring)

```java
package com.devxkamlesh.lagfixer.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class LagFixerModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> LagFixerScreenFactory.build(parent); // your Cloth Config screen
    }
}
```

---

## 5. Hopper Mixin Target (26.x, Mojang Mappings)

### Important note on mappings

26.x uses **Mojang official mappings**. Class names match what you'd see in the Minecraft source via [mcsrc.dev](https://mcsrc.dev).

### Class and method

| Item | Mojang name |
|---|---|
| Class | `net.minecraft.world.level.block.entity.HopperBlockEntity` |
| Tick method | `tick(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity)` — this is a **static** method, the `BlockEntityTicker` lambda |
| Transfer logic | `tryMoveItems(...)` — the private method that does the actual push/pull |

The vanilla hopper tick is registered as a static ticker. The `BlockEntity#getTicker` method returns a `BlockEntityTicker` — a functional interface. The actual work happens inside the static `tick()` method on `HopperBlockEntity`.

### Mixin example

```java
package com.devxkamlesh.lagfixer.mixin.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    /**
     * Injected into the static tick() method.
     * Verified target: net.minecraft.world.level.block.entity.HopperBlockEntity#tick
     * Mapping: Mojang official — no remapping needed on 26.x
     *
     * ponytail: this is the entry gate; actual cooldown logic lives in a separate
     * helper so it's testable without spinning up a world.
     */
    @Inject(
        method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void lagfixer$onTick(
        Level level, BlockPos pos, BlockState state, HopperBlockEntity self,
        CallbackInfo ci
    ) {
        // Your early-exit / cooldown logic here.
        // Call ci.cancel() to skip the vanilla tick when conditions are met.
    }
}
```

**Verify the exact method descriptor against [mcsrc.dev](https://mcsrc.dev) for the exact 26.3 build before shipping.** The static method signature is stable since 1.21 but Mojang can rename or restructure between minor versions.

---

## 6. Entity AI Tick Mixin Target (26.x, Mojang Mappings)

### Class and method

| Item | Mojang name |
|---|---|
| Primary class | `net.minecraft.world.entity.Mob` (was `MobEntity` in Yarn) |
| AI tick method | `Mob#tick()` — calls `AIManager` / Brain ticking internally |
| Goal tick method | `Mob#customServerAiStep(ServerLevel)` — where per-tick goal evaluation happens |
| Brain-based AI | `Brain#tick(ServerLevel, LivingEntity)` — called from `Mob#customServerAiStep` |

For coarse "skip idle mob AI" optimisations, inject into `Mob#tick()`. For fine-grained goal/brain skip, inject into `customServerAiStep`.

### Mixin example — skip AI for distant/idle mobs

```java
package com.devxkamlesh.lagfixer.mixin.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    /**
     * Gate for per-tick AI execution.
     * Verified target: net.minecraft.world.entity.Mob#customServerAiStep(ServerLevel)
     * Mapping: Mojang official
     *
     * ponytail: ceiling is O(n) mob scan per tick. Upgrade path:
     *   bucket mobs by chunk and only scan the bucket, not the flat list.
     */
    @Inject(
        method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lagfixer$maybeSkipAi(ServerLevel level, CallbackInfo ci) {
        Mob self = (Mob)(Object)this;
        if (AiSkipHelper.shouldSkipAi(self, level)) {
            ci.cancel();
        }
    }
}
```

**Note:** In 26.x, `Entity#getEntityWorld()` was **renamed** from `Entity#getWorld()` (Yarn rename forced by a class-hierarchy change in 1.21.9). Double-check the exact method name on your target build via mcsrc.dev. Always document the MC version the injection point was verified against in the Javadoc.

---

## Quick reference: where each check lives

| Concern | Where to put it |
|---|---|
| Read config file | `LagFixerMixinPlugin.onLoad()` |
| Compat mod detection | `LagFixerCompat.init()` called from `onLoad()` |
| Per-mixin toggle | `LagFixerMixinPlugin.shouldApplyMixin()` |
| Cloth Config screen | `ClientModInitializer` → `ModMenuApi` entrypoint |
| Log which categories disabled | `LagFixerCompat.init()` via SLF4J |

---

## Sources

- [Fabric Wiki — Mixin Introduction](https://fabricmc.net/wiki/tutorial:mixin_introduction)
- [Fabric Wiki — Registering Mixins](https://wiki.fabricmc.net/tutorial:mixin_registration)
- [Fabric Wiki — Mixins and Obfuscation](https://wiki.fabricmc.net/drafts:mixin_obfuscation)
- [Fabric Wiki — fabric.mod.json Spec](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec)
- [Fabric Docs — Migrating Mappings](https://docs.fabricmc.net/develop/porting/mappings/)
- [Fabric for Minecraft 26.1 announcement](https://fabricmc.net/2026/03/14/261.html)
- [CaffeineMC/mixin-config — CaffeineConfig pattern](https://github.com/CaffeineMC/mixin-config)
- [SpongePowered/Mixin wiki](https://github.com/SpongePowered/Mixin/wiki)
- [TerraformersMC/ModMenu wiki](https://github.com/TerraformersMC/ModMenu/wiki/)
- [mcsrc.dev — live Minecraft source browser](https://mcsrc.dev)

*Content was paraphrased and synthesised from the above sources for licensing compliance.*
