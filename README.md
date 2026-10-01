# Merchantry

**A server-side shop, economy and player market for Minecraft 1.21.1 — no client install needed.**

Merchantry adds a GUI shop, an optional currency with hard-to-automate ways to earn it, selling to the server, a player market, repairs, homes, permanent utility commands (`/craft`, `/anvil`, `/ec`...) and keepInventory charges. Everything runs on the server — players join with a plain client.

- Modrinth: https://modrinth.com/mod/merchantry
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/merchantry

## Versions

| Folder | Loader | Notes |
|---|---|---|
| [`merchantry_neoforge_1.21.1`](merchantry_neoforge_1.21.1) | NeoForge 21.1 | Full feature set, including the `/exchange` for Create: Numismatics |
| [`merchantry_fabric_1.21.1`](merchantry_fabric_1.21.1) | Fabric (Loader ≥ 0.16, Fabric API) | Same features except `/exchange` (Numismatics has no Fabric 1.21.1 release) |

Both versions use Mojang mappings, so the code is nearly identical. Shared logic is duplicated, so changes usually have to be applied in both folders.

## Building

Each folder is a standalone Gradle project:

```bash
cd merchantry_neoforge_1.21.1   # or merchantry_fabric_1.21.1
./gradlew build
```

The jar ends up in `build/libs/`. The mod targets Java 21. The Fabric build runs Gradle itself on Java 25 (downloaded automatically, required by Loom 1.18).

## License

[MIT](LICENSE) © KociakTV
