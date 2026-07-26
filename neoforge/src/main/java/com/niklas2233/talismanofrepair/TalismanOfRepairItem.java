package com.niklas2233.talismanofrepair;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class TalismanOfRepairItem extends Item {
    // ponytail: internal throttle, not a real item cooldown — avoids vanilla's white cooldown-overlay bar
    private final Map<UUID, Long> nextRepairTick = new HashMap<>();

    public TalismanOfRepairItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isHeld) {
        super.inventoryTick(stack, level, entity, slot, isHeld);
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        runRepairPass(player);
    }

    void runRepairPass(Player player) {
        if (!checkCooldown(player)) {
            return;
        }
        if (!chargeXp(player)) {
            return;
        }
        repairAll(player);
    }

    private boolean checkCooldown(Player player) {
        int cooldownTicks = RepairConfig.cooldownTicks();
        if (cooldownTicks == -1) {
            return false;
        }
        if (cooldownTicks == 0) {
            return true;
        }
        long now = player.level().getGameTime();
        Long next = nextRepairTick.get(player.getUUID());
        if (next != null && now < next) {
            return false;
        }
        nextRepairTick.put(player.getUUID(), now + cooldownTicks);
        return true;
    }

    private boolean chargeXp(Player player) {
        int cost = RepairConfig.xpCost();
        if (cost <= 0) {
            return true;
        }
        if (player.totalExperience < cost) {
            return false;
        }
        player.giveExperiencePoints(-cost);
        return true;
    }

    private void repairAll(Player player) {
        BiConsumer<ItemStack, Player> repair = TalismanOfRepairItem::tryRepair;
        for (ItemStack invStack : player.getInventory().items) {
            repair.accept(invStack, player);
        }
        for (ItemStack armorStack : player.getInventory().armor) {
            repair.accept(armorStack, player);
        }
        for (ItemStack offhandStack : player.getInventory().offhand) {
            repair.accept(offhandStack, player);
        }
        AccessoriesCompat.repairEquipped(player, repair);
    }

    private static void tryRepair(ItemStack stack, Player player) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0
                || stack.has(DataComponents.UNBREAKABLE)) {
            return;
        }
        if (stack == player.getMainHandItem() && player.swinging) {
            return;
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
    }
}
