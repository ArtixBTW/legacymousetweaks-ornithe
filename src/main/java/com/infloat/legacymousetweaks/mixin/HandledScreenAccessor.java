package com.infloat.legacymousetweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screen.game.inventory.InventoryMenuScreen;
import net.minecraft.inventory.menu.InventoryMenu;
import net.minecraft.inventory.slot.InventorySlot;

@Mixin(InventoryMenuScreen.class)
public interface HandledScreenAccessor {
    @Invoker("getSlot")
    InventorySlot lmt_getSlot(int mouseX, int mouseY);

    @Invoker("clickSlot")
    void lmt_clickSlot(InventorySlot slot, int slotId, int button, int mode);

    @Accessor("menu")
    InventoryMenu lmt_getMenu();
}
