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

    public static void registerAccessoryTick() {
        if (LOADED) {
            AccessoriesRepair.registerTicking();
        }
    }
}
