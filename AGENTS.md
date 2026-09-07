# AGENTS.md

## Project overview

A Gradle multi-project of custom Bukkit/Paper plugins for a single Minecraft server (`WorldmeServer`).
Each subproject builds one independent plugin JAR.

- Server platform: **Purpur** for Minecraft **26.2** (write it as `26.2`, not `1.26.2`).
- API dependency: `io.papermc.paper:paper-api:26.2.build.+`
- Group: `top.worldme`, version: `1.0.0`
- Base package: `top.worldme`

## Modules

| Subproject  | Plugin name        | Extra compile-only deps                          | Notes                                     |
|-------------|--------------------|--------------------------------------------------|-------------------------------------------|
| AutoClear   | Worldme-AutoClear  | none                                             | Simple plugin, no external soft-depends.  |
| Trigger     | Worldme-Trigger    | `net.luckperms:api:5.5`                          | Soft-depends on `LuckPerms`, `Multiverse-Core`. Command `/worldmetrigger` (alias `/wmt`). |
| Accessories | Worldme-Accessories| `net.momirealms:craft-engine-core:26.7.4`        | Soft-depends on `CraftEngine`.            |
| ItemSkin    | Worldme-ItemSkin   | `net.momirealms:craft-engine-bukkit:26.7.4`      | Soft-depends on `CraftEngine`.            |
| Fishing     | Worldme-Fishing    | `net.momirealms:custom-fishing:2.3.24`, `net.momirealms:craft-engine-core:26.7.4`, `net.momirealms:craft-engine-bukkit:26.7.4` | Soft-depends on `CustomFishing` and `CraftEngine`. Command `/wmfishing` (alias `/wmf`), `/wmfishing open <player>` for NPCs. |

## Build commands

Use the Gradle wrapper (`gradlew.bat` on Windows, `gradlew` elsewhere).

```bash
# Build all plugin JARs
./gradlew build

# Build a single plugin
./gradlew :AutoClear:build

# Clean build artifacts including the output directory
./gradlew clean
```

- The root `jar` task is intentionally disabled; plugins only build from subprojects.
- Built JARs are written to the repo root `out/` directory as `Worldme-{Project}-{version}.jar`.
- `clean` deletes `out/` as well as Gradle build dirs.

## Conventions

- Plugin metadata lives in each module at `src/main/resources/plugin.yml`.
- Each plugin `main` class is `top.worldme.{ProjectName}` and extends `JavaPlugin`.
- Keep modules independent; do not add cross-module compile dependencies unless required.

## Repositories

Declared in root `build.gradle.kts`:

- `mavenCentral()`
- `https://repo.papermc.io/repository/maven-public/`
- `https://repo.momirealms.net/releases/`

## Testing

There are currently no test sources. Verification is manual: build the project and install the produced JAR from `out/` into the server's `plugins/` folder.
