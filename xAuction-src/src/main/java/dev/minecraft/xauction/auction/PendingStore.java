package dev.minecraft.xauction.auction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, List<ItemStack>> pending = new ConcurrentHashMap<>();

    public PendingStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pending.yml");
    }

    public void load() {
        pending.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var root = yaml.getConfigurationSection("pending");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<ItemStack> items = new ArrayList<>();
                List<?> raw = root.getList(key);
                if (raw != null) {
                    for (Object o : raw) {
                        if (o instanceof ItemStack stack) {
                            items.add(stack);
                        }
                    }
                }
                if (!items.isEmpty()) {
                    pending.put(uuid, items);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var e : pending.entrySet()) {
            yaml.set("pending." + e.getKey(), e.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Cannot save pending.yml: " + e.getMessage());
        }
    }

    public void add(UUID uuid, ItemStack item) {
        pending.computeIfAbsent(uuid, id -> new ArrayList<>()).add(item.clone());
        save();
    }

    public void deliver(Player player) {
        List<ItemStack> items = pending.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) {
            return;
        }
        List<ItemStack> leftover = new ArrayList<>();
        for (ItemStack item : items) {
            leftover.addAll(player.getInventory().addItem(item).values());
        }
        if (!leftover.isEmpty()) {
            pending.put(player.getUniqueId(), leftover);
            if (player.getLocation() != null) {
                for (ItemStack item : leftover) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
                pending.remove(player.getUniqueId());
            }
        }
        save();
    }

    public boolean has(UUID uuid) {
        List<ItemStack> items = pending.get(uuid);
        return items != null && !items.isEmpty();
    }
}
