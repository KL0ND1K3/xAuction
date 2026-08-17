package dev.minecraft.xauction.config;

import dev.minecraft.xauction.XAuctionPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigManager {
    private final XAuctionPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration gui;
    private FileConfiguration messages;

    public ConfigManager(XAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = loadFile("config.yml");
        gui = loadFile("gui.yml");
        messages = loadFile("messages.yml");
    }

    private FileConfiguration loadFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        InputStream def = plugin.getResource(name);
        if (def != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(def, StandardCharsets.UTF_8)));
        }
        return yaml;
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration gui() {
        return gui;
    }

    public FileConfiguration messages() {
        return messages;
    }
}
