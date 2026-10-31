# Lag Fixer: Fabric mod build spec

Handoff document for whoever builds this: an AI coding agent, a freelancer, or the author himself. Read the whole file before writing code. Where a decision is marked "confirm with author," ask before proceeding rather than guessing.

## One-line summary

Lag Fixer is a client-side Minecraft Fabric mod that reduces tick time and improves FPS on low-end hardware, without changing vanilla game behavior. It is built and maintained by devxkamlesh, who also publishes Smart FPS Booster, AutoInventorySorter, Mod Menu Pro, and Totem Auto Equip on Modrinth under the same handle.

## Primary use case

The target player runs Minecraft on a weak or mid-range machine (the reference test rig is an Intel i5 10th Gen, GTX 1650, 16GB RAM) and plays singleplayer or hosts a LAN world. That player wants lower tick times and higher FPS in redstone-heavy or mob-farm-heavy worlds, without installing a full modpack or losing vanilla behavior.

A secondary use case is a small survival multiplayer (SMP) server run by friends, where the server operator installs Lag Fixer on both the server and every client to get the tick-side benefits on their own dedicated server.

## Important scope clarification (confirm with author before building)

Lag Fixer is client-side only, meaning its `fabric.mod.json` declares `"environment": "client"`. This has a direct consequence that must be reflected in both the code and the Modrinth page copy:

- In singleplayer and LAN-hosted worlds, the integrated server runs inside the same client process. Mixins targeting hopper ticking, entity AI, redstone calculation, and similar logic-side code take effect automatically, because that code loads in the client JVM.
- On a remote dedicated server the player does not administer (most public SMPs), only the purely client-side optimizations apply: particle capping, render-distance capping, and any rendering-thread work. The tick-loop, hopper, and AI optimizations need the mod installed on that server too, which a normal player cannot do on someone else's server.

The Modrinth description must state this plainly, so players on rented survival servers do not expect logic-side gains they cannot get.

## Target platform

- Minecraft: latest stable release at build time (verify the current version before starting; do not hardcode an old one from this spec).
- Loader: Fabric Loader, latest stable.
- Language: Java 21 (required for Minecraft 1.20.5 and newer).
- Build tool: Gradle with the Fabric Loom plugin.
- Mappings: Yarn. Confirm exact method and field names for the target Minecraft version using a mapping viewer such as Linkie before writing any Mixin. Do not copy method names from this spec as final; they are illustrative only.

## Dependencies

- Fabric API: required, hard dependency.
- FastUtil: already on the classpath through Minecraft's own dependencies. No extra jar needed for basic primitive collections.
- Cloth Config API: optional (soft) dependency, used only to render the settings screen. The mod must still function with sane defaults if Cloth Config is not installed.
- Mod Menu: optional (soft) dependency, used to register the config screen entry and the update-checker badge.

## Feature list

Each optimization ships as its own togglable category, off by itself if the player disables it, so a conflict with one other mod does not force uninstalling all of Lag Fixer.

### Hopper optimization
Replace vanilla's per-tick hopper polling with event-driven checking: a hopper only re-evaluates a transfer when the container beside it actually changes, and caches the result in between. Use case: auto-farms and item-sorting builds with many hoppers, where per-tick polling is a measurable tick-time cost.

### Entity and AI optimization
Cache point-of-interest and pathfinding lookups, skip idle-mob AI ticking where vanilla behavior allows it, and iterate entities in chunk order for better CPU cache locality. Use case: villages, mob farms, and mob-heavy caves.

### Redstone calculation optimization
Optimize redstone wire power-level recalculation to avoid redundant recomputation on large contraptions. Use case: big redstone builds and technical-survival worlds.

### Collision and shape caching
Cache VoxelShape lookups for block collision and raycasting instead of recomputing them on every check. Use case: general world simulation cost, most noticeable with many entities or complex terrain nearby.

### Particle and render-distance capping (client-only)
A client-side slider to cap particle count and render distance independently of the vanilla video settings menu. Use case: the single largest, most direct FPS lever on a weak GPU, and the one optimization here that helps on any server, including ones the player does not administer.

### Tick and FPS profiler with HUD overlay
A keybind-toggled overlay showing current FPS, server tick time in milliseconds, and which optimization categories are active. Back it with a rolling logger that records average, minimum, and maximum tick time over a sample window and can dump the results to a file on command. Use case: lets the player see the effect in real time, and gives the author real before-and-after numbers to publish on the Modrinth page instead of unverified claims.

### Config screen
Built through Mod Menu's `ModMenuApi` entrypoint plus Cloth Config, with one toggle per optimization category described above, plus the particle and render-distance sliders. Registering it as a `modmenu` entrypoint in `fabric.mod.json` is what puts a working config button next to the mod's entry in the Mod Menu list.

### Presets
Three buttons, "Potato," "Balanced," and "Quality," that set every toggle and slider to a predefined combination in one click, for a player who wants a result without understanding what each category does.

### Update checker
Use Mod Menu's built-in update-checker support against the mod's Modrinth page, so an outdated install shows a small badge in the mod list rather than the player having to check manually.

## Explicit non-goals

Do not add any of the following. Each one either duplicates a mod that already does it well or moves Lag Fixer away from its stated purpose.

- No new blocks, items, mobs, biomes, or dimensions.
- No change to vanilla game behavior, drop rates, damage values, or mechanics. Every optimization must produce identical results to vanilla, only faster. This is a hard requirement, not a suggestion.
- No shader support or rendering-engine replacement. That is Sodium and Iris's job; Lag Fixer should stay compatible with both, not compete with either.
- No server administration tooling: no anti-grief, no permissions system, no chat moderation.
- No telemetry or analytics that phones home without the player's explicit opt-in.
- No ads, sponsored content, or monetization code bundled in the jar. Several mod-hosting platforms remove listings for this.
- No duplicating a mixin target that Lithium, FerriteCore, or a similar mod already patches, if that mod is present. Detect it at startup with `FabricLoader.getInstance().isModLoaded(...)` and disable the overlapping category automatically, rather than letting two mods patch the same method and crash.

## Compatibility rules for the builder

- Check for Lithium, FerriteCore, Sodium, and Iris at startup and log which overlapping categories were auto-disabled, so the player can see why in the log file if something seems inactive.
- Every Mixin needs a documented injection point (class, method, and the Yarn mapping version it was verified against), because mappings and method signatures change between Minecraft versions and the next maintainer needs to find them again.
- Keep each optimization category behind its own Mixin config plugin condition, not a single global on/off switch, so the per-category toggles in the settings screen actually do something at the bytecode level and not just in a config file nobody reads.

## Deliverables expected from the builder

1. A working Fabric mod source repository with a clear module layout separating hopper, entity/AI, redstone, and collision optimizations into their own packages.
2. A README covering build instructions, supported Minecraft version, and a list of which optimizations are auto-disabled in the presence of which other mods.
3. A tested build against the current stable Minecraft version, confirmed to launch and load without errors alongside Fabric API, Cloth Config, and Mod Menu.
4. A `/lagfixer profile` command or equivalent keybind that runs the tick profiler for a fixed window and prints the average, minimum, and maximum tick time, formatted for pasting straight into a Modrinth description.
5. A short draft of Modrinth page copy: title, one-paragraph summary, feature list, and the scope clarification from earlier in this document stated in player-facing language.

## Branding

Publish under the existing devxkamlesh handle, matching the tone and format of the author's other four Modrinth listings (Smart FPS Booster, AutoInventorySorter, Mod Menu Pro, Totem Auto Equip). Cross-link Lag Fixer's config screen entry with Mod Menu Pro where the platform allows it, since both mods share an author and a settings-screen use case.