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
| Cleaner     | Worldme-Cleaner    | `net.momirealms:craft-engine-core:26.7.4`, `net.momirealms:craft-engine-bukkit:26.7.4` | Soft-depends on `CraftEngine`. Trash GUI command `/wmcleaner` (alias `/wmc`). |
| Mail        | Worldme-Mail       | `org.xerial:sqlite-jdbc:3.47.1.0` (shaded), `craft-engine-core/bukkit:26.7.4` | Soft-depends on `CraftEngine`. Command `/wmmail` (alias `/wmm`). Exposes `MailApi` via `getApi()` for other modules. |
| Ownership   | Worldme-Ownership   | `com.github.MilkBowl:VaultAPI:1.7.1`              | Soft-depends on `Vault`. Command `/wmbind` (alias `/wmb`). |
| Market      | Worldme-Market     | `org.xerial:sqlite-jdbc:3.47.1.0` (shaded), `com.github.MilkBowl:VaultAPI:1.7.1`, `craft-engine-core/bukkit:26.7.4` | Depends on `Worldme-Mail` and `Vault`; soft-depends on `CraftEngine`. Command `/wmmarket` (alias `/wmmk`). Calls the Mail API reflectively and delivers sales/expiry items by mail. |
| Territory   | Worldme-Territory  | `org.xerial:sqlite-jdbc:3.47.1.0` (shaded), `com.github.MilkBowl:VaultAPI:1.7.1`, `me.clip:placeholderapi:2.11.6` | SQLite-backed claims (16×16×16 units), subregions, members/permissions/settings, protection listener, events and `TerritoryApi` via `getApi()`. Soft-depends on `Vault`, `PlaceholderAPI`. Command `/wmterritory` (alias `/wml`). |
| Guild       | Worldme-Guild      | `org.xerial:sqlite-jdbc:3.47.1.0` (shaded), `com.github.MilkBowl:VaultAPI:1.7.1`, `me.clip:placeholderapi:2.11.6`, `compileOnly(project(":Territory"))` | SQLite-backed guilds; each guild owns a `Worldme-Territory` region (`guildUuid(id)=new UUID(0L,id)`). Hard-depends on `Worldme-Territory`; soft-depends on `Vault`, `PlaceholderAPI`. Command `/wmguild` (alias `/wmg`). Extensible feature registry (`beacon`, `warehouse`) with structure scanning from `structures.yml`; exposes `GuildApi` via `getApi()`. |

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
  - Exception: `Guild` intentionally `compileOnly`-depends on `Territory` and declares `depend: [Worldme-Territory]`, since a guild's functional carrier is a Territory region.
- SQLite-backed modules fat-jar `sqlite-jdbc` into the plugin; config/UI text is MiniMessage; messages live in `config.yml`, GUI layout in `menu.yml`.

## Repositories

Declared in root `build.gradle.kts`:

- `mavenCentral()`
- `https://repo.papermc.io/repository/maven-public/`
- `https://repo.momirealms.net/releases/`

`Guild`/`Territory` also declare `https://repo.extendedclip.com/content/repositories/placeholderapi/` locally for PlaceholderAPI.

## Testing

There are currently no test sources. Verification is manual: build the project and install the produced JAR from `out/` into the server's `plugins/` folder.
