package dev.minecraft.xauction.gui;

import dev.minecraft.xauction.XAuctionPlugin;
import dev.minecraft.xauction.auction.Auction;
import dev.minecraft.xauction.auction.HistoryEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class GuiService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private final XAuctionPlugin plugin;
    private int frame;

    public GuiService(XAuctionPlugin plugin) {
        this.plugin = plugin;
        int ticks = Math.max(5, plugin.configs().gui().getInt("animate-gradient-ticks", 10));
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, ticks, ticks);
    }

    public void openAuction(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.AUCTION);
        Inventory inv = Bukkit.createInventory(holder, size(), title(player, "auction"));
        holder.inventory(inv);
        fillAuction(player, inv);
        player.openInventory(inv);
        plugin.playSound(player, "open");
    }

    public void openHistory(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.HISTORY);
        Inventory inv = Bukkit.createInventory(holder, size(), title(player, "history"));
        holder.inventory(inv);
        fillHistory(player, inv);
        player.openInventory(inv);
        plugin.playSound(player, "open");
    }

    public void openConfirm(Player player, double amount) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.CONFIRM);
        holder.extra = amount;
        Inventory inv = Bukkit.createInventory(holder, 27, title(player, "confirm"));
        holder.inventory(inv);
        fillConfirm(player, inv, amount);
        player.openInventory(inv);
        plugin.playSound(player, "open");
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder
                    && holder.type() == MenuHolder.Type.AUCTION) {
                fillAuction(player, holder.getInventory());
            }
        }
    }

    private void fillAuction(Player player, Inventory inv) {
        if (plugin.auctions() == null) {
            return;
        }
        inv.clear();
        fillBorder(player, inv, slot("lot-slot", 22), slot("info-slot", 20), slot("timer-slot", 24),
                slot("balance-slot", 30), slot("history-slot", 31), slot("help-slot", 32));
        String[] kv = plugin.auctions().placeholders(player);
        Auction auction = plugin.auctions().current();
        if (auction != null && !auction.finished()) {
            inv.setItem(slot("lot-slot", 22), lotIcon(player, auction, kv));
        }
        put(player, inv, "info-slot", 20, "items.info", kv);
        put(player, inv, "timer-slot", 24, "items.timer", kv);
        put(player, inv, "balance-slot", 30, "items.balance", kv);
        put(player, inv, "history-slot", 31, "items.history", kv);
        put(player, inv, "help-slot", 32, "items.help", kv);
    }

    private void fillHistory(Player player, Inventory inv) {
        inv.clear();
        fillBorder(player, inv, slot("history-back-slot", 40));
        List<Integer> slots = plugin.configs().gui().getIntegerList("history-slots");
        if (slots.isEmpty()) {
            slots = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21);
        }
        List<HistoryEntry> latest = plugin.history().latest();
        for (int i = 0; i < slots.size(); i++) {
            if (i >= latest.size()) {
                inv.setItem(slots.get(i), named(player, Material.GRAY_STAINED_GLASS_PANE,
                        plugin.configs().gui().getString("items.history-empty.name", " "),
                        plugin.configs().gui().getStringList("items.history-empty.lore")));
                continue;
            }
            HistoryEntry entry = latest.get(i);
            String[] kv = new String[]{
                    "item", entry.item(),
                    "winner", entry.winner(),
                    "price", plugin.economy().format(entry.price()),
                    "amount", String.valueOf(entry.amount()),
                    "time", TIME.format(Instant.ofEpochMilli(entry.at()).atZone(ZoneId.systemDefault())),
                    "rare_label", entry.rare() ? "Редкий " : ""
            };
            ItemStack item = new ItemStack(plugin.clients().item(player, entry.material()), Math.min(64, entry.amount()));
            item.editMeta(meta -> {
                if (entry.rare()) {
                    meta.setEnchantmentGlintOverride(true);
                }
                meta.displayName(noItalic(plugin.msg().parse(player,
                        plugin.configs().gui().getString("items.history-lot.name", "{item}"), kv)));
                meta.lore(plugin.msg().lore(player, plugin.configs().gui().getStringList("items.history-lot.lore"), kv)
                        .stream().map(this::noItalic).toList());
            });
            inv.setItem(slots.get(i), item);
        }
        put(player, inv, "history-back-slot", 40, "items.back", plugin.auctions().placeholders(player));
    }

    private void fillConfirm(Player player, Inventory inv, double amount) {
        inv.clear();
        fillBorder(player, inv, slot("confirm-yes-slot", 11), slot("confirm-item-slot", 13), slot("confirm-no-slot", 15));
        String[] kv = new String[]{"price", plugin.economy().format(amount)};
        put(player, inv, "confirm-yes-slot", 11, "items.confirm-yes", kv);
        put(player, inv, "confirm-item-slot", 13, "items.confirm-item", kv);
        put(player, inv, "confirm-no-slot", 15, "items.confirm-no", kv);
    }

    private ItemStack lotIcon(Player player, Auction auction, String[] kv) {
        ItemStack item = new ItemStack(plugin.clients().item(player, auction.lot().material()), auction.lot().amount());
        String namePath = auction.lot().rare() ? "items.lot-rare-name" : "items.lot-name";
        item.editMeta(meta -> {
            if (auction.lot().glow() || auction.lot().rare()) {
                meta.setEnchantmentGlintOverride(true);
            }
            meta.displayName(noItalic(plugin.msg().parse(player,
                    plugin.configs().gui().getString(namePath, "{item}"), kv)));
            meta.lore(plugin.msg().lore(player, plugin.configs().gui().getStringList("items.lot-lore"), kv)
                    .stream().map(this::noItalic).toList());
        });
        return item;
    }

    private void fillBorder(Player player, Inventory inv, int... keepSlots) {
        List<String> mats = plugin.configs().gui().getStringList("border-materials");
        if (mats.isEmpty()) {
            mats = List.of("PURPLE_STAINED_GLASS_PANE");
        }
        ItemStack pane = named(player, material(mats.get(frame % mats.size()), Material.PURPLE_STAINED_GLASS_PANE),
                plugin.configs().gui().getString("border-name", " "),
                plugin.configs().gui().getStringList("border-lore"));
        int s = inv.getSize();
        boolean[] keep = new boolean[s];
        for (int slot : keepSlots) {
            mark(keep, slot);
        }
        for (int i = 0; i < s; i++) {
            if (keep[i]) {
                continue;
            }
            if (i < 9 || i >= s - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, pane);
            }
        }
    }

    private static void mark(boolean[] keep, int slot) {
        if (slot >= 0 && slot < keep.length) {
            keep[slot] = true;
        }
    }

    private void put(Player player, Inventory inv, String path, int def, String itemPath, String[] kv) {
        inv.setItem(slot(path, def), fromCfg(player, itemPath, kv));
    }

    private int slot(String path, int def) {
        return plugin.configs().gui().getInt(path, def);
    }

    private ItemStack fromCfg(Player player, String path, String[] kv) {
        ConfigurationSection s = plugin.configs().gui().getConfigurationSection(path);
        Material mat = material(s == null ? null : s.getString("material"), Material.STONE);
        String name = plugin.configs().gui().getString(path + ".name", path);
        List<String> lore = plugin.configs().gui().getStringList(path + ".lore");
        return named(player, mat, name, lore, kv);
    }

    private ItemStack named(Player player, Material material, String name, List<String> lore) {
        return named(player, material, name, lore, new String[0]);
    }

    private ItemStack named(Player player, Material material, String name, List<String> lore, String[] kv) {
        ItemStack item = new ItemStack(plugin.clients().item(player, material == null ? Material.STONE : material));
        item.editMeta(meta -> {
            meta.displayName(noItalic(plugin.msg().parse(player, name, kv)));
            meta.lore(plugin.msg().lore(player, lore, kv).stream().map(this::noItalic).toList());
        });
        return item;
    }

    private Component title(Player player, String key) {
        List<String> list = plugin.configs().gui().getStringList("titles." + key);
        Component raw;
        if (list.isEmpty()) {
            String single = plugin.configs().gui().getString("titles." + key);
            raw = plugin.msg().parse(single == null ? "<bold>xAuction</bold>" : single);
        } else {
            raw = plugin.msg().parse(list.get(frame % list.size()));
        }
        return plugin.clients().title(player, raw);
    }

    private int size() {
        int size = plugin.configs().gui().getInt("size", 45);
        if (size < 27) {
            return 27;
        }
        return Math.min(54, size / 9 * 9);
    }

    private void tick() {
        frame++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder
                    && holder.type() == MenuHolder.Type.AUCTION) {
                fillBorder(player, holder.getInventory(), slot("lot-slot", 22), slot("info-slot", 20),
                        slot("timer-slot", 24), slot("balance-slot", 30), slot("history-slot", 31), slot("help-slot", 32));
            }
        }
    }

    private Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private Material material(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
