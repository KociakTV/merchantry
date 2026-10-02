# Merchantry

**A server-side shop, economy and player market for Minecraft 1.21.1 and 1.20.1 — no client install needed.**

Merchantry adds a GUI shop, an optional currency with hard-to-automate ways to earn it, selling to the server, a player market, repairs, homes, permanent utility commands (`/craft`, `/anvil`, `/ec`...) and keepInventory charges. Everything runs on the server — players join with a plain client.

- Modrinth: https://modrinth.com/mod/merchantry
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/merchantry

## Versions

| Folder | Loader | Notes |
|---|---|---|
| [`merchantry_neoforge_1.21.1`](merchantry_neoforge_1.21.1) | NeoForge 21.1 | Version 1.1.0: `/exchange` plus compatibility with Create and addons, AE2, FTB Ultimine, Curios, grave and map mods |
| [`merchantry_fabric_1.21.1`](merchantry_fabric_1.21.1) | Fabric (Loader ≥ 0.16, Fabric API) | Version 1.1.0: compatibility with FTB Ultimine, Trinkets, grave and map mods; no `/exchange`, Create or AE2 (no Fabric 1.21.1 releases) |
| [`merchantry_fabric_1.20.1`](merchantry_fabric_1.20.1) | Fabric (Loader ≥ 0.15, Fabric API) | Version 1.1.0: `/exchange` plus compatibility with Create and addons, AE2, FTB Ultimine, Trinkets, grave and map mods |

Both versions use Mojang mappings, so the code is nearly identical. Shared logic is duplicated, so changes usually have to be applied in both folders.

## Building

Each folder is a standalone Gradle project:

```bash
cd merchantry_neoforge_1.21.1   # or merchantry_fabric_1.21.1 / merchantry_fabric_1.20.1
./gradlew build
```

The jar ends up in `build/libs/`. The 1.21.1 versions target Java 21, the 1.20.1 version Java 17. The Fabric build runs Gradle itself on Java 25 (downloaded automatically, required by Loom 1.18).

## License

[MIT](LICENSE) © KociakTV
