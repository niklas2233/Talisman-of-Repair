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

    public static void registerAccessoryTick() {
        if (LOADED) {
            AccessoriesRepair.registerTicking();
        }
    }
}
