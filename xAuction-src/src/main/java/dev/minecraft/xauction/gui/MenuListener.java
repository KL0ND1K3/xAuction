package dev.minecraft.xauction.gui;

import dev.minecraft.xauction.XAuctionPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class MenuListener implements Listener {
    private final XAuctionPlugin plugin;

    public MenuListener(XAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        int slot = event.getRawSlot();
        switch (holder.type()) {
            case AUCTION -> auction(player, slot);
            case HISTORY -> {
                if (slot == plugin.configs().gui().getInt("history-back-slot", 40)) {
                    plugin.gui().openAuction(player);
                }
            }
            case CONFIRM -> confirm(player, slot);
        }
    }

    private void auction(Player player, int slot) {
        int lot = plugin.configs().gui().getInt("lot-slot", 22);
        int info = plugin.configs().gui().getInt("info-slot", 20);
        int history = plugin.configs().gui().getInt("history-slot", 31);
        if (slot == lot || slot == info) {
            plugin.playSound(player, "click");
            plugin.auctions().beginBid(player);
        } else if (slot == history) {
            plugin.playSound(player, "click");
            plugin.gui().openHistory(player);
        }
    }

    private void confirm(Player player, int slot) {
        if (slot == plugin.configs().gui().getInt("confirm-yes-slot", 11)) {
            plugin.auctions().confirm(player);
        } else if (slot == plugin.configs().gui().getInt("confirm-no-slot", 15)) {
            plugin.auctions().deny(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
