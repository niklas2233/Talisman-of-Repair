# Talisman of Repair

An item that automatically repairs your gear over time, for Minecraft 1.21.1.

- `fabric/` — Fabric + Fabric API build
- `neoforge/` — NeoForge build

Both are independent Gradle projects; build each with its own `./gradlew build`.

## Behavior

- Repairs 1 durability point every `cooldownTicks` (default 20, `-1` disables) across your
  main inventory, armor, offhand, and equipped Accessories (if installed).
- Costs `xpLevelCost` XP levels per repair pass (default 1, `0` = free).
- Configurable in-game: ModMenu + Cloth Config on Fabric, the native "Config" screen on NeoForge.

See `docs/superpowers/specs/2026-07-24-repair-talisman-design.md` for the full design and
`docs/superpowers/plans/2026-07-24-repair-talisman-implementation.md` for the build plan.
