# Talisman of Repair

<img src="assets/talisman_of_repair.png" alt="Talisman of Repair icon" width="64" height="64">

An item that automatically repairs your gear over time, for Minecraft 1.21.1. Inspired by
[ProjectE](https://github.com/sinkillerj/ProjectE)'s Talisman of Repair, rebuilt here as a
standalone mod with no ProjectE dependency.

- `fabric/` — Fabric + Fabric API build
- `neoforge/` — NeoForge build

Both are independent Gradle projects; build each with its own `./gradlew build`.

## Behavior

- Repairs 1 durability point every `cooldownTicks` (default 20, `-1` disables) across your
  main inventory, armor, offhand, and equipped Accessories (if installed).
- Costs `xpLevelCost` XP levels per repair pass (default 1, `0` = free).
- Configurable in-game: ModMenu + Cloth Config on Fabric, the native "Config" screen on NeoForge.
