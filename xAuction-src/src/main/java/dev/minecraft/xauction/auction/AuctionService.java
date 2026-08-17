package dev.minecraft.xauction.auction;

import dev.minecraft.xauction.XAuctionPlugin;
import dev.minecraft.xauction.gui.MenuHolder;
import dev.minecraft.xauction.util.Money;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class AuctionService {
    private final XAuctionPlugin plugin;
    private final List<Lot> lots = new ArrayList<>();
    private final Map<UUID, UUID> typing = new ConcurrentHashMap<>();
    private final Set<UUID> anvilOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PendingConfirm> confirms = new ConcurrentHashMap<>();
    private Auction current;
    private long nextAt;
    private String lastLotId = "";
    private BossBar bossBar;

    public AuctionService(XAuctionPlugin plugin) {
        this.plugin = plugin;
        reloadLots();
        int delay = Math.max(1, plugin.configs().config().getInt("first-delay-seconds", 10));
        nextAt = System.currentTimeMillis() + delay * 1000L;
    }

    public void startScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void reloadLots() {
        lots.clear();
        ConfigurationSection section = plugin.configs().config().getConfigurationSection("lots");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            Material material = material(s.getString("material"), Material.DIAMOND);
            lots.add(new Lot(
                    id,
                    material,
                    s.getInt("amount", 1),
                    s.getDouble("start-price", 10),
                    s.getString("display", material.name()),
                    s.getInt("weight", 1),
                    s.getBoolean("glow", false),
                    s.getBoolean("rare", false)
            ));
        }
        plugin.getLogger().info("Loaded " + lots.size() + " auction lots");
    }

    public Auction current() {
        return current;
    }

    public boolean isTyping(Player player) {
        UUID auctionId = typing.get(player.getUniqueId());
        return auctionId != null && current != null && !current.finished() && current.id().equals(auctionId);
    }

    public boolean isAnvilSession(Player player) {
        return anvilOpen.contains(player.getUniqueId());
    }

    public boolean isConfirming(Player player) {
        PendingConfirm confirm = confirms.get(player.getUniqueId());
        return confirm != null && confirm.until >= System.currentTimeMillis();
    }

    public void clearTyping(Player player) {
        typing.remove(player.getUniqueId());
    }

    public void clearSession(Player player) {
        typing.remove(player.getUniqueId());
        confirms.remove(player.getUniqueId());
        anvilOpen.remove(player.getUniqueId());
        stripDummy(player);
    }

    public String[] placeholders(Player player) {
        Auction auction = current;
        double min = minBid();
        String price = auction == null ? "—" : plugin.economy().format(auction.currentBid());
        String minText = auction == null ? "—" : plugin.economy().format(min);
        String lock = auction == null || !auction.hasBidder() ? "—" : Money.clock(auction.lockRemainingMs() / 1000);
        String time = auction == null ? "—" : Money.clock(auction.remainingMs() / 1000);
        String balance = player == null || !plugin.economy().ready()
                ? "—"
                : plugin.economy().format(plugin.economy().balance(player));
        boolean rare = auction != null && auction.lot().rare();
        return new String[]{
                "item", auction == null ? "—" : auction.lot().display(),
                "amount", auction == null ? "0" : String.valueOf(auction.lot().amount()),
                "price", price,
                "min", minText,
                "player", auction == null ? "—" : auction.bidderName(),
                "lock", lock,
                "lock_sec", String.valueOf(plugin.configs().config().getInt("bid-lock-seconds", 10)),
                "time", time,
                "balance", balance,
                "rare_label", rare ? "Редкий " : "",
                "rare_line", rare ? "<!italic><gold>Редкий лот</gold>" : "<!italic><dark_gray></dark_gray>"
        };
    }

    public double minBid() {
        if (current == null) {
            return 0;
        }
        double increment = plugin.configs().config().getDouble("min-increment", 1);
        if (current.hasBidder()) {
            return Money.round(current.currentBid() + increment);
        }
        return current.lot().startPrice();
    }

    public void openOrHint(Player player) {
        plugin.gui().openAuction(player);
        if (current == null || current.finished()) {
            plugin.msg().send(player, "waiting-lot");
        }
    }

    public void beginBid(Player player) {
        if (current == null || current.finished()) {
            plugin.msg().send(player, "no-auction");
            return;
        }
        if (!plugin.economy().ready()) {
            plugin.msg().send(player, "no-economy");
            plugin.playSound(player, "error");
            return;
        }
        typing.put(player.getUniqueId(), current.id());
        plugin.msg().send(player, "bid-prompt", placeholders(player));
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> openAnvil(player));
    }

    public void submitText(Player player, String raw) {
        if (raw == null) {
            return;
        }
        String t = raw.trim();
        if (isConfirming(player) && isYes(t)) {
            confirm(player);
            return;
        }
        if (isConfirming(player) && isNo(t)) {
            deny(player);
            return;
        }
        if (t.equalsIgnoreCase("отмена") || t.equalsIgnoreCase("cancel") || t.equals("-") || isNo(t)) {
            clearTyping(player);
            confirms.remove(player.getUniqueId());
            plugin.msg().send(player, "bid-cancel");
            player.closeInventory();
            return;
        }
        boolean decimals = plugin.configs().config().getBoolean("allow-decimal", true);
        Double amount = Money.parse(t, decimals);
        if (amount == null) {
            plugin.msg().send(player, "bid-invalid", placeholders(player));
            plugin.playSound(player, "error");
            return;
        }
        requestBid(player, amount);
    }

    public void requestBid(Player player, double amount) {
        if (!canBid(player, amount, true)) {
            return;
        }
        if (needsConfirm(player, amount)) {
            int seconds = Math.max(5, plugin.configs().config().getInt("confirm-seconds", 15));
            confirms.put(player.getUniqueId(), new PendingConfirm(amount, current.id(), System.currentTimeMillis() + seconds * 1000L));
            clearTyping(player);
            String[] kv = new String[]{"price", plugin.economy().format(amount)};
            plugin.msg().send(player, "confirm-ask", kv);
            plugin.msg().send(player, "confirm-buttons", kv);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.gui().openConfirm(player, amount));
            plugin.playSound(player, "click");
            return;
        }
        if (placeBid(player, amount)) {
            clearTyping(player);
            confirms.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    public void confirm(Player player) {
        PendingConfirm pending = confirms.remove(player.getUniqueId());
        if (pending == null || pending.until < System.currentTimeMillis()) {
            plugin.msg().send(player, "confirm-none");
            return;
        }
        if (current == null || current.finished() || !current.id().equals(pending.auctionId)) {
            plugin.msg().send(player, "no-auction");
            return;
        }
        if (placeBid(player, pending.amount)) {
            player.closeInventory();
        } else {
            plugin.gui().openAuction(player);
        }
    }

    public void deny(Player player) {
        confirms.remove(player.getUniqueId());
        clearTyping(player);
        plugin.msg().send(player, "bid-cancel");
        player.closeInventory();
        if (current != null && !current.finished()) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.gui().openAuction(player));
        }
    }

    public boolean placeBid(Player player, double amount) {
        if (!canBid(player, amount, true)) {
            return false;
        }
        Auction auction = current;
        amount = Money.round(amount);
        double charge = player.getUniqueId().equals(auction.bidder())
                ? Money.round(amount - auction.currentBid())
                : amount;
        UUID previous = auction.bidder();
        double previousBid = auction.currentBid();
        String previousName = auction.bidderName();
        if (!plugin.economy().withdraw(player, charge)) {
            plugin.msg().send(player, "not-enough",
                    "price", plugin.economy().format(charge),
                    "balance", plugin.economy().format(plugin.economy().balance(player)));
            plugin.playSound(player, "error");
            return false;
        }
        if (previous != null && !previous.equals(player.getUniqueId())) {
            OfflinePlayer old = Bukkit.getOfflinePlayer(previous);
            if (!plugin.economy().deposit(old, previousBid)) {
                plugin.getLogger().severe("Failed to refund " + previousName + " " + previousBid);
            } else {
                Player online = Bukkit.getPlayer(previous);
                if (online != null) {
                    plugin.msg().send(online, "outbid", "price", plugin.economy().format(previousBid));
                    plugin.playSound(online, "error");
                }
            }
        }
        int lock = Math.max(1, plugin.configs().config().getInt("bid-lock-seconds", 10));
        auction.bid(player.getUniqueId(), player.getName(), amount, System.currentTimeMillis() + lock * 1000L);
        plugin.msg().send(player, "bid-ok", placeholders(player));
        plugin.playSound(player, "bid");
        if (plugin.configs().config().getBoolean("broadcast-bids", true)) {
            plugin.msg().broadcast("bid-broadcast", placeholders(player));
        }
        plugin.gui().refreshAll();
        return true;
    }

    public void forceNext(boolean announce) {
        if (current != null && !current.finished()) {
            if (current.hasBidder()) {
                win();
            } else {
                expire();
            }
        }
        nextAt = 0;
        startNext();
        if (announce) {
            Bukkit.getOnlinePlayers().forEach(p -> plugin.msg().send(p, "forced-next"));
        }
    }

    public void shutdown() {
        hideBossBar();
        typing.clear();
        confirms.clear();
        anvilOpen.clear();
        if (current != null && !current.finished() && current.hasBidder()) {
            OfflinePlayer old = Bukkit.getOfflinePlayer(current.bidder());
            plugin.economy().deposit(old, current.currentBid());
            current.finish();
        }
        current = null;
    }

    public boolean isDummy(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(dummyKey(), PersistentDataType.BYTE);
    }

    public void stripDummy(Player player) {
        player.setItemOnCursor(null);
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isDummy(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        if (player.getOpenInventory().getTopInventory() instanceof org.bukkit.inventory.AnvilInventory anvil) {
            anvil.setItem(0, null);
            anvil.setItem(1, null);
            anvil.setItem(2, null);
        }
    }

    public void closeAnvil(Player player) {
        anvilOpen.remove(player.getUniqueId());
        if (player.getOpenInventory().getTopInventory() instanceof org.bukkit.inventory.AnvilInventory anvil) {
            anvil.setItem(0, null);
            anvil.setItem(1, null);
            anvil.setItem(2, null);
        }
        Bukkit.getScheduler().runTask(plugin, () -> stripDummy(player));
    }

    public void prepareAnvil(AnvilView view) {
        view.setRepairCost(0);
        view.setMaximumRepairCost(0);
        view.setRepairItemCountCost(0);
        Player player = (Player) view.getPlayer();
        view.getTopInventory().setItem(2, dummyPaper(player, "anvil-result-name"));
    }

    public void showBossBar(Player player) {
        if (bossBar != null && current != null && !current.finished()) {
            player.showBossBar(bossBar);
        }
    }

    private boolean canBid(Player player, double amount, boolean messages) {
        Auction auction = current;
        if (auction == null || auction.finished()) {
            if (messages) {
                plugin.msg().send(player, "no-auction");
            }
            return false;
        }
        if (!plugin.economy().ready()) {
            if (messages) {
                plugin.msg().send(player, "no-economy");
                plugin.playSound(player, "error");
            }
            return false;
        }
        amount = Money.round(amount);
        double min = minBid();
        if (amount + 1e-9 < min) {
            if (messages) {
                plugin.msg().send(player, "bid-low", "price", plugin.economy().format(min));
                plugin.playSound(player, "error");
            }
            return false;
        }
        if (player.getUniqueId().equals(auction.bidder()) && Math.abs(amount - auction.currentBid()) < 1e-9) {
            if (messages) {
                plugin.msg().send(player, "same-bid");
                plugin.playSound(player, "error");
            }
            return false;
        }
        double charge = player.getUniqueId().equals(auction.bidder())
                ? Money.round(amount - auction.currentBid())
                : amount;
        if (!plugin.economy().has(player, charge)) {
            if (messages) {
                plugin.msg().send(player, "not-enough",
                        "price", plugin.economy().format(charge),
                        "balance", plugin.economy().format(plugin.economy().balance(player)));
                plugin.playSound(player, "error");
            }
            return false;
        }
        return true;
    }

    private boolean needsConfirm(Player player, double amount) {
        double percent = plugin.configs().config().getDouble("confirm-percent", 50);
        if (percent <= 0) {
            return false;
        }
        double balance = plugin.economy().balance(player);
        if (current != null && player.getUniqueId().equals(current.bidder())) {
            balance += current.currentBid();
        }
        return amount > balance * (percent / 100.0) + 1e-9;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        expireConfirms(now);
        if (current == null || current.finished()) {
            if (now >= nextAt) {
                startNext();
            }
            return;
        }
        if (current.hasBidder() && now >= current.lockAt()) {
            win();
            afterEnd();
            return;
        }
        if (now >= current.endsAt()) {
            if (current.hasBidder()) {
                win();
            } else {
                expire();
            }
            afterEnd();
            return;
        }
        updateBossBar();
        if (plugin.configs().config().getBoolean("actionbar", false)) {
            String action = current.hasBidder() ? "actionbar-lock" : "actionbar-wait";
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.msg().actionbar(player, action, placeholders(player));
            }
        }
        plugin.gui().refreshAll();
    }

    private void startNext() {
        if (plugin.configs().config().getBoolean("skip-if-empty", true) && Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        Lot lot = pick();
        if (lot == null) {
            plugin.getLogger().warning("No auction lots in config.yml");
            nextAt = System.currentTimeMillis() + 30_000L;
            return;
        }
        int duration = Math.max(15, plugin.configs().config().getInt("duration-seconds", 600));
        current = new Auction(lot, duration * 1000L);
        lastLotId = lot.id();
        plugin.msg().broadcast(lot.rare() ? "start-rare" : "start", placeholders(null));
        if (plugin.configs().config().getBoolean("titles", true)) {
            Title.Times times = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(400));
            String titlePath = lot.rare() ? "title-start-rare" : "title-start";
            String subPath = lot.rare() ? "subtitle-start-rare" : "subtitle-start";
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(Title.title(
                        plugin.msg().parse(player, plugin.msg().raw(titlePath), placeholders(player)),
                        plugin.msg().parse(player, plugin.msg().raw(subPath), placeholders(player)),
                        times
                ));
                plugin.playSound(player, lot.rare() ? "rare" : "start");
            }
        }
        showBossBar();
        plugin.gui().refreshAll();
    }

    private void win() {
        Auction auction = current;
        if (auction == null || auction.finished()) {
            return;
        }
        auction.finish();
        hideBossBar();
        typing.clear();
        confirms.clear();
        String[] kv = placeholders(null);
        plugin.msg().broadcast("win-broadcast", kv);
        UUID winner = auction.bidder();
        Player online = winner == null ? null : Bukkit.getPlayer(winner);
        if (online != null) {
            plugin.msg().send(online, "win-self", kv);
            plugin.playSound(online, "win");
            give(online, auction.lot());
        } else if (winner != null) {
            plugin.pending().add(winner, auction.lot().stack());
        }
        plugin.history().add(new HistoryEntry(
                auction.lot().display(),
                auction.lot().material(),
                auction.lot().amount(),
                auction.bidderName(),
                auction.currentBid(),
                auction.lot().rare(),
                System.currentTimeMillis()
        ));
        current = null;
        closeMenus();
        plugin.gui().refreshAll();
    }

    private void expire() {
        Auction auction = current;
        if (auction == null || auction.finished()) {
            return;
        }
        auction.finish();
        hideBossBar();
        typing.clear();
        confirms.clear();
        plugin.msg().broadcast("expire", placeholders(null));
        plugin.history().add(new HistoryEntry(
                auction.lot().display(),
                auction.lot().material(),
                auction.lot().amount(),
                "никто",
                0,
                auction.lot().rare(),
                System.currentTimeMillis()
        ));
        current = null;
        closeMenus();
        plugin.gui().refreshAll();
    }

    private void afterEnd() {
        int between = Math.max(0, plugin.configs().config().getInt("between-auctions-seconds", 5));
        nextAt = System.currentTimeMillis() + between * 1000L;
        if (between > 0 && !Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.msg().broadcast("next-soon", "time", Money.clock(between));
        }
    }

    private void give(Player player, Lot lot) {
        ItemStack item = lot.stack();
        var leftover = player.getInventory().addItem(item);
        if (leftover.isEmpty()) {
            return;
        }
        plugin.msg().send(player, "inventory-full");
        boolean drop = plugin.configs().config().getBoolean("drop-if-full", true);
        for (ItemStack extra : leftover.values()) {
            if (drop && player.getLocation() != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            } else {
                plugin.pending().add(player.getUniqueId(), extra);
            }
        }
    }

    private Lot pick() {
        if (lots.isEmpty()) {
            return null;
        }
        List<Lot> pool = lots.stream().filter(l -> lots.size() == 1 || !l.id().equals(lastLotId)).toList();
        if (pool.isEmpty()) {
            pool = lots;
        }
        int total = 0;
        for (Lot lot : pool) {
            total += lot.weight();
        }
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = 0;
        for (Lot lot : pool) {
            cursor += lot.weight();
            if (roll < cursor) {
                return lot;
            }
        }
        return pool.getFirst();
    }

    private void openAnvil(Player player) {
        if (!isTyping(player) || current == null) {
            return;
        }
        var view = player.openAnvil(player.getLocation(), true);
        if (!(view instanceof AnvilView anvil)) {
            return;
        }
        anvilOpen.add(player.getUniqueId());
        anvil.setRepairCost(0);
        anvil.setMaximumRepairCost(0);
        anvil.setRepairItemCountCost(0);
        anvil.getTopInventory().setItem(0, dummyPaper(player, "anvil-item-name"));
        plugin.playSound(player, "open");
    }

    private ItemStack dummyPaper(Player player, String namePath) {
        ItemStack paper = new ItemStack(Material.PAPER);
        paper.editMeta(meta -> {
            meta.displayName(plugin.msg().parse(player,
                    plugin.configs().gui().getString(namePath, "{price}"),
                    placeholders(player)));
            meta.getPersistentDataContainer().set(dummyKey(), PersistentDataType.BYTE, (byte) 1);
        });
        return paper;
    }

    private NamespacedKey dummyKey() {
        return plugin.dummyKey();
    }

    private void showBossBar() {
        if (!plugin.configs().config().getBoolean("bossbar", true) || current == null) {
            return;
        }
        if (bossBar == null) {
            bossBar = BossBar.bossBar(Component.text("xAuction"), 1f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10);
        }
        updateBossBar();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    private void updateBossBar() {
        if (bossBar == null || current == null) {
            return;
        }
        String path = current.hasBidder() ? "bossbar-lock" : "bossbar-wait";
        bossBar.name(plugin.msg().parse(plugin.msg().raw(path), placeholders(null)));
        float progress;
        if (current.hasBidder()) {
            int lock = Math.max(1, plugin.configs().config().getInt("bid-lock-seconds", 10));
            progress = (float) (current.lockRemainingMs() / (lock * 1000.0));
        } else {
            int duration = Math.max(1, plugin.configs().config().getInt("duration-seconds", 600));
            progress = (float) (current.remainingMs() / (duration * 1000.0));
        }
        bossBar.progress(Math.max(0f, Math.min(1f, progress)));
        bossBar.color(current.lot().rare() ? BossBar.Color.YELLOW : BossBar.Color.PURPLE);
    }

    private void hideBossBar() {
        if (bossBar == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    private void closeMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                player.closeInventory();
            }
        }
    }

    private void expireConfirms(long now) {
        confirms.entrySet().removeIf(e -> {
            if (e.getValue().until >= now) {
                return false;
            }
            Player player = Bukkit.getPlayer(e.getKey());
            if (player != null) {
                plugin.msg().send(player, "confirm-timeout");
            }
            return true;
        });
    }

    private static boolean isYes(String text) {
        return text.equalsIgnoreCase("да") || text.equalsIgnoreCase("yes") || text.equalsIgnoreCase("y");
    }

    private static boolean isNo(String text) {
        return text.equalsIgnoreCase("нет") || text.equalsIgnoreCase("no") || text.equalsIgnoreCase("n");
    }

    private static Material material(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private record PendingConfirm(double amount, UUID auctionId, long until) {
    }
}
