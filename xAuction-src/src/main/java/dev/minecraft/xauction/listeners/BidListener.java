package dev.minecraft.xauction.listeners;

import dev.minecraft.xauction.XAuctionPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

public final class BidListener implements Listener {
    private final XAuctionPlugin plugin;

    public BidListener(XAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.auctions().isTyping(player) && !plugin.auctions().isConfirming(player)) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> plugin.auctions().submitText(player, text));
    }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        if (!(event.getView() instanceof AnvilView view)) {
            return;
        }
        if (!(view.getPlayer() instanceof Player player) || !plugin.auctions().isAnvilSession(player)) {
            return;
        }
        plugin.auctions().prepareAnvil(view);
        event.setResult(view.getTopInventory().getItem(2));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.auctions().isDummy(event.getCurrentItem()) || plugin.auctions().isDummy(event.getCursor())) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            plugin.auctions().stripDummy(player);
        }
        if (!(event.getView() instanceof AnvilView view) || !plugin.auctions().isAnvilSession(player)) {
            return;
        }
        event.setCancelled(true);
        player.setItemOnCursor(null);
        if (event.getRawSlot() != 2) {
            return;
        }
        String text = view.getRenameText();
        if (text == null || text.isBlank()) {
            plugin.msg().send(player, "bid-invalid", plugin.auctions().placeholders(player));
            plugin.playSound(player, "error");
            return;
        }
        plugin.auctions().submitText(player, text);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.auctions().isAnvilSession(player) || event.getNewItems().values().stream().anyMatch(plugin.auctions()::isDummy)) {
            event.setCancelled(true);
            plugin.auctions().stripDummy(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getView() instanceof AnvilView && plugin.auctions().isAnvilSession(player)) {
            plugin.auctions().closeAnvil(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (plugin.auctions().isDummy(item)) {
            event.setCancelled(true);
            event.getItemDrop().remove();
            plugin.auctions().stripDummy(event.getPlayer());
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.auctions().isDummy(event.getMainHandItem()) || plugin.auctions().isDummy(event.getOffHandItem())
                || plugin.auctions().isAnvilSession(event.getPlayer())) {
            event.setCancelled(true);
            plugin.auctions().stripDummy(event.getPlayer());
        }
    }
}
