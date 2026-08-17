package dev.minecraft.xauction;

import dev.minecraft.xauction.auction.AuctionService;
import dev.minecraft.xauction.auction.HistoryStore;
import dev.minecraft.xauction.auction.PendingStore;
import dev.minecraft.xauction.command.AuctionCommand;
import dev.minecraft.xauction.compat.Clients;
import dev.minecraft.xauction.config.ConfigManager;
import dev.minecraft.xauction.config.Msg;
import dev.minecraft.xauction.economy.EconomyService;
import dev.minecraft.xauction.gui.GuiService;
import dev.minecraft.xauction.gui.MenuListener;
import dev.minecraft.xauction.listeners.BidListener;
import dev.minecraft.xauction.listeners.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class XAuctionPlugin extends JavaPlugin {
    private ConfigManager configs;
    private Msg msg;
    private Clients clients;
    private EconomyService economy;
    private PendingStore pending;
    private HistoryStore history;
    private AuctionService auctions;
    private GuiService gui;
    private NamespacedKey dummyKey;

    @Override
    public void onEnable() {
        configs = new ConfigManager(this);
        configs.load();
        clients = new Clients();
        msg = new Msg(configs, clients);
        economy = new EconomyService(this);
        economy.hook();
        pending = new PendingStore(this);
        pending.load();
        history = new HistoryStore(this);
        history.load();
        dummyKey = new NamespacedKey(this, "anvil_dummy");
        gui = new GuiService(this);
        auctions = new AuctionService(this);
        auctions.startScheduler();

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BidListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);

        var command = getCommand("auction");
        if (command != null) {
            AuctionCommand executor = new AuctionCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("xAuction enabled. Economy: " + economy.describe());
    }

    @Override
    public void onDisable() {
        if (auctions != null) {
            auctions.shutdown();
        }
        if (pending != null) {
            pending.save();
        }
        if (history != null) {
            history.save();
        }
    }

    public void reloadAll() {
        configs.load();
        economy.hook();
        auctions.reloadLots();
    }

    public void playSound(Player player, String key) {
        String name = configs.config().getString("sounds." + key);
        if (name == null) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(name);
            sound = clients.sound(player, sound);
            player.playSound(player.getLocation(), sound, 0.75f, 1.15f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public ConfigManager configs() {
        return configs;
    }

    public Msg msg() {
        return msg;
    }

    public Clients clients() {
        return clients;
    }

    public EconomyService economy() {
        return economy;
    }

    public PendingStore pending() {
        return pending;
    }

    public HistoryStore history() {
        return history;
    }

    public NamespacedKey dummyKey() {
        return dummyKey;
    }

    public AuctionService auctions() {
        return auctions;
    }

    public GuiService gui() {
        return gui;
    }
}
