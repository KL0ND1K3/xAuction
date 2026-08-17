package dev.minecraft.xauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {
    public enum Type {AUCTION, HISTORY, CONFIRM}

    private final Type type;
    private Inventory inventory;
    public double extra;

    public MenuHolder(Type type) {
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
