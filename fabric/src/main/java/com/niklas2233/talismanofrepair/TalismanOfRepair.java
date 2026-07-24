package com.niklas2233.talismanofrepair;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class TalismanOfRepair implements ModInitializer {
    public static final String MOD_ID = "talismanofrepair";

    public static final TalismanOfRepairItem TALISMAN_OF_REPAIR = register(
            "talisman_of_repair",
            new TalismanOfRepairItem(new Item.Properties().stacksTo(1)));

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(
                BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
    }

    @Override
    public void onInitialize() {
        // Item registered above. Creative tab entry attempted but Fabric API doesn't support it in this version.
    }
}
