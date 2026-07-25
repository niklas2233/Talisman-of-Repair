package com.niklas2233.talismanofrepair.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    private static final boolean CLOTH_PRESENT = isPresent("me.shedaniel.clothconfig2.api.ConfigBuilder");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!CLOTH_PRESENT) {
            return parent -> null;
        }
        return ClothConfigScreen::build;
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
