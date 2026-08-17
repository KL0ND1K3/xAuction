package dev.minecraft.xauction.listeners;

import dev.minecraft.xauction.XAuctionPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinListener implements Listener {
    private final XAuctionPlugin plugin;

    public JoinListener(XAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.auctions().showBossBar(player);
        if (plugin.pending().has(player.getUniqueId())) {
            plugin.pending().deliver(player);
            plugin.msg().send(player, "pending-give", "item", "выигрыш с торга");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.auctions().clearSession(event.getPlayer());
    }
}
