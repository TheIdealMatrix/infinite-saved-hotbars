package de.kevin_stefan.infinitesavedhotbars;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.screen.slot.SlotActionType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CreativeHotbars {

    private static final int VANILLA_ROWS = 9;
    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("hotbar_extended.nbt");

    private static final ArrayList<ItemStack[]> rows = new ArrayList<>();

    private CreativeHotbars() {
    }

    public static void init(CreativeInventoryScreen.CreativeScreenHandler handler) {
        // Add a separation line
        for (int i = 0; i < 9; i++) {
            handler.itemList.add(Items.GRAY_STAINED_GLASS_PANE.getDefaultStack());
        }

        loadFromFile();

        // Add all rows from internal list to container
        for (ItemStack[] row : rows) {
            handler.itemList.addAll(List.of(row));
        }

        // Add empty row at the bottom
        addEmptyRow(handler);
    }

    /**
     * @return true if the callback should be canceled, otherwise false
     */
    public static boolean onSlotClick(CreativeInventoryScreen.CreativeScreenHandler handler, int slot, SlotActionType actionType) {
        // Ignore vanilla rows
        if (slot < VANILLA_ROWS * 9) {
            return false;
        }

        // Cancel clicks on separator line
        if (slot < (VANILLA_ROWS + 1) * 9) {
            return true;
        }

        if (actionType == SlotActionType.PICKUP) {
            boolean controlPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), InputUtil.GLFW_KEY_LEFT_CONTROL);
            ItemStack cursorStack = handler.getCursorStack();
            ItemStack itemInSlot = handler.itemList.get(slot);

            // Execute vanilla behavior when override key isn't pressed and no item in cursor
            if (!controlPressed && cursorStack.isEmpty()) {
                return false;
            }
            // Do nothing when override key isn't pressed, there is an item in the cursor and the slot isn't empty
            if (!controlPressed && !cursorStack.isEmpty() && !itemInSlot.isEmpty()) {
                return true;
            }

            // Set item from cursor in the container slot
            handler.itemList.set(slot, cursorStack);

            // Add an empty row at the bottom if there is now an item in the last row
            addEmptyRow(handler);

            // Set item from cursor in our internal list
            int row = slot / 9 - (VANILLA_ROWS + 1);
            int index = slot % 9;
            setItem(row, index, cursorStack);

            try {
                saveToFile();
                // Empty the cursor after everything went successful
                handler.setCursorStack(ItemStack.EMPTY);
            } catch (IllegalStateException e) {
                InfiniteSavedHotbars.LOGGER.error("Failed to encode item", e);
                // Revert the slot change on exception
                handler.itemList.set(slot, itemInSlot);
            }
        }

        return true;
    }

    /**
     * Sets the item at the row and index inside our internal list
     */
    private static void setItem(int row, int index, ItemStack item) {
        // Create empty internal rows until the required row exists
        while (rows.size() <= row) {
            ItemStack[] items = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                items[i] = ItemStack.EMPTY;
            }
            rows.add(items);
        }
        rows.get(row)[index] = item;
    }

    /**
     * Adds an empty row at the bottom of the container if last row isn't empty
     */
    private static void addEmptyRow(CreativeInventoryScreen.CreativeScreenHandler handler) {
        int size = handler.itemList.size();
        for (int i = size - 1; i >= size - 9; i--) { // For each item in the last row
            ItemStack item = handler.itemList.get(i);
            if (!item.isEmpty()) { // Add empty row if an item was found
                for (int j = 0; j < 9; j++) {
                    handler.itemList.add(ItemStack.EMPTY);
                }
                break;
            }
        }
    }

    /**
     * Removes all empty rows inside our internal list after the last row with items
     */
    private static void removeEmptyRows() {
        for (int i = rows.size() - 1; i >= 0; i--) {
            ItemStack[] row = rows.get(i);
            for (ItemStack itemStack : row) {
                if (!itemStack.isEmpty()) {
                    return;
                }
            }
            rows.remove(i);
        }
    }

    private static void saveToFile() throws IllegalStateException {
        removeEmptyRows();
        try {
            var registryOps = MinecraftClient.getInstance().world.getRegistryManager().getOps(NbtOps.INSTANCE);
            NbtCompound nbtCompound = NbtHelper.putDataVersion(new NbtCompound());
            for (int i = 0; i < rows.size(); i++) {
                ItemStack[] row = rows.get(i);
                NbtList nbtRow = new NbtList();
                for (ItemStack itemStack : row) {
                    if (itemStack.isEmpty()) {
                        nbtRow.add(new NbtCompound());
                    } else {
                        NbtElement nbtElement = ItemStack.CODEC.encodeStart(registryOps, itemStack).getOrThrow(); // throws IllegalStateException
                        nbtRow.add(nbtElement);
                    }
                }
                nbtCompound.put(String.valueOf(i), nbtRow);
            }

            NbtIo.write(nbtCompound, FILE);
        } catch (IOException | NullPointerException | IllegalStateException e) {
            InfiniteSavedHotbars.LOGGER.error("Failed to save extended creative slots", e);
        }
    }

    private static void loadFromFile() {
        try {
            NbtCompound nbtCompound = NbtIo.read(FILE);
            if (nbtCompound == null) {
                return;
            }

            int dataVersion = NbtHelper.getDataVersion(nbtCompound, 3955); // 1.21.1
            nbtCompound = DataFixTypes.HOTBAR.update(MinecraftClient.getInstance().getDataFixer(), nbtCompound, dataVersion);

            rows.clear();
            var registryOps = MinecraftClient.getInstance().world.getRegistryManager().getOps(NbtOps.INSTANCE);
            int i = 0;
            while (nbtCompound.contains(String.valueOf(i))) {
                NbtList nbtRow = (NbtList) nbtCompound.get(String.valueOf(i));
                ItemStack[] row = new ItemStack[9];
                for (int j = 0; j < nbtRow.size(); j++) {
                    row[j] = ItemStack.CODEC.parse(registryOps, nbtRow.get(j)).resultOrPartial().orElse(ItemStack.EMPTY);
                }
                rows.add(row);
                i++;
            }
        } catch (Exception e) {
            InfiniteSavedHotbars.LOGGER.error("Failed to load extended creative slots", e);
        }
    }

}
