package com.infloat.legacymousetweaks.client;

import org.lwjgl.input.Keyboard;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.options.KeyBinding;
import net.ornithemc.osl.keybinds.api.KeybindEvents;
import net.ornithemc.osl.keybinds.api.KeybindRegistry;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

public class LegacyMouseTweaksClient implements ClientModInitializer {
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = new KeyBinding(
                "key.legacymousetweaks.open_config",
                Keyboard.KEY_NONE,
                "key.categories.legacymousetweaks");

        KeybindEvents.REGISTER_KEYBINDS.register(() -> KeybindRegistry.register(openConfigKey));

        MinecraftClientEvents.TICK_END.register(client -> {
            if (openConfigKey.isPressed()) {
                client.openScreen(new MouseTweaksConfigScreen(client.screen));
            }
        });
    }
}
