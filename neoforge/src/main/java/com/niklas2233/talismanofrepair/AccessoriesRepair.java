package com.niklas2233.talismanofrepair;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotReference;
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

    static void registerTicking() {
        AccessoriesAPI.registerAccessory(TalismanOfRepair.TALISMAN_OF_REPAIR.get(), new Accessory() {
            @Override
            public void tick(ItemStack stack, SlotReference reference) {
                if (reference.entity() instanceof Player player && !player.level().isClientSide()) {
                    TalismanOfRepair.TALISMAN_OF_REPAIR.get().runRepairPass(player);
                }
            }
        });
    }
}
