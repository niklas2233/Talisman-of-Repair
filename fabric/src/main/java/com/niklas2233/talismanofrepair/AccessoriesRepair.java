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
