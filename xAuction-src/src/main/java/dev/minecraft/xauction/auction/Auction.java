package dev.minecraft.xauction.auction;

import java.util.UUID;

public final class Auction {
    private final UUID id = UUID.randomUUID();
    private final Lot lot;
    private final long startedAt;
    private final long endsAt;
    private double currentBid;
    private UUID bidder;
    private String bidderName = "—";
    private long lockAt;
    private boolean finished;

    public Auction(Lot lot, long durationMillis) {
        this.lot = lot;
        this.startedAt = System.currentTimeMillis();
        this.endsAt = startedAt + durationMillis;
        this.currentBid = lot.startPrice();
    }

    public UUID id() {
        return id;
    }

    public Lot lot() {
        return lot;
    }

    public long startedAt() {
        return startedAt;
    }

    public long endsAt() {
        return endsAt;
    }

    public double currentBid() {
        return currentBid;
    }

    public UUID bidder() {
        return bidder;
    }

    public String bidderName() {
        return bidderName;
    }

    public long lockAt() {
        return lockAt;
    }

    public boolean finished() {
        return finished;
    }

    public boolean hasBidder() {
        return bidder != null;
    }

    public void bid(UUID player, String name, double amount, long lockAt) {
        this.bidder = player;
        this.bidderName = name;
        this.currentBid = amount;
        this.lockAt = lockAt;
    }

    public void finish() {
        this.finished = true;
    }

    public long remainingMs() {
        return Math.max(0, endsAt - System.currentTimeMillis());
    }

    public long lockRemainingMs() {
        if (bidder == null) {
            return 0;
        }
        return Math.max(0, lockAt - System.currentTimeMillis());
    }
}
