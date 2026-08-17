package dev.minecraft.xauction.auction;

import org.bukkit.Material;

public final class HistoryEntry {
    private final String item;
    private final Material material;
    private final int amount;
    private final String winner;
    private final double price;
    private final boolean rare;
    private final long at;

    public HistoryEntry(String item, Material material, int amount, String winner, double price, boolean rare, long at) {
        this.item = item;
        this.material = material == null ? Material.STONE : material;
        this.amount = Math.max(1, amount);
        this.winner = winner == null || winner.isBlank() ? "никто" : winner;
        this.price = price;
        this.rare = rare;
        this.at = at;
    }

    public String item() {
        return item;
    }

    public Material material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public String winner() {
        return winner;
    }

    public double price() {
        return price;
    }

    public boolean rare() {
        return rare;
    }

    public long at() {
        return at;
    }
}
