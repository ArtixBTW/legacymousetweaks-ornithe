package com.infloat.legacymousetweaks.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.infloat.legacymousetweaks.config.ConfigManager;
import com.infloat.legacymousetweaks.config.MouseTweaksConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.game.inventory.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.game.inventory.InventoryMenuScreen;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.inventory.menu.InventoryMenu;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.inventory.slot.InventorySlot;
import net.minecraft.item.ItemStack;

@Mixin(InventoryMenuScreen.class)
public abstract class GuiContainerMixin {
    @Unique
    private InventorySlot lmt_oldSelectedInventorySlot;

    @Unique
    private boolean lmt_canDoLMBDrag;

    @Unique
    private boolean lmt_canDoRMBDrag;

    @Shadow
    protected boolean isDraggingItem;

    @Shadow
    protected Set<InventorySlot> draggedInvSlots;

    @Invoker("getSlot")
    protected abstract InventorySlot lmt_getSlot(int mouseX, int mouseY);

    @Invoker("clickSlot")
    protected abstract void lmt_clickSlot(InventorySlot slot, int slotId, int button, int mode);

    @Shadow
    protected InventoryMenu menu;

    @Unique
    private static boolean lmt_isStackEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.size <= 0;
    }

    @Unique
    private static MouseTweaksConfig lmt_config() {
        return ConfigManager.get();
    }

    @Unique
    private static boolean lmt_areStacksCompatible(ItemStack a, ItemStack b) {
        if (lmt_isStackEmpty(a) || lmt_isStackEmpty(b)) {
            return true;
        }
        if (!ItemStack.matchesItem(a, b)) {
            return false;
        }

        return !a.hasCustomData() || a.getMetadata() == b.getMetadata();
    }

    @Unique
    private static boolean lmt_isCraftingOutput(InventorySlot slot) {
        return slot instanceof CraftingResultSlot;
    }

    @Unique
    private void lmt_rmbTweakMaybeClickInventorySlot(InventorySlot slot, ItemStack stackOnMouse) {
        if (slot == null) {
            return;
        }
        if (lmt_isStackEmpty(stackOnMouse)) {
            return;
        }
        if (lmt_isCraftingOutput(slot)) {
            return;
        }

        ItemStack selectedInventorySlotStack = slot.getItem();
        if (!lmt_areStacksCompatible(selectedInventorySlotStack, stackOnMouse)) {
            return;
        }

        if (!lmt_isStackEmpty(selectedInventorySlotStack)
                && selectedInventorySlotStack.size >= slot.getMaxStackSize(selectedInventorySlotStack)) {
            return;
        }

        this.lmt_clickSlot(slot, slot.index, 1, 0);
    }

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void lmt_onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        InventorySlot selectedSlot = this.lmt_getSlot(mouseX, mouseY);
        this.lmt_oldSelectedInventorySlot = selectedSlot;

        ItemStack stackOnMouse = Minecraft.getInstance().player.inventory.getCursorItem();
        if (mouseButton == 0) {
            this.lmt_canDoLMBDrag = lmt_isStackEmpty(stackOnMouse);
        } else if (mouseButton == 1) {
            this.lmt_canDoRMBDrag = !lmt_isStackEmpty(stackOnMouse) && lmt_config().rmbTweak;
            if (this.lmt_canDoRMBDrag) {
                // Replace vanilla RMB click/drag behavior with MouseTweaks-style behavior.
                this.isDraggingItem = false;
                this.draggedInvSlots.clear();
                this.lmt_rmbTweakMaybeClickInventorySlot(selectedSlot, stackOnMouse);
                ci.cancel();
            }
        }
    }

    @Inject(method = "mouseDragged(IIIJ)V", at = @At("HEAD"), cancellable = true)
    private void lmt_onMouseDragged(int mouseX, int mouseY, int mouseButton, long timeSinceClick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        InventorySlot selectedInventorySlot = this.lmt_getSlot(mouseX, mouseY);
        if (mouseButton == 1 && this.lmt_canDoRMBDrag) {
            if (selectedInventorySlot != this.lmt_oldSelectedInventorySlot) {
                this.lmt_rmbTweakMaybeClickInventorySlot(selectedInventorySlot, mc.player.inventory.getCursorItem());
                this.lmt_oldSelectedInventorySlot = selectedInventorySlot;
            }

            ci.cancel();
            return;
        }

        if (selectedInventorySlot == this.lmt_oldSelectedInventorySlot) {
            return;
        }

        ItemStack stackOnMouse = mc.player.inventory.getCursorItem();

        this.lmt_oldSelectedInventorySlot = selectedInventorySlot;

        if (selectedInventorySlot == null) {
            return;
        }

        if (mouseButton == 0) {
            if (!this.lmt_canDoLMBDrag) {
                return;
            }

            ItemStack selectedInventorySlotStack = selectedInventorySlot.getItem();
                if (lmt_isStackEmpty(selectedInventorySlotStack)) {
                    return;
                }

                boolean shiftIsDown = Screen.isShiftDown();
                if (lmt_isStackEmpty(stackOnMouse)) {
                    if (!lmt_config().lmbTweakWithoutItem || !shiftIsDown) {
                        return;
                    }
                    this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 1);
                } else {
                    if (!lmt_config().lmbTweakWithItem) {
                        return;
                    }
                if (!lmt_areStacksCompatible(selectedInventorySlotStack, stackOnMouse)) {
                    return;
                }

                if (shiftIsDown) {
                    this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 1);
                } else {
                    if (stackOnMouse.size + selectedInventorySlotStack.size > stackOnMouse.getMaxSize()) {
                        return;
                    }
                    this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 0);
                    if (!lmt_isCraftingOutput(selectedInventorySlot)) {
                        this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 0);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseReleased(III)V", at = @At("HEAD"), cancellable = true)
    private void lmt_onMouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        if (state == 0) {
            this.lmt_canDoLMBDrag = false;
        } else if (state == 1) {
            if (this.lmt_canDoRMBDrag) {
                // Prevent vanilla from adding an extra RMB action on release.
                this.isDraggingItem = false;
                this.draggedInvSlots.clear();
                ci.cancel();
            }
            this.lmt_canDoRMBDrag = false;
        }
    }

    @Inject(method = "render(IIF)V", at = @At("HEAD"))
    private void lmt_onRenderWheel(int mouseXParam, int mouseYParam, float deltaTicks, CallbackInfo ci) {
        try {
            if (!lmt_config().wheelTweak) {
                return;
            }
            // In the creative screen the item list has its own scroll and onMouseClick
            // behaves differently — items disappear or scroll fires twice. Disable entirely.
            if ((Object) this instanceof CreativeInventoryScreen) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.width <= 0 || mc.height <= 0) {
                return;
            }

            int wheel = Mouse.getDWheel();
            if (wheel == 0) {
                return;
            }
            if (menu == null || menu.slots == null) {
                return;
            }

            int screenWidth = ((Screen) (Object) this).width;
            int screenHeight = ((Screen) (Object) this).height;
            int mouseX = Mouse.getX() * screenWidth / mc.width;
            int mouseY = screenHeight - Mouse.getY() * screenHeight / mc.height - 1;

            InventorySlot selectedInventorySlot = this.lmt_getSlot(mouseX, mouseY);
            if (selectedInventorySlot == null) {
                return;
            }

            ItemStack selectedInventorySlotStack = selectedInventorySlot.getItem();
            if (lmt_isStackEmpty(selectedInventorySlotStack)) {
                return;
            }

            int delta = wheel / 120;
            if (delta == 0) {
                delta = wheel > 0 ? 1 : -1;
            }

            List<InventorySlot> slots = menu.slots;
            int numItemsToMove = Math.abs(delta);
            boolean pushItems = delta < 0;

            if (lmt_config().wheelScrollDirection == 2 && this.lmt_otherInventoryIsAbove(selectedInventorySlot, slots)) {
                pushItems = !pushItems;
            }
            if (lmt_config().wheelScrollDirection == 1) {
                pushItems = !pushItems;
            }

            if (pushItems) {
                this.lmt_wheelPush(selectedInventorySlot, slots, numItemsToMove);
            } else {
                this.lmt_wheelPull(selectedInventorySlot, slots, numItemsToMove);
            }
        } catch (Throwable t) {
            System.err.println("[LegacyMouseTweaks] Wheel tweak error: " + t);
        }
    }

    @Unique
    private void lmt_wheelPush(InventorySlot selectedInventorySlot, List<InventorySlot> slots, int numItemsToMove) {
        ItemStack selectedInventorySlotStack = selectedInventorySlot.getItem();
        if (lmt_isStackEmpty(selectedInventorySlotStack)) {
            return;
        }

        ItemStack stackOnMouse = Minecraft.getInstance().player.inventory.getCursorItem();
        if (!lmt_isStackEmpty(stackOnMouse) && !selectedInventorySlot.isItemAllowed(stackOnMouse)) {
            return;
        }

        numItemsToMove = Math.min(numItemsToMove, selectedInventorySlotStack.size);
        List<InventorySlot> targetInventorySlots = this.lmt_findPushInventorySlots(selectedInventorySlot, slots, numItemsToMove, false);
        if (targetInventorySlots.isEmpty()) {
            return;
        }

        this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 0);

        for (InventorySlot slot : targetInventorySlots) {
            ItemStack slotStack = slot.getItem();
            int current = lmt_isStackEmpty(slotStack) ? 0 : slotStack.size;
            int max = lmt_isStackEmpty(slotStack) ? slot.getMaxStackSize() : slot.getMaxStackSize(slotStack);
            int clickTimes = Math.min(numItemsToMove, Math.max(0, max - current));
            numItemsToMove -= clickTimes;

            while (clickTimes-- > 0) {
                this.lmt_clickSlot(slot, slot.index, 1, 0);
            }

            if (numItemsToMove <= 0) {
                break;
            }
        }

        this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 0);
    }

    @Unique
    private void lmt_wheelPull(InventorySlot selectedInventorySlot, List<InventorySlot> slots, int numItemsToMove) {
        ItemStack selectedInventorySlotStack = selectedInventorySlot.getItem();
        if (lmt_isStackEmpty(selectedInventorySlotStack)) {
            return;
        }

        int maxItemsToMove = selectedInventorySlot.getMaxStackSize(selectedInventorySlotStack) - selectedInventorySlotStack.size;
        numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
            InventorySlot targetInventorySlot = this.lmt_findPullInventorySlot(selectedInventorySlot, slots);
            if (targetInventorySlot == null) {
                return;
            }

            ItemStack targetStack = targetInventorySlot.getItem();
            if (lmt_isStackEmpty(targetStack)) {
                return;
            }

            ItemStack stackOnMouse = Minecraft.getInstance().player.inventory.getCursorItem();
            if (!lmt_isStackEmpty(stackOnMouse) && !targetInventorySlot.isItemAllowed(stackOnMouse)) {
                return;
            }

            int numItemsInTargetInventorySlot = targetStack.size;
            int numItemsToMoveFromTargetInventorySlot = Math.min(numItemsToMove, numItemsInTargetInventorySlot);
            numItemsToMove -= numItemsToMoveFromTargetInventorySlot;

            this.lmt_clickSlot(targetInventorySlot, targetInventorySlot.index, 0, 0);

            if (numItemsToMoveFromTargetInventorySlot == numItemsInTargetInventorySlot) {
                this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 0, 0);
            } else {
                for (int i = 0; i < numItemsToMoveFromTargetInventorySlot; i++) {
                    this.lmt_clickSlot(selectedInventorySlot, selectedInventorySlot.index, 1, 0);
                }
            }

            this.lmt_clickSlot(targetInventorySlot, targetInventorySlot.index, 0, 0);
        }
    }

    @Unique
    private InventorySlot lmt_findPullInventorySlot(InventorySlot selectedInventorySlot, List<InventorySlot> slots) {
        ItemStack selectedInventorySlotStack = selectedInventorySlot.getItem();
        PlayerInventory playerInventory = Minecraft.getInstance().player.inventory;
        boolean findInPlayerInventory = selectedInventorySlot.inventory != playerInventory;

        boolean reverse = lmt_config().wheelSearchOrder == 1;
        int startIndex = reverse ? slots.size() - 1 : 0;
        int endIndex = reverse ? -1 : slots.size();
        int direction = reverse ? -1 : 1;

        for (int i = startIndex; i != endIndex; i += direction) {
            InventorySlot slot = slots.get(i);
            if (slot == selectedInventorySlot) {
                continue;
            }

            boolean slotInPlayerInventory = slot.inventory == playerInventory;
            if (findInPlayerInventory != slotInPlayerInventory) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (lmt_isStackEmpty(stack)) {
                continue;
            }
            if (!lmt_areStacksCompatible(selectedInventorySlotStack, stack)) {
                continue;
            }

            return slot;
        }

        return null;
    }

    @Unique
    private List<InventorySlot> lmt_findPushInventorySlots(InventorySlot selectedInventorySlot, List<InventorySlot> slots, int itemCount, boolean mustDistributeAll) {
        ItemStack selectedInventorySlotStack = selectedInventorySlot.getItem();
        PlayerInventory playerInventory = Minecraft.getInstance().player.inventory;
        boolean findInPlayerInventory = selectedInventorySlot.inventory != playerInventory;

        List<InventorySlot> result = new ArrayList<InventorySlot>();
        List<InventorySlot> emptyInventorySlots = new ArrayList<InventorySlot>();

        boolean reverse = lmt_config().wheelSearchOrder == 1;
        int startIndex = reverse ? slots.size() - 1 : 0;
        int endIndex = reverse ? -1 : slots.size();
        int direction = reverse ? -1 : 1;

        for (int i = startIndex; i != endIndex && itemCount > 0; i += direction) {
            InventorySlot slot = slots.get(i);
            if (slot == selectedInventorySlot) {
                continue;
            }

            boolean slotInPlayerInventory = slot.inventory == playerInventory;
            if (findInPlayerInventory != slotInPlayerInventory) {
                continue;
            }
            if (slot instanceof CraftingResultSlot) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (lmt_isStackEmpty(stack)) {
                if (slot.isItemAllowed(selectedInventorySlotStack)) {
                    emptyInventorySlots.add(slot);
                }
            } else if (lmt_areStacksCompatible(selectedInventorySlotStack, stack) && stack.size < slot.getMaxStackSize(stack)) {
                result.add(slot);
                itemCount -= Math.min(itemCount, slot.getMaxStackSize(stack) - stack.size);
            }
        }

        for (int i = 0; i < emptyInventorySlots.size() && itemCount > 0; i++) {
            InventorySlot slot = emptyInventorySlots.get(i);
            result.add(slot);
            itemCount -= Math.min(itemCount, slot.getMaxStackSize());
        }

        if (mustDistributeAll && itemCount > 0) {
            return new ArrayList<InventorySlot>();
        }

        return result;
    }

    @Unique
    private boolean lmt_otherInventoryIsAbove(InventorySlot selectedInventorySlot, List<InventorySlot> slots) {
        PlayerInventory playerInventory = Minecraft.getInstance().player.inventory;
        boolean selectedInPlayerInventory = selectedInventorySlot.inventory == playerInventory;

        int otherAbove = 0;
        int otherBelow = 0;

        for (InventorySlot slot : slots) {
            boolean slotInPlayerInventory = slot.inventory == playerInventory;
            if (slotInPlayerInventory == selectedInPlayerInventory) {
                continue;
            }

            if (slot.y < selectedInventorySlot.y) {
                otherAbove++;
            } else {
                otherBelow++;
            }
        }

        return otherAbove > otherBelow;
    }
}
