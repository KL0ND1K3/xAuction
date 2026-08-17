package dev.minecraft.xauction.auction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class Lot {
    private final String id;
    private final Material material;
    private final int amount;
    private final double startPrice;
    private final String display;
    private final int weight;
    private final boolean glow;
    private final boolean rare;

    public Lot(String id, Material material, int amount, double startPrice, String display, int weight, boolean glow, boolean rare) {
        this.id = id;
        this.material = material;
        this.amount = Math.max(1, amount);
        this.startPrice = Math.max(0, startPrice);
        this.display = display == null || display.isBlank() ? material.name() : display;
        this.weight = Math.max(1, weight);
        this.glow = glow || rare;
        this.rare = rare;
    }

    public String id() {
        return id;
    }

    public Material material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public double startPrice() {
        return startPrice;
    }

    public String display() {
        return display;
    }

    public int weight() {
        return weight;
    }

    public boolean glow() {
        return glow;
    }

    public boolean rare() {
        return rare;
    }

    public ItemStack stack() {
        ItemStack item = new ItemStack(material, amount);
        if (glow) {
            item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        }
        return item;
    }
}
