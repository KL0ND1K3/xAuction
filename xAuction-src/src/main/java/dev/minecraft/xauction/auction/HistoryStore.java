package dev.minecraft.xauction.auction;

import dev.minecraft.xauction.XAuctionPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HistoryStore {
    private final XAuctionPlugin plugin;
    private final File file;
    private final List<HistoryEntry> entries = new ArrayList<>();

    public HistoryStore(XAuctionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "history.yml");
    }

    public void load() {
        entries.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> raw = yaml.getMapList("history");
        for (Map<?, ?> map : raw) {
            Material material = Material.STONE;
            try {
                material = Material.valueOf(String.valueOf(map.get("material")));
            } catch (IllegalArgumentException ignored) {
            }
            entries.add(new HistoryEntry(
                    string(map, "item", "лот"),
                    material,
                    asInt(map.get("amount"), 1),
                    string(map, "winner", "никто"),
                    asDouble(map.get("price"), 0),
                    Boolean.parseBoolean(string(map, "rare", "false")),
                    asLong(map.get("at"), System.currentTimeMillis())
            ));
        }
        trim();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> raw = new ArrayList<>();
        for (HistoryEntry entry : entries) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item", entry.item());
            row.put("material", entry.material().name());
            row.put("amount", entry.amount());
            row.put("winner", entry.winner());
            row.put("price", entry.price());
            row.put("rare", entry.rare());
            row.put("at", entry.at());
            raw.add(row);
        }
        yaml.set("history", raw);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Cannot save history.yml: " + e.getMessage());
        }
    }

    public void add(HistoryEntry entry) {
        entries.addFirst(entry);
        trim();
        save();
    }

    public List<HistoryEntry> latest() {
        return List.copyOf(entries);
    }

    private void trim() {
        int max = Math.max(1, plugin.configs().config().getInt("history-size", 10));
        while (entries.size() > max) {
            entries.removeLast();
        }
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double asDouble(Object value, double fallback) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long asLong(Object value, long fallback) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
