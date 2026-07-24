# Repair Talisman — Design Spec

## Summary

A standalone (no ProjectE dependency) reimplementation of ProjectE's "Talisman of Repair" item, shipped as two independent mods — one for Fabric, one for NeoForge — targeting the current latest stable Minecraft release. The core repair logic mirrors ProjectE's actual `RepairTalisman.java` implementation, with one deliberate deviation: repairs cost XP levels (configurable, 0 = free), where the original is entirely free.

Reference: [`RepairTalisman.java`](https://github.com/sinkillerj/ProjectE/blob/master/src/main/java/moze_intel/projecte/gameObjs/items/RepairTalisman.java) and `ServerConfig.java` in [sinkillerj/ProjectE](https://github.com/sinkillerj/ProjectE).

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
- Not stackable beyond 1 (matches original — it's a persistent-effect item, stacking doesn't make sense).

### Texture

Generated placeholder 16×16 PNG (simple icon, flat design) at `assets/talismanofrepair/textures/item/talisman_of_repair.png`, plus a standard `item/generated` model JSON. Swappable later without code changes.

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

Ported behavior from the real `RepairTalisman.java`, plus an XP cost layered on top:

1. **Trigger**: override `Item#inventoryTick(ItemStack, Level, Entity, int, boolean)` on the talisman's `Item` class. This is vanilla `Item` API present identically on both Fabric and NeoForge — the game already calls it once per tick for every stack sitting in a player's inventory (main inventory, armor, offhand). No custom tick-event registration needed for the trigger itself.
2. **Cooldown gate**: server-side only (`!level.isClientSide()`). Use vanilla `player.getCooldowns()` (`ItemCooldowns`), keyed on the talisman item type:
   - If the item is currently on cooldown for this player, do nothing this tick.
   - Otherwise, read `cooldownTicks` from config:
     - `-1` → ability disabled entirely, do nothing (no cooldown applied).
     - `0` → no cooldown applied, but proceeds to step 3 every tick.
     - `> 0` → apply that cooldown to the player for this item, then proceed to step 3.
   - This matches the original's `checkCooldown` exactly: cooldown is consumed whenever the check runs, independent of whether a repair actually happens.
3. **XP gate (deviation from original)**: read `xpLevelCost` from config.
   - `0` → skip this check entirely, always proceed (free, matches original behavior).
   - `> 0` → if `player.experienceLevel < xpLevelCost` (using level-based cost, matching how anvils spend levels), do nothing further this pass — the repair is skipped for this interval, but the cooldown from step 2 still applies (so it retries next interval). Otherwise, deduct `xpLevelCost` via `player.giveExperienceLevels(-xpLevelCost)` once for the whole pass (not per item), then proceed to step 4.
4. **Repair scan**: iterate the player's main inventory + armor slots + offhand (vanilla `Inventory` fields — no capability/`IItemHandler` abstraction needed since Curios integration is explicitly out of scope). For each stack:
   - Skip if empty.
   - Skip if not damageable (`stack.isDamageableItem()`), not repairable (`stack.isRepairable()`), or already at full durability (`stack.getDamageValue() == 0`).
   - Skip if this is the player's current main-hand stack **and** `player.swinging` is true (avoids the same attack-animation edge case the original guards against).
   - Otherwise: `stack.setDamageValue(stack.getDamageValue() - 1)` — repairs exactly 1 durability point.

No support for Alchemical Bags, Alchemical Chests, or Pedestals — those are ProjectE-specific container/multiblock systems this standalone mod doesn't have equivalents for, and are out of scope.

### Config

Per-loader native config (NeoForge `ModConfigSpec`, Fabric a simple JSON config), both exposing:

| Key | Default | Meaning |
|---|---|---|
| `cooldownTicks` | `20` | Ticks between repair passes. `-1` disables the talisman's ability entirely. |
| `xpLevelCost` | `1` | XP levels charged per repair pass. `0` disables the cost (free, matches original). |

## Testing

Minecraft mod logic isn't practically unit-testable in isolation (needs a running game/level). The verification is a manual smoke test per mod, run once each is buildable:

1. Build and launch the dev client (`./gradlew runClient` for both loaders).
2. `/give` the talisman, confirm the crafting recipe also works in a crafting table.
3. Damage several items (tool, armor piece) and confirm they repair 1 durability roughly every second while the talisman sits anywhere in inventory.
4. Confirm no repair happens with `cooldownTicks = -1`.
5. Confirm XP drains at the configured rate, and repairs pause (without erroring) when XP is insufficient, resuming once XP is available again.
6. Confirm `xpLevelCost = 0` repairs for free with no XP change.
7. Confirm the main-hand item is skipped mid-swing (attack an entity while wielding a damaged weapon and watch it not repair on that exact tick).
