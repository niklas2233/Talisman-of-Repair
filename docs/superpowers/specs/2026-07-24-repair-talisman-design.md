# Repair Talisman — Design Spec

## Summary

"Talisman of Repair" is a mod item that automatically repairs damaged gear over time, shipped as two independent mods — one for Fabric, one for NeoForge — targeting the current latest stable Minecraft release. Repairs are cooldown-gated (1 durability point per interval) and cost XP levels (configurable, 0 = free). Adds soft/optional support for the [Accessories](https://github.com/wisp-forest/accessories) trinket API (repairs equipped accessories too) and an in-game config screen (ModMenu + Cloth Config on Fabric, native on NeoForge) — matching the dependency/config pattern used in the user's `cobblemon-autobattle` mod.

Repo: [niklas2233/Talisman-of-Repair](https://github.com/niklas2233/Talisman-of-Repair)

## Architecture

Two independent, self-contained mod projects in this repo:

```
talismanofrepair/
  fabric/     — Fabric + Fabric API mod
  neoforge/   — NeoForge mod
```

No shared build tooling (no Architectury). Each project duplicates the same logic independently — justified because the mod is small (one item, one behavior) and this avoids multiloader build complexity for something this size. If the logic ever needs to grow significantly, revisit with Architectury common-module split.

Each mod has its own `docs`-free source tree following that loader's standard mod template (`fabric.mod.json` / `neoforge.mods.toml`, `assets/talismanofrepair/...`).

## Item

- Mod ID: `talismanofrepair`
- Item ID: `talismanofrepair:talisman_of_repair`
- Display name: "Talisman of Repair"
- Plain `Item` (not armor, not a tool) — sits anywhere in inventory, no equip slot required.
- Not stackable beyond 1 (it's a persistent-effect item, stacking doesn't make sense).

### Texture

64×64 PNG (an amulet with an anvil icon, provided by the user at `assets/talisman_of_repair.png`) at `assets/talismanofrepair/textures/item/talisman_of_repair.png` in each mod, plus a standard `item/generated` model JSON. 64 is a clean multiple of 16, so it renders as a crisp higher-resolution icon under vanilla's texture handling.

### Recipe

Shaped crafting recipe, identical in both mods:

```
D P D
P E P
D P D
```

- `D` = Diamond
- `P` = Ender Pearl
- `E` = Emerald (center)

Output: 1 Talisman of Repair.

## Repair Logic

Repair behavior:

1. **Trigger**: override `Item#inventoryTick(ItemStack, Level, Entity, int, boolean)` on the talisman's `Item` class. This is vanilla `Item` API present identically on both Fabric and NeoForge — the game already calls it once per tick for every stack sitting in a player's inventory (main inventory, armor, offhand). No custom tick-event registration needed for the trigger itself.
2. **Cooldown gate**: server-side only (`!level.isClientSide()`). Use vanilla `player.getCooldowns()` (`ItemCooldowns`), keyed on the talisman item type:
   - If the item is currently on cooldown for this player, do nothing this tick.
   - Otherwise, read `cooldownTicks` from config:
     - `-1` → ability disabled entirely, do nothing (no cooldown applied).
     - `0` → no cooldown applied, but proceeds to step 3 every tick.
     - `> 0` → apply that cooldown to the player for this item, then proceed to step 3.
   - Cooldown is consumed whenever the check runs, independent of whether a repair actually happens.
3. **XP gate**: read `xpLevelCost` from config.
   - `0` → skip this check entirely, always proceed (free).
   - `> 0` → if `player.experienceLevel < xpLevelCost` (using level-based cost, matching how anvils spend levels), do nothing further this pass — the repair is skipped for this interval, but the cooldown from step 2 still applies (so it retries next interval). Otherwise, deduct `xpLevelCost` via `player.giveExperienceLevels(-xpLevelCost)` once for the whole pass (not per item), then proceed to step 4.
4. **Repair scan**: iterate the player's main inventory + armor slots + offhand (vanilla `Inventory` fields — no capability/`IItemHandler` abstraction needed for the base case), **plus** every equipped Accessories slot when the Accessories mod is present (see below). For each stack found:
   - Skip if empty.
   - Skip if not damageable (`stack.isDamageableItem()`), not repairable (`stack.isRepairable()`), or already at full durability (`stack.getDamageValue() == 0`).
   - Skip if this is the player's current main-hand stack **and** `player.swinging` is true (avoids a stack-reference edge case during the attack animation).
   - Otherwise: `stack.setDamageValue(stack.getDamageValue() - 1)` — repairs exactly 1 durability point.

No support for container/multiblock repair (e.g. repairing items sitting in a chest or block entity) — out of scope for this mod.

## Accessories Integration (soft dependency)

[wisp-forest/accessories](https://github.com/wisp-forest/accessories) is the modern Fabric+NeoForge trinket/accessory API (successor to Curios). Support is a **soft/optional dependency** — the mod works fully without Accessories installed, and gains equipped-accessory repair when it is.

- Both mods add Accessories as compile-only (`modCompileOnly` on Fabric, `compileOnly` on NeoForge) against `io.wispforest:accessories-fabric` / `io.wispforest:accessories-neoforge` (from `maven.wispforest.io`) — never a hard runtime dependency.
- All direct references to Accessories classes live in one isolated helper class per mod (e.g. `AccessoriesIntegration`), never touched unless a presence check passes first (`FabricLoader.getInstance().isModLoaded("accessories")` on Fabric, `ModList.get().isLoaded("accessories")` on NeoForge) — same isolation technique used for the Cloth Config screen below, so referencing an absent mod's classes never throws `NoClassDefFoundError`.
- When present: step 4 of the repair scan additionally calls `AccessoriesCapability.get(player)` (null-safe) and iterates `.getAllEquipped()`, applying the same eligibility checks (damageable, repairable, damaged) to each equipped accessory's stack. No main-hand swing exception applies to these (they're not held).

## ModMenu + Config Screen

Both mods use `net.neoforged.neoforge.common.ModConfigSpec` for config, giving a single config-definition style across both projects (source duplicated between the two mods, consistent with the "two separate mods" decision — not a shared module).

- **NeoForge**: `ModConfigSpec` is native to NeoForge. Registering it automatically gets a "Config" button in the mod list screen — no extra code or dependency needed.
- **Fabric**: `ModConfigSpec` isn't native, so Fabric adds [Forge Config API Port](https://github.com/Fuzss/forgeconfigapiport) (`fuzs.forgeconfigapiport:forgeconfigapiport-fabric`, `modImplementation` — a real runtime dependency, this one's required) which implements the same `ModConfigSpec` class under Fabric, backed by a real config file on disk.
- **Fabric config screen**: [ModMenu](https://github.com/TerraformersMC/ModMenu) + [Cloth Config](https://github.com/shedaniel/cloth-config) integration, both `modCompileOnly` (soft/optional, exactly mirroring the pattern in `cobblemon-autobattle`):
  - `ModMenuIntegration implements ModMenuApi`, registered via the `"modmenu"` entrypoint in `fabric.mod.json`.
  - `getModConfigScreenFactory()` checks Cloth Config's presence via `Class.forName(...)` before referencing any of its classes; returns `parent -> null` if absent, otherwise a factory building a Cloth Config screen with one entry per config value (`cooldownTicks`, `xpLevelCost`).
  - NeoForge needs none of this — its config screen is automatic.

### Config

| Key | Default | Meaning |
|---|---|---|
| `cooldownTicks` | `20` | Ticks between repair passes. `-1` disables the talisman's ability entirely. |
| `xpLevelCost` | `1` | XP levels charged per repair pass. `0` disables the cost (free). |

## Testing

Minecraft mod logic isn't practically unit-testable in isolation (needs a running game/level). The verification is a manual smoke test per mod, run once each is buildable:

1. Build and launch the dev client (`./gradlew runClient` for both loaders).
2. `/give` the talisman, confirm the crafting recipe also works in a crafting table.
3. Damage several items (tool, armor piece) and confirm they repair 1 durability roughly every second while the talisman sits anywhere in inventory.
4. Confirm no repair happens with `cooldownTicks = -1`.
5. Confirm XP drains at the configured rate, and repairs pause (without erroring) when XP is insufficient, resuming once XP is available again.
6. Confirm `xpLevelCost = 0` repairs for free with no XP change.
7. Confirm the main-hand item is skipped mid-swing (attack an entity while wielding a damaged weapon and watch it not repair on that exact tick).
8. With Accessories **not** installed: confirm both mods still load and repair main inv/armor/offhand normally (no crash, no missing-class errors).
9. With Accessories installed: equip a damaged item in an accessory slot and confirm it repairs on the same cadence.
10. Fabric only, ModMenu **not** installed: confirm the mod still loads (soft dependency).
11. Fabric only, ModMenu + Cloth Config installed: open the config screen from ModMenu, change `cooldownTicks`/`xpLevelCost`, confirm the change persists to the config file and takes effect in-game.
12. NeoForge: confirm the "Config" button in the mod list opens a working screen for the same two values, with no ModMenu involved.
