package de.kevin_stefan.infinitesavedhotbars.mixin;

import de.kevin_stefan.infinitesavedhotbars.Config;
import de.kevin_stefan.infinitesavedhotbars.CreativeHotbars;
import de.kevin_stefan.infinitesavedhotbars.CustomCheckboxWidget;
import de.kevin_stefan.infinitesavedhotbars.InfiniteSavedHotbars;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractContainerScreen<ItemPickerMenu> implements FabricCreativeModeInventoryScreen {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Unique
    private CustomCheckboxWidget checkbox;

    public CreativeInventoryScreenMixin(ItemPickerMenu screenHandler, Inventory inventory, Component text) {
        super(screenHandler, inventory, text);
    }

    @Shadow
    protected abstract boolean isCreativeSlot(@Nullable Slot slot);

    @Inject(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;getType()Lnet/minecraft/world/item/CreativeModeTab$Type;", shift = At.Shift.BEFORE, ordinal = 2))
    private void setSelectedTab(CreativeModeTab group, CallbackInfo info) {
        if (group.getType() != CreativeModeTab.Type.HOTBAR) {
            return;
        }

        CreativeHotbars.init(menu);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo info) {
        if (selectedTab.getType() != CreativeModeTab.Type.HOTBAR || !isCreativeSlot(slot)) {
            return;
        }

        int slotIndex = getSlotIndex(slotId);
        if (CreativeHotbars.onSlotClick(menu, slotIndex, actionType)) {
            // Update the view
            menu.scrollTo(scrollOffs);

            info.cancel();
        }
    }

    @Inject(method = "selectTab", at = @At("RETURN"))
    private void handleAutoScroll(CreativeModeTab group, CallbackInfo info) {
        if (checkbox == null) {
            checkbox = createCheckBox();
            this.addRenderableWidget(checkbox);
        }

        if (group.getType() != CreativeModeTab.Type.HOTBAR) {
            checkbox.visible = false;
            return;
        }
        checkbox.visible = true;

        if (Config.getInstance().getAutoScroll()) {
            this.scrollOffs = menu.getScrollForRowIndex(10);
            menu.scrollTo(scrollOffs);
        }
    }

    @Inject(method = "resize", at = @At("RETURN"))
    private void resize(int width, int height, CallbackInfo ci) {
        this.removeWidget(checkbox);
        checkbox = createCheckBox();
        this.addRenderableWidget(checkbox);
    }

    /**
     * @return index of the slot in the handler.itemList
     */
    @Unique
    private int getSlotIndex(int slotId) {
        try {
            int row = menu.getRowIndexForScroll(scrollOffs);
            return row * 9 + slotId;
        } catch (Exception e) {
            InfiniteSavedHotbars.LOGGER.error("Failed to get slot index", e);
            return -1;
        }
    }

    @Unique
    private CustomCheckboxWidget createCheckBox() {
        int x = this.leftPos + 180;
        int y = this.topPos + 5;
        int i = 10;
        if (this.getPageCount() > 1) {
            x -= i + 11;
        }
        boolean checked = Config.getInstance().getAutoScroll();
        CustomCheckboxWidget checkbox = new CustomCheckboxWidget(x, y, i, i, checked, (widget, isChecked) -> {
            Config.getInstance().setAutoScroll(isChecked);
        });
        checkbox.setTooltip(Tooltip.create(Component.translatable("inventory.hotbarCheckbox")));
        return checkbox;
    }

}
