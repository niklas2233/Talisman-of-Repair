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
                .startIntField(Component.literal("XP Cost"), RepairConfig.xpCost())
                .setDefaultValue(1)
                .setMin(0)
                .setSaveConsumer(RepairConfig::setXpCost)
                .build());

        builder.setSavingRunnable(() -> RepairConfig.SPEC.save());
        return builder.build();
    }
}
