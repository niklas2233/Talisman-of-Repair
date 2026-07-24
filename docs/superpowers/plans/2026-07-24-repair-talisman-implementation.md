# Repair Talisman Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build two working, buildable Minecraft mods (Fabric and NeoForge) implementing the "Talisman of Repair" item per `docs/superpowers/specs/2026-07-24-repair-talisman-design.md`.

**Architecture:** Two fully independent Gradle projects (`fabric/`, `neoforge/`) in this repo, no shared build tooling. Each duplicates the same item/repair/config/Accessories-compat logic in its own source tree, registered through that loader's native APIs.

**Tech Stack:** Java 21, Minecraft 1.21.10, official Mojang mappings on both loaders.

## Global Constraints

- Minecraft version: **1.21.10** (chosen because it's the newest version where Fabric API, NeoForge stable, ModMenu, Cloth Config, Forge Config API Port, and Accessories *all* have real releases — confirmed live against Modrinth/Maven on 2026-07-24).
- Resource pack_format for this MC version: **88** (verified from a real 1.21.10 mod jar).
- Mod ID: `talismanofrepair`. Java package: `com.niklas2233.talismanofrepair`.
- Mappings note: as of 1.21.10, both Fabric and NeoForge toolchains use the class name `net.minecraft.resources.Identifier` (NOT `ResourceLocation` — confirmed against live Fabric API and NeoForge source; Mojang's own proguard mapping still says `ResourceLocation` internally, but both loader toolchains rename it). Use `Identifier.fromNamespaceAndPath(ns, path)`.
- `Item#inventoryTick` signature in 1.21.10 is `inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot)` — confirmed against Mojang's official client mappings for 1.21.10 and a live mod's override (FrozenBlock/WilderWild). It is guaranteed server-side (param type is `ServerLevel`, not `Level`) and still fires for every stack in a player's inventory, not just equipped ones (`slot` is `null` for non-equipped stacks).
- `ItemStack#isRepairable()` no longer exists in 1.21.10. The equivalent check is `stack.isDamaged() && !stack.has(DataComponents.UNBREAKABLE)`.
- `ItemCooldowns#isOnCooldown` / `#addCooldown` now take an `ItemStack`, not an `Item` (confirmed against 1.21.10 official mappings).
- `Inventory` no longer has separate `armor`/`offhand` fields — only `items` (main 36 slots). Equipped items (armor, offhand, mainhand) are read via `LivingEntity#getItemBySlot(EquipmentSlot)`, iterating `EquipmentSlot.values()`.
- No unit tests: Minecraft mod logic requires a running game to exercise meaningfully. Each task's automated verification is **a successful Gradle build** (`BUILD SUCCESSFUL`, confirms compilation + resource validation against the real Minecraft/loader classpath). Manual smoke testing is documented in the spec's Testing section and is out of scope for this plan's automated checks.
- Dependency versions (all verified live against Maven/Modrinth on 2026-07-24 for MC 1.21.10):
  - Fabric Loader: `0.19.3`
  - Fabric API: `0.138.4+1.21.10`
  - Fabric Loom plugin (`net.fabricmc.fabric-loom-remap`): `1.17-SNAPSHOT`
  - NeoForge: `21.10.64`
  - NeoForge ModDev Gradle plugin (`net.neoforged.moddev`): `2.0.142`
  - Forge Config API Port (Fabric): `fuzs.forgeconfigapiport:forgeconfigapiport-fabric:21.10.1` + `fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi:21.10.1`
  - ModMenu: `com.terraformersmc:modmenu:16.0.1`
  - Cloth Config: `me.shedaniel.cloth:cloth-config-fabric:20.0.149`
  - Accessories: `io.wispforest:accessories-fabric:1.4.3-beta` / `io.wispforest:accessories-neoforge:1.4.3-beta`
  - Gradle wrapper: `9.5.1` for the Fabric project, `8.14.5` for the NeoForge project (NeoForge's ModDev plugin documents 8.8+ as its baseline; pinning to the last 8.x avoids an untested Gradle-9 combination on that side, while Fabric's own live example mod already uses 9.5.1 successfully).

---

### Task 1: Fabric project scaffold

**Files:**
- Create: `fabric/settings.gradle`
- Create: `fabric/build.gradle`
- Create: `fabric/gradle.properties`
- Create: `fabric/gradle/wrapper/gradle-wrapper.properties` (+ jar, via real `gradle wrapper` invocation)
- Create: `fabric/src/main/resources/fabric.mod.json`
- Create: `fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`

**Interfaces:**
- Produces: `TalismanOfRepair` class (Fabric `ModInitializer`), `MOD_ID` constant `"talismanofrepair"`, used by every later Fabric task.

- [ ] **Step 1: Download a temporary Gradle 9.5.1 distribution to bootstrap the wrapper**

```bash
cd /tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad
curl -sL -o gradle-9.5.1-bin.zip https://services.gradle.org/distributions/gradle-9.5.1-bin.zip
unzip -q gradle-9.5.1-bin.zip
```

- [ ] **Step 2: Create the Fabric project directory and generate the wrapper**

```bash
mkdir -p ~/talismanofrepair/fabric
cd ~/talismanofrepair/fabric
/tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad/gradle-9.5.1/bin/gradle wrapper --gradle-version 9.5.1
```

Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` in `fabric/`.

- [ ] **Step 3: Clean up the temporary Gradle distribution**

```bash
rm -rf /tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad/gradle-9.5.1 /tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad/gradle-9.5.1-bin.zip
```

- [ ] **Step 4: Write `fabric/settings.gradle`**

```groovy
pluginManagement {
	repositories {
		maven {
			name = 'Fabric'
			url = 'https://maven.fabricmc.net/'
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

rootProject.name = 'talismanofrepair'
```

- [ ] **Step 5: Write `fabric/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

minecraft_version=1.21.10
loader_version=0.19.3
loom_version=1.17-SNAPSHOT

mod_version=1.0.0
maven_group=com.niklas2233

fabric_api_version=0.138.4+1.21.10
fcap_version=21.10.1
modmenu_version=16.0.1
cloth_config_version=20.0.149
accessories_version=1.4.3-beta
```

- [ ] **Step 6: Write `fabric/build.gradle`**

```groovy
plugins {
	id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group

repositories {
	maven { url = 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven/' }
	maven { url = 'https://maven.terraformersmc.com/releases' }
	maven { url = 'https://maven.shedaniel.me/' }
	maven { url = 'https://maven.wispforest.io/releases' }
	maven { url = 'https://maven.su5ed.dev/releases' }
}

loom {
	splitEnvironmentSourceSets()

	mods {
		"talismanofrepair" {
			sourceSet sourceSets.main
			sourceSet sourceSets.client
		}
	}
}

dependencies {
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	mappings loom.officialMojangMappings()
	modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
	modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"

	modImplementation "fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${project.fcap_version}"
	modImplementation "fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi:${project.fcap_version}"

	modCompileOnly "com.terraformersmc:modmenu:${project.modmenu_version}"
	modCompileOnly "me.shedaniel.cloth:cloth-config-fabric:${project.cloth_config_version}"
	modCompileOnly "io.wispforest:accessories-fabric:${project.accessories_version}"
}

processResources {
	def version = project.version
	inputs.property "version", version
	filesMatching("fabric.mod.json") {
		expand "version": version
	}
}

tasks.withType(JavaCompile).configureEach {
	it.options.release = 21
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}
```

- [ ] **Step 7: Write `fabric/src/main/resources/fabric.mod.json`**

```json
{
  "schemaVersion": 1,
  "id": "talismanofrepair",
  "version": "${version}",
  "name": "Talisman of Repair",
  "description": "Adds an item that automatically repairs your gear over time.",
  "authors": ["niklas2233"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.niklas2233.talismanofrepair.TalismanOfRepair"],
    "modmenu": ["com.niklas2233.talismanofrepair.client.ModMenuIntegration"]
  },
  "depends": {
    "fabricloader": ">=0.19.0",
    "minecraft": "1.21.10",
    "java": ">=21",
    "fabric-api": "*",
    "forgeconfigapiport": "*"
  }
}
```

- [ ] **Step 8: Write `fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`**

```java
package com.niklas2233.talismanofrepair;

import net.fabricmc.api.ModInitializer;

public class TalismanOfRepair implements ModInitializer {
    public static final String MOD_ID = "talismanofrepair";

    @Override
    public void onInitialize() {
        // Item registration added in Task 2.
    }
}
```

- [ ] **Step 9: Verify the empty project builds**

Run: `cd ~/talismanofrepair/fabric && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL` (first run downloads Minecraft/mappings/dependencies — can take several minutes).

- [ ] **Step 10: Commit**

```bash
cd ~/talismanofrepair
git add fabric/
git commit -m "Scaffold Fabric mod project"
```

---

### Task 2: Fabric item, texture, recipe, registration

**Files:**
- Create: `tools/gen_icon.py`
- Create: `fabric/src/main/resources/assets/talismanofrepair/textures/item/talisman_of_repair.png` (generated)
- Create: `fabric/src/main/resources/assets/talismanofrepair/models/item/talisman_of_repair.json`
- Create: `fabric/src/main/resources/assets/talismanofrepair/lang/en_us.json`
- Create: `fabric/src/main/resources/data/talismanofrepair/recipe/talisman_of_repair.json`
- Modify: `fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`

**Interfaces:**
- Consumes: `TalismanOfRepair.MOD_ID` (Task 1).
- Produces: `TalismanOfRepair.TALISMAN_OF_REPAIR` (a plain `net.minecraft.world.item.Item` instance registered in `BuiltInRegistries.ITEM`), used by Task 3 (repair logic gets attached directly to this item's class) and Task 5 (Accessories/ModMenu reference it indirectly via config only, no direct dependency).

- [ ] **Step 1: Write the placeholder icon generator (pure stdlib, no PIL)**

```python
# tools/gen_icon.py
import struct
import sys
import zlib

def make_png(path):
    size = 16
    cx = cy = size / 2 - 0.5
    purple = (147, 74, 214, 255)
    outline = (86, 38, 138, 255)
    highlight = (200, 150, 240, 255)
    pixels = []
    for y in range(size):
        row = bytearray([0])  # filter byte: none
        for x in range(size):
            dx, dy = abs(x - cx), abs(y - cy)
            dist = (dx / (size * 0.42)) + (dy / (size * 0.42))
            if dist <= 0.85:
                color = highlight if (x < cx and y < cy and dist < 0.5) else purple
            elif dist <= 1.0:
                color = outline
            else:
                color = (0, 0, 0, 0)
            row.extend(color)
        pixels.append(bytes(row))
    raw = b"".join(pixels)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data +
                struct.pack(">I", zlib.crc32(tag + data)))

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    idat = zlib.compress(raw, 9)
    with open(path, "wb") as f:
        f.write(sig)
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", idat))
        f.write(chunk(b"IEND", b""))

if __name__ == "__main__":
    make_png(sys.argv[1])
```

- [ ] **Step 2: Generate the Fabric texture**

```bash
mkdir -p ~/talismanofrepair/fabric/src/main/resources/assets/talismanofrepair/textures/item
python3 ~/talismanofrepair/tools/gen_icon.py ~/talismanofrepair/fabric/src/main/resources/assets/talismanofrepair/textures/item/talisman_of_repair.png
```

- [ ] **Step 3: Write the item model**

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "talismanofrepair:item/talisman_of_repair"
  }
}
```
Path: `fabric/src/main/resources/assets/talismanofrepair/models/item/talisman_of_repair.json`

- [ ] **Step 4: Write the lang file**

```json
{
  "item.talismanofrepair.talisman_of_repair": "Talisman of Repair"
}
```
Path: `fabric/src/main/resources/assets/talismanofrepair/lang/en_us.json`

- [ ] **Step 5: Write the recipe**

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "DPD",
    "PEP",
    "DPD"
  ],
  "key": {
    "D": { "item": "minecraft:diamond" },
    "P": { "item": "minecraft:ender_pearl" },
    "E": { "item": "minecraft:emerald" }
  },
  "result": {
    "id": "talismanofrepair:talisman_of_repair",
    "count": 1
  }
}
```
Path: `fabric/src/main/resources/data/talismanofrepair/recipe/talisman_of_repair.json`

- [ ] **Step 6: Register the item in `TalismanOfRepair.java`**

```java
package com.niklas2233.talismanofrepair;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class TalismanOfRepair implements ModInitializer {
    public static final String MOD_ID = "talismanofrepair";

    public static final TalismanOfRepairItem TALISMAN_OF_REPAIR = register(
            "talisman_of_repair",
            new TalismanOfRepairItem(new Item.Properties().stacksTo(1)));

    private static <T extends Item> T register(String name, T item) {
        return net.minecraft.core.Registry.register(
                BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name), item);
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.add((ItemLike) TALISMAN_OF_REPAIR));
    }
}
```

- [ ] **Step 7: Add a placeholder `TalismanOfRepairItem` class (full repair logic comes in Task 3)**

```java
package com.niklas2233.talismanofrepair;

import net.minecraft.world.item.Item;

public class TalismanOfRepairItem extends Item {
    public TalismanOfRepairItem(Properties properties) {
        super(properties);
    }
}
```
Path: `fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairItem.java`

- [ ] **Step 8: Build and verify**

Run: `cd ~/talismanofrepair/fabric && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
cd ~/talismanofrepair
git add tools/ fabric/
git commit -m "Add Fabric talisman item, texture, recipe, and registration"
```

---

### Task 3: Fabric repair logic

**Files:**
- Modify: `fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairItem.java`
- Create: `fabric/src/main/java/com/niklas2233/talismanofrepair/AccessoriesCompat.java`
- Create: `fabric/src/main/java/com/niklas2233/talismanofrepair/AccessoriesRepair.java`

**Interfaces:**
- Consumes: `RepairConfig.cooldownTicks()` / `RepairConfig.xpLevelCost()` (produced in Task 4 — write `TalismanOfRepairItem` now, `RepairConfig` compiles it in Task 4; this task's build step will therefore fail until Task 4 lands, so do Tasks 3 and 4 back-to-back before the build-verification step).
- Produces: `TalismanOfRepairItem#inventoryTick` fully implements the cooldown/XP-gated repair scan described in the spec. `AccessoriesCompat.repairEquipped(Player, BiConsumer<ItemStack, Player>)` is the seam Task 5's Accessories dependency plugs into — safe to call whether or not Accessories is installed.

- [ ] **Step 1: Write `TalismanOfRepairItem.java`**

```java
package com.niklas2233.talismanofrepair;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class TalismanOfRepairItem extends Item {
    public TalismanOfRepairItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!checkCooldown(player, stack)) {
            return;
        }
        if (!chargeXp(player)) {
            return;
        }
        repairAll(player);
    }

    private boolean checkCooldown(Player player, ItemStack stack) {
        int cooldownTicks = RepairConfig.cooldownTicks();
        if (cooldownTicks == -1) {
            return false;
        }
        ItemCooldowns cooldowns = player.getCooldowns();
        if (cooldowns.isOnCooldown(stack)) {
            return false;
        }
        if (cooldownTicks > 0) {
            cooldowns.addCooldown(stack, cooldownTicks);
        }
        return true;
    }

    private boolean chargeXp(Player player) {
        int cost = RepairConfig.xpLevelCost();
        if (cost <= 0) {
            return true;
        }
        if (player.experienceLevel < cost) {
            return false;
        }
        player.giveExperienceLevels(-cost);
        return true;
    }

    private void repairAll(Player player) {
        BiConsumer<ItemStack, Player> repair = TalismanOfRepairItem::tryRepair;
        for (ItemStack invStack : player.getInventory().items) {
            repair.accept(invStack, player);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            repair.accept(player.getItemBySlot(slot), player);
        }
        AccessoriesCompat.repairEquipped(player, repair);
    }

    private static void tryRepair(ItemStack stack, Player player) {
        if (stack.isEmpty() || !stack.isDamaged() || stack.has(DataComponents.UNBREAKABLE)) {
            return;
        }
        if (stack == player.getMainHandItem() && player.swinging) {
            return;
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
    }
}
```

- [ ] **Step 2: Write `AccessoriesCompat.java` (the isolation seam)**

```java
package com.niklas2233.talismanofrepair;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

public final class AccessoriesCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("accessories");

    private AccessoriesCompat() {
    }

    public static void repairEquipped(Player player, BiConsumer<ItemStack, Player> repair) {
        if (LOADED) {
            AccessoriesRepair.repairEquipped(player, repair);
        }
    }
}
```

- [ ] **Step 3: Write `AccessoriesRepair.java` (only ever touched behind the presence check above)**

```java
package com.niklas2233.talismanofrepair;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

final class AccessoriesRepair {
    private AccessoriesRepair() {
    }

    static void repairEquipped(Player player, BiConsumer<ItemStack, Player> repair) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return;
        }
        capability.getAllEquipped().forEach(ref -> repair.accept(ref.stack(), player));
    }
}
```

- [ ] **Step 4: Commit (build verification deferred to end of Task 4, since `RepairConfig` doesn't exist yet)**

```bash
cd ~/talismanofrepair
git add fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairItem.java
git add fabric/src/main/java/com/niklas2233/talismanofrepair/AccessoriesCompat.java
git add fabric/src/main/java/com/niklas2233/talismanofrepair/AccessoriesRepair.java
git commit -m "Add Fabric repair logic and Accessories compat seam"
```

---

### Task 4: Fabric config (ModConfigSpec via Forge Config API Port)

**Files:**
- Create: `fabric/src/main/java/com/niklas2233/talismanofrepair/RepairConfig.java`
- Modify: `fabric/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`

**Interfaces:**
- Produces: `RepairConfig.cooldownTicks()`, `RepairConfig.xpLevelCost()`, `RepairConfig.SPEC` (a `net.neoforged.neoforge.common.ModConfigSpec`) — consumed by `TalismanOfRepairItem` (Task 3) and by `ClothConfigScreen` (Task 5).

- [ ] **Step 1: Write `RepairConfig.java`**

```java
package com.niklas2233.talismanofrepair;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RepairConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    private static final ModConfigSpec.IntValue XP_LEVEL_COST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COOLDOWN_TICKS = builder
                .comment("Ticks between repair passes. -1 disables the talisman entirely.")
                .defineInRange("cooldownTicks", 20, -1, Integer.MAX_VALUE);
        XP_LEVEL_COST = builder
                .comment("XP levels charged per repair pass. 0 makes repairs free.")
                .defineInRange("xpLevelCost", 1, 0, Integer.MAX_VALUE);
        SPEC = builder.build();
    }

    private RepairConfig() {
    }

    public static int cooldownTicks() {
        return COOLDOWN_TICKS.get();
    }

    public static int xpLevelCost() {
        return XP_LEVEL_COST.get();
    }
}
```

- [ ] **Step 2: Register the config in `TalismanOfRepair.onInitialize()`**

```java
    @Override
    public void onInitialize() {
        net.neoforged.fml.config.ConfigTracker.INSTANCE.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON, RepairConfig.SPEC, MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.add((ItemLike) TALISMAN_OF_REPAIR));
    }
```
(Replace the whole method body with the above — the `ItemGroupEvents` line already exists from Task 2, just add the `ConfigTracker` line above it.)

- [ ] **Step 3: Build and verify**

Run: `cd ~/talismanofrepair/fabric && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`. This also validates Task 3's code, since `RepairConfig` now exists.

- [ ] **Step 4: Commit**

```bash
cd ~/talismanofrepair
git add fabric/src/main/java/com/niklas2233/talismanofrepair/
git commit -m "Add Fabric config via Forge Config API Port"
```

---

### Task 5: Fabric ModMenu + Cloth Config screen

**Files:**
- Create: `fabric/src/client/java/com/niklas2233/talismanofrepair/client/ModMenuIntegration.java`
- Create: `fabric/src/client/java/com/niklas2233/talismanofrepair/client/ClothConfigScreen.java`

**Interfaces:**
- Consumes: `RepairConfig.cooldownTicks()/xpLevelCost()` and `RepairConfig.SPEC` (Task 4).
- Produces: nothing consumed by later tasks — this is a leaf integration.

- [ ] **Step 1: Write `ClothConfigScreen.java`**

```java
package com.niklas2233.talismanofrepair.client;

import com.niklas2233.talismanofrepair.RepairConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ClothConfigScreen {
    static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Talisman of Repair"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(entryBuilder
                .startIntField(Component.literal("Cooldown Ticks"), RepairConfig.cooldownTicks())
                .setDefaultValue(20)
                .setMin(-1)
                .setSaveConsumer(RepairConfig::setCooldownTicks)
                .build());

        general.addEntry(entryBuilder
                .startIntField(Component.literal("XP Level Cost"), RepairConfig.xpLevelCost())
                .setDefaultValue(1)
                .setMin(0)
                .setSaveConsumer(RepairConfig::setXpLevelCost)
                .build());

        builder.setSavingRunnable(() -> RepairConfig.SPEC.save());
        return builder.build();
    }
}
```

- [ ] **Step 2: Add setter methods to `RepairConfig.java` (needed by the screen's save consumers)**

Add these two methods to the `RepairConfig` class from Task 4:

```java
    public static void setCooldownTicks(int value) {
        COOLDOWN_TICKS.set(value);
    }

    public static void setXpLevelCost(int value) {
        XP_LEVEL_COST.set(value);
    }
```

- [ ] **Step 3: Write `ModMenuIntegration.java`**

```java
package com.niklas2233.talismanofrepair.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    private static final boolean CLOTH_PRESENT = isPresent("me.shedaniel.clothconfig2.api.ConfigBuilder");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!CLOTH_PRESENT) {
            return parent -> null;
        }
        return ClothConfigScreen::build;
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `cd ~/talismanofrepair/fabric && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
cd ~/talismanofrepair
git add fabric/src/client/ fabric/src/main/java/com/niklas2233/talismanofrepair/RepairConfig.java
git commit -m "Add Fabric ModMenu and Cloth Config screen"
```

This completes the Fabric mod.

---

### Task 6: NeoForge project scaffold

**Files:**
- Create: `neoforge/settings.gradle`
- Create: `neoforge/build.gradle`
- Create: `neoforge/gradle.properties`
- Create: `neoforge/gradle/wrapper/gradle-wrapper.properties` (+ jar, via real `gradle wrapper` invocation)
- Create: `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `neoforge/src/main/resources/pack.mcmeta`
- Create: `neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`

**Interfaces:**
- Produces: `TalismanOfRepair` class (`@Mod("talismanofrepair")`), `MOD_ID` constant, `ITEMS` `DeferredRegister.Items` — used by Task 7.

- [ ] **Step 1: Download a temporary Gradle 8.14.5 distribution to bootstrap the wrapper**

```bash
cd /tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad
curl -sL -o gradle-8.14.5-bin.zip https://services.gradle.org/distributions/gradle-8.14.5-bin.zip
unzip -q gradle-8.14.5-bin.zip
```

- [ ] **Step 2: Create the NeoForge project directory and generate the wrapper**

```bash
mkdir -p ~/talismanofrepair/neoforge
cd ~/talismanofrepair/neoforge
/tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad/gradle-8.14.5/bin/gradle wrapper --gradle-version 8.14.5
```

- [ ] **Step 3: Clean up the temporary Gradle distribution**

```bash
rm -rf /tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad/gradle-8.14.5 /tmp/claude-1000/-home-niklas-claude/00e2ff2f-f53c-45f1-84fb-b399be547418/scratchpad/gradle-8.14.5-bin.zip
```

- [ ] **Step 4: Write `neoforge/settings.gradle`**

```groovy
pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven {
			name = "NeoForged"
			url = 'https://maven.neoforged.net/releases'
			content {
				includeGroup "net.neoforged"
			}
		}
	}
}

plugins {
	id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}

rootProject.name = 'talismanofrepair'
```

- [ ] **Step 5: Write `neoforge/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2G

mod_version=1.0.0
maven_group=com.niklas2233

minecraft_version=1.21.10
neoforge_version=21.10.64

accessories_version=1.4.3-beta
```

- [ ] **Step 6: Write `neoforge/build.gradle`**

```groovy
plugins {
	id 'net.neoforged.moddev' version '2.0.142'
}

version = project.mod_version
group = project.maven_group

repositories {
	maven { url = 'https://maven.wispforest.io/releases' }
	maven { url = 'https://maven.su5ed.dev/releases' }
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

neoForge {
	version = project.neoforge_version

	runs {
		client {
			client()
		}
		server {
			server()
		}
	}

	mods {
		talismanofrepair {
			sourceSet sourceSets.main
		}
	}
}

dependencies {
	compileOnly "io.wispforest:accessories-neoforge:${project.accessories_version}"
}

processResources {
	def version = project.version
	inputs.property "version", version
	filesMatching("META-INF/neoforge.mods.toml") {
		expand "version": version
	}
}
```

- [ ] **Step 7: Write `neoforge/src/main/resources/pack.mcmeta`**

```json
{
  "pack": {
    "description": "Talisman of Repair resources",
    "pack_format": 88
  }
}
```

- [ ] **Step 8: Write `neoforge/src/main/resources/META-INF/neoforge.mods.toml`**

```toml
modLoader = "javafml"
loaderVersion = "[1,)"
license = "MIT"

[[mods]]
modId = "talismanofrepair"
version = "${version}"
displayName = "Talisman of Repair"
authors = "niklas2233"
description = "Adds an item that automatically repairs your gear over time."

[[dependencies.talismanofrepair]]
modId = "neoforge"
mandatory = true
type = "required"
versionRange = "[21.10.0,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.talismanofrepair]]
modId = "minecraft"
mandatory = true
type = "required"
versionRange = "[1.21.10,1.21.11)"
ordering = "NONE"
side = "BOTH"
```

- [ ] **Step 9: Write `neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`**

```java
package com.niklas2233.talismanofrepair;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TalismanOfRepair.MOD_ID)
public class TalismanOfRepair {
    public static final String MOD_ID = "talismanofrepair";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public TalismanOfRepair(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, RepairConfig.SPEC);
    }
}
```

- [ ] **Step 10: Verify the empty project builds**

Run: `cd ~/talismanofrepair/neoforge && ./gradlew build --console=plain`
Expected: initially FAILS (references `RepairConfig`, which doesn't exist yet — that's expected and fixed in Task 8). Confirm the failure is specifically "cannot find symbol RepairConfig", not a Gradle/plugin/dependency-resolution error. If it's a resolution error, fix the repository/dependency setup before continuing.

- [ ] **Step 11: Commit**

```bash
cd ~/talismanofrepair
git add neoforge/
git commit -m "Scaffold NeoForge mod project"
```

---

### Task 7: NeoForge item, assets, recipe, registration

**Files:**
- Create: `neoforge/src/main/resources/assets/talismanofrepair/textures/item/talisman_of_repair.png` (copied from Fabric)
- Create: `neoforge/src/main/resources/assets/talismanofrepair/models/item/talisman_of_repair.json`
- Create: `neoforge/src/main/resources/assets/talismanofrepair/lang/en_us.json`
- Create: `neoforge/src/main/resources/data/talismanofrepair/recipe/talisman_of_repair.json`
- Modify: `neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepair.java`
- Create: `neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairItem.java`

**Interfaces:**
- Consumes: `TalismanOfRepair.MOD_ID`, `TalismanOfRepair.ITEMS` (Task 6).
- Produces: `TalismanOfRepair.TALISMAN_OF_REPAIR` (`DeferredItem<TalismanOfRepairItem>`), used by Task 8 (repair logic lives directly on `TalismanOfRepairItem`, no further cross-task interface needed) and Task 9 (creative tab event, same class).

- [ ] **Step 1: Copy the texture from the Fabric project**

```bash
mkdir -p ~/talismanofrepair/neoforge/src/main/resources/assets/talismanofrepair/textures/item
cp ~/talismanofrepair/fabric/src/main/resources/assets/talismanofrepair/textures/item/talisman_of_repair.png \
   ~/talismanofrepair/neoforge/src/main/resources/assets/talismanofrepair/textures/item/talisman_of_repair.png
```

- [ ] **Step 2: Write the item model (identical to Fabric's)**

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "talismanofrepair:item/talisman_of_repair"
  }
}
```
Path: `neoforge/src/main/resources/assets/talismanofrepair/models/item/talisman_of_repair.json`

- [ ] **Step 3: Write the lang file (identical to Fabric's)**

```json
{
  "item.talismanofrepair.talisman_of_repair": "Talisman of Repair"
}
```
Path: `neoforge/src/main/resources/assets/talismanofrepair/lang/en_us.json`

- [ ] **Step 4: Write the recipe (identical to Fabric's)**

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "DPD",
    "PEP",
    "DPD"
  ],
  "key": {
    "D": { "item": "minecraft:diamond" },
    "P": { "item": "minecraft:ender_pearl" },
    "E": { "item": "minecraft:emerald" }
  },
  "result": {
    "id": "talismanofrepair:talisman_of_repair",
    "count": 1
  }
}
```
Path: `neoforge/src/main/resources/data/talismanofrepair/recipe/talisman_of_repair.json`

- [ ] **Step 5: Write a placeholder `TalismanOfRepairItem.java` (full repair logic comes in Task 8)**

```java
package com.niklas2233.talismanofrepair;

import net.minecraft.world.item.Item;

public class TalismanOfRepairItem extends Item {
    public TalismanOfRepairItem(Properties properties) {
        super(properties);
    }
}
```

- [ ] **Step 6: Register the item and creative tab entry in `TalismanOfRepair.java`**

```java
package com.niklas2233.talismanofrepair;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TalismanOfRepair.MOD_ID)
public class TalismanOfRepair {
    public static final String MOD_ID = "talismanofrepair";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<TalismanOfRepairItem> TALISMAN_OF_REPAIR =
            ITEMS.registerItem("talisman_of_repair", props -> new TalismanOfRepairItem(props.stacksTo(1)));

    public TalismanOfRepair(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, RepairConfig.SPEC);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(TALISMAN_OF_REPAIR);
        }
    }
}
```

- [ ] **Step 7: Build and verify**

Run: `cd ~/talismanofrepair/neoforge && ./gradlew build --console=plain`
Expected: still FAILS on `RepairConfig` (Task 8 fixes this) — confirm the failure is only that missing symbol.

- [ ] **Step 8: Commit**

```bash
cd ~/talismanofrepair
git add neoforge/
git commit -m "Add NeoForge talisman item, assets, recipe, and registration"
```

---

### Task 8: NeoForge repair logic and config

**Files:**
- Modify: `neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairItem.java`
- Create: `neoforge/src/main/java/com/niklas2233/talismanofrepair/RepairConfig.java`
- Create: `neoforge/src/main/java/com/niklas2233/talismanofrepair/AccessoriesCompat.java`
- Create: `neoforge/src/main/java/com/niklas2233/talismanofrepair/AccessoriesRepair.java`

**Interfaces:**
- Produces: identical public surface to the Fabric equivalents from Tasks 3–4 (`RepairConfig.cooldownTicks()/xpLevelCost()/SPEC`, `AccessoriesCompat.repairEquipped(...)`), same logic, native NeoForge registration instead of Forge Config API Port / FabricLoader presence checks.

- [ ] **Step 1: Write `TalismanOfRepairItem.java` (identical logic to the Fabric version from Task 3)**

```java
package com.niklas2233.talismanofrepair;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class TalismanOfRepairItem extends Item {
    public TalismanOfRepairItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!checkCooldown(player, stack)) {
            return;
        }
        if (!chargeXp(player)) {
            return;
        }
        repairAll(player);
    }

    private boolean checkCooldown(Player player, ItemStack stack) {
        int cooldownTicks = RepairConfig.cooldownTicks();
        if (cooldownTicks == -1) {
            return false;
        }
        ItemCooldowns cooldowns = player.getCooldowns();
        if (cooldowns.isOnCooldown(stack)) {
            return false;
        }
        if (cooldownTicks > 0) {
            cooldowns.addCooldown(stack, cooldownTicks);
        }
        return true;
    }

    private boolean chargeXp(Player player) {
        int cost = RepairConfig.xpLevelCost();
        if (cost <= 0) {
            return true;
        }
        if (player.experienceLevel < cost) {
            return false;
        }
        player.giveExperienceLevels(-cost);
        return true;
    }

    private void repairAll(Player player) {
        BiConsumer<ItemStack, Player> repair = TalismanOfRepairItem::tryRepair;
        for (ItemStack invStack : player.getInventory().items) {
            repair.accept(invStack, player);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            repair.accept(player.getItemBySlot(slot), player);
        }
        AccessoriesCompat.repairEquipped(player, repair);
    }

    private static void tryRepair(ItemStack stack, Player player) {
        if (stack.isEmpty() || !stack.isDamaged() || stack.has(DataComponents.UNBREAKABLE)) {
            return;
        }
        if (stack == player.getMainHandItem() && player.swinging) {
            return;
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
    }
}
```

- [ ] **Step 2: Write `RepairConfig.java` (native NeoForge, no FCAP needed)**

```java
package com.niklas2233.talismanofrepair;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RepairConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    private static final ModConfigSpec.IntValue XP_LEVEL_COST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COOLDOWN_TICKS = builder
                .comment("Ticks between repair passes. -1 disables the talisman entirely.")
                .defineInRange("cooldownTicks", 20, -1, Integer.MAX_VALUE);
        XP_LEVEL_COST = builder
                .comment("XP levels charged per repair pass. 0 makes repairs free.")
                .defineInRange("xpLevelCost", 1, 0, Integer.MAX_VALUE);
        SPEC = builder.build();
    }

    private RepairConfig() {
    }

    public static int cooldownTicks() {
        return COOLDOWN_TICKS.get();
    }

    public static int xpLevelCost() {
        return XP_LEVEL_COST.get();
    }
}
```

- [ ] **Step 3: Write `AccessoriesCompat.java`**

```java
package com.niklas2233.talismanofrepair;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.function.BiConsumer;

public final class AccessoriesCompat {
    private static final boolean LOADED = ModList.get().isLoaded("accessories");

    private AccessoriesCompat() {
    }

    public static void repairEquipped(Player player, BiConsumer<ItemStack, Player> repair) {
        if (LOADED) {
            AccessoriesRepair.repairEquipped(player, repair);
        }
    }
}
```

- [ ] **Step 4: Write `AccessoriesRepair.java` (identical to Fabric's)**

```java
package com.niklas2233.talismanofrepair;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

final class AccessoriesRepair {
    private AccessoriesRepair() {
    }

    static void repairEquipped(Player player, BiConsumer<ItemStack, Player> repair) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return;
        }
        capability.getAllEquipped().forEach(ref -> repair.accept(ref.stack(), player));
    }
}
```

- [ ] **Step 5: Build and verify**

Run: `cd ~/talismanofrepair/neoforge && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
cd ~/talismanofrepair
git add neoforge/src/main/java/com/niklas2233/talismanofrepair/
git commit -m "Add NeoForge repair logic, config, and Accessories compat"
```

---

### Task 9: NeoForge config screen

**Files:**
- Create: `neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairClient.java`

**Interfaces:**
- Consumes: `RepairConfig.SPEC` (Task 8).
- Produces: nothing consumed elsewhere — leaf task.

- [ ] **Step 1: Write the client-side config screen registration**

NeoForge's `ConfigurationScreen` isn't wired up automatically — it must be registered as an extension point on the client-only mod class.

```java
package com.niklas2233.talismanofrepair;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TalismanOfRepair.MOD_ID, dist = Dist.CLIENT)
public class TalismanOfRepairClient {
    public TalismanOfRepairClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `cd ~/talismanofrepair/neoforge && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
cd ~/talismanofrepair
git add neoforge/src/main/java/com/niklas2233/talismanofrepair/TalismanOfRepairClient.java
git commit -m "Add NeoForge config screen registration"
```

This completes the NeoForge mod.

---

### Task 10: Final verification and README

**Files:**
- Create: `README.md`

**Interfaces:**
- None — this task only verifies prior tasks and adds top-level documentation.

- [ ] **Step 1: Full clean build of both projects**

```bash
cd ~/talismanofrepair/fabric && ./gradlew clean build --console=plain
cd ~/talismanofrepair/neoforge && ./gradlew clean build --console=plain
```
Expected: `BUILD SUCCESSFUL` for both.

- [ ] **Step 2: Confirm both mod jars were produced**

```bash
find ~/talismanofrepair/fabric/build/libs ~/talismanofrepair/neoforge/build/libs -name "*.jar"
```
Expected: at least one non-`-sources`, non-`-dev` jar per project.

- [ ] **Step 3: Write `README.md`**

```markdown
# Talisman of Repair

An item that automatically repairs your gear over time, for Minecraft 1.21.10.

- `fabric/` — Fabric + Fabric API build
- `neoforge/` — NeoForge build

Both are independent Gradle projects; build each with its own `./gradlew build`.

## Behavior

- Repairs 1 durability point every `cooldownTicks` (default 20, `-1` disables) across your
  main inventory, all equipped slots, and equipped Accessories (if installed).
- Costs `xpLevelCost` XP levels per repair pass (default 1, `0` = free).
- Configurable in-game: ModMenu + Cloth Config on Fabric, the native "Config" screen on NeoForge.

See `docs/superpowers/specs/2026-07-24-repair-talisman-design.md` for the full design and
`docs/superpowers/plans/2026-07-24-repair-talisman-implementation.md` for the build plan.
```

- [ ] **Step 4: Commit and push**

```bash
cd ~/talismanofrepair
git add README.md
git commit -m "Add project README"
git push
```

- [ ] **Step 5: Manual smoke test reminder**

The spec's Testing section (items 1–12) requires a running game client and cannot be automated here — run through it manually once both jars are in a real Minecraft instance.
