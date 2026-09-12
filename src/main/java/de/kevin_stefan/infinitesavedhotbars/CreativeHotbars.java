package de.kevin_stefan.infinitesavedhotbars;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    public static void init(CreativeModeInventoryScreen.ItemPickerMenu handler) {
        // Add a separation line
        for (int i = 0; i < 9; i++) {
            handler.items.add(Items.STAINED_GLASS_PANE.gray().getDefaultInstance());
        }

        loadFromFile();

        // Add all rows from internal list to container
        for (ItemStack[] row : rows) {
            handler.items.addAll(List.of(row));
        }

        // Add empty row at the bottom
        addEmptyRow(handler);
    }

    /**
     * @return true if the callback should be canceled, otherwise false
     */
    public static boolean onSlotClick(CreativeModeInventoryScreen.ItemPickerMenu handler, int slot, ContainerInput actionType) {
        // Ignore vanilla rows
        if (slot < VANILLA_ROWS * 9) {
            return false;
        }

        // Cancel clicks on separator line
        if (slot < (VANILLA_ROWS + 1) * 9) {
            return true;
        }

        if (actionType == ContainerInput.PICKUP) {
            boolean controlPressed = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL);
            ItemStack cursorStack = handler.getCarried();
            ItemStack itemInSlot = handler.items.get(slot);

            // Execute vanilla behavior when override key isn't pressed and no item in cursor
            if (!controlPressed && cursorStack.isEmpty()) {
                return false;
            }
            // Do nothing when override key isn't pressed, there is an item in the cursor and the slot isn't empty
            if (!controlPressed && !cursorStack.isEmpty() && !itemInSlot.isEmpty()) {
                return true;
            }

            // Set item from cursor in the container slot
            handler.items.set(slot, cursorStack);

            // Add an empty row at the bottom if there is now an item in the last row
            addEmptyRow(handler);

            // Set item from cursor in our internal list
            int row = slot / 9 - (VANILLA_ROWS + 1);
            int index = slot % 9;
            setItem(row, index, cursorStack);

            try {
                saveToFile();
                // Empty the cursor after everything went successful
                handler.setCarried(ItemStack.EMPTY);
            } catch (IllegalStateException e) {
                InfiniteSavedHotbars.LOGGER.error("Failed to encode item", e);
                // Revert the slot change on exception
                handler.items.set(slot, itemInSlot);
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
    private static void addEmptyRow(CreativeModeInventoryScreen.ItemPickerMenu handler) {
        int size = handler.items.size();
        for (int i = size - 1; i >= size - 9; i--) { // For each item in the last row
            ItemStack item = handler.items.get(i);
            if (!item.isEmpty()) { // Add empty row if an item was found
                for (int j = 0; j < 9; j++) {
                    handler.items.add(ItemStack.EMPTY);
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
            var registryOps = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
            CompoundTag nbtCompound = NbtUtils.addCurrentDataVersion(new CompoundTag());
            for (int i = 0; i < rows.size(); i++) {
                ItemStack[] row = rows.get(i);
                ListTag nbtRow = new ListTag();
                for (ItemStack itemStack : row) {
                    if (itemStack.isEmpty()) {
                        nbtRow.add(new CompoundTag());
                    } else {
                        Tag nbtElement = ItemStack.CODEC.encodeStart(registryOps, itemStack).getOrThrow(); // throws IllegalStateException
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
            CompoundTag nbtCompound = NbtIo.read(FILE);
            if (nbtCompound == null) {
                return;
            }

            int dataVersion = NbtUtils.getDataVersion(nbtCompound, 3955); // 1.21.1
            int newDataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
            if (dataVersion != newDataVersion) {
                if (nbtCompound.size() - 1 > 9) { // if there are more than 9 rows
                    int iterations = Math.ceilDivExact(nbtCompound.size() - 1, 9);
                    for (int i = 0; i < iterations; i++) { // iterate over 9 rows at a time
                        CompoundTag newNbtCompound = new CompoundTag();
                        for (int j = 0; j < 9; j++) {
                            int index = i * 9 + j;
                            if (!nbtCompound.contains(String.valueOf(index))) {
                                break;
                            }
                            newNbtCompound.put(String.valueOf(index), nbtCompound.get(String.valueOf(index)));
                            nbtCompound.remove(String.valueOf(index));
                        }
                        newNbtCompound = DataFixTypes.HOTBAR.update(Minecraft.getInstance().getFixerUpper(), newNbtCompound, dataVersion, newDataVersion);
                        nbtCompound.merge(newNbtCompound);
                    }
                } else {
                    nbtCompound = DataFixTypes.HOTBAR.update(Minecraft.getInstance().getFixerUpper(), nbtCompound, dataVersion, newDataVersion);
                }
            }

            rows.clear();
            var registryOps = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
            int i = 0;
            while (nbtCompound.contains(String.valueOf(i))) {
                ListTag nbtRow = (ListTag) nbtCompound.get(String.valueOf(i));
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
