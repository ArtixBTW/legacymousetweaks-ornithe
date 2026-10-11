package com.infloat.legacymousetweaks.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class LegacyMouseTweaksModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MouseTweaksConfigScreen((net.minecraft.client.gui.screen.Screen) parent);
    }
}
