package de.kevin_stefan.infinitesavedhotbars.mixin;

import de.kevin_stefan.infinitesavedhotbars.Config;
import de.kevin_stefan.infinitesavedhotbars.CreativeHotbars;
import de.kevin_stefan.infinitesavedhotbars.InfiniteSavedHotbars;
import de.kevin_stefan.infinitesavedhotbars.CustomCheckboxWidget;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeScreenHandler> implements FabricCreativeInventoryScreen {

    @Shadow
    private static ItemGroup selectedTab;

    @Shadow
    private float scrollPosition;

    @Unique
    private CustomCheckboxWidget checkbox;

    public CreativeInventoryScreenMixin(CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Shadow
    protected abstract boolean isCreativeInventorySlot(@Nullable Slot slot);

    @Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGroup;getType()Lnet/minecraft/item/ItemGroup$Type;", shift = At.Shift.BEFORE, ordinal = 2))
    private void setSelectedTab(ItemGroup group, CallbackInfo info) {
        if (group.getType() != ItemGroup.Type.HOTBAR) {
            return;
        }

        CreativeHotbars.init(handler);
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo info) {
        if (selectedTab.getType() != ItemGroup.Type.HOTBAR || !isCreativeInventorySlot(slot)) {
            return;
        }

        int slotIndex = getSlotIndex(slotId);
        if (CreativeHotbars.onSlotClick(handler, slotIndex, actionType)) {
            // Update the view
            handler.scrollItems(scrollPosition);

            info.cancel();
        }
    }

    @Inject(method = "setSelectedTab", at = @At("RETURN"))
    private void handleAutoScroll(ItemGroup group, CallbackInfo info) {
        if (checkbox == null) {
            checkbox = createCheckBox();
            this.addDrawableChild(checkbox);
        }

        if (group.getType() != ItemGroup.Type.HOTBAR) {
            checkbox.visible = false;
            return;
        }
        checkbox.visible = true;

        if (Config.getInstance().getAutoScroll()) {
            this.scrollPosition = handler.getScrollPosition(10);
            handler.scrollItems(scrollPosition);
        }
    }

    @Inject(method = "resize", at = @At("RETURN"))
    private void resize(MinecraftClient client, int width, int height, CallbackInfo ci) {
        this.remove(checkbox);
        checkbox = createCheckBox();
        this.addDrawableChild(checkbox);
    }

    /**
     * @return index of the slot in the handler.itemList
     */
    @Unique
    private int getSlotIndex(int slotId) {
        try {
            int row = handler.getRow(scrollPosition);
            return row * 9 + slotId;
        } catch (Exception e) {
            InfiniteSavedHotbars.LOGGER.error("Failed to get slot index", e);
            return -1;
        }
    }

    @Unique
    private CustomCheckboxWidget createCheckBox() {
        int x = this.x + this.backgroundWidth - 15;
        int y = this.y + 5;
        int i = 9;
        boolean checked = Config.getInstance().getAutoScroll();
        return new CustomCheckboxWidget(x, y, i, i, checked, (widget, isChecked) -> {
            Config.getInstance().setAutoScroll(isChecked);
        });
    }

}
