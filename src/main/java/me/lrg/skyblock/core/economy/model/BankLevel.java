package me.lrg.skyblock.core.economy.model;

public record BankLevel(int level, long capacity, long upgradeCost) {
    public BankLevel {
        if (level < 1) throw new IllegalArgumentException("level must be at least 1");
        if (capacity < 0L) throw new IllegalArgumentException("capacity must be non-negative");
        if (upgradeCost < 0L) throw new IllegalArgumentException("upgradeCost must be non-negative");
    }
}
