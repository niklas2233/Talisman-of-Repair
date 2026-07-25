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
