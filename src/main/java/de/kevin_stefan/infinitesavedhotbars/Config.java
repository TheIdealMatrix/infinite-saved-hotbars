package de.kevin_stefan.infinitesavedhotbars;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class Config {

    private static Config config;
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(InfiniteSavedHotbars.MOD_ID + ".json");

    private boolean autoScroll = false;

    private Config() {
    }

    public static Config getInstance() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public boolean getAutoScroll() {
        return autoScroll;
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        save();
    }

    public void save() {
        try {
            Files.writeString(CONFIG_FILE, new GsonBuilder().create().toJson(this, Config.class));
        } catch (IOException e) {
            InfiniteSavedHotbars.LOGGER.error("Failed to save config", e);
        }
    }

    private static Config load() {
        Config config = null;
        try {
            config = new GsonBuilder().create().fromJson(Files.readString(CONFIG_FILE), Config.class);
        } catch (NoSuchFileException ignored) {
        } catch (IOException e) {
            InfiniteSavedHotbars.LOGGER.error("Failed to load config", e);
        }
        if (config == null) {
            return new Config();
        }
        return config;
    }

}
