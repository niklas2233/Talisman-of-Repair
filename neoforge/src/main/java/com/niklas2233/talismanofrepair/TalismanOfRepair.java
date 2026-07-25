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
