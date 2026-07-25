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
