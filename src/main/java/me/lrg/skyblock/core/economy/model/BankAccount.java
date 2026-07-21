package me.lrg.skyblock.core.economy.model;

import java.util.UUID;

public record BankAccount(UUID owner, long balance, int level) {
    public BankAccount {
        if (owner == null) throw new NullPointerException("owner");
        if (balance < 0L) throw new IllegalArgumentException("balance must be non-negative");
        if (level < 1) throw new IllegalArgumentException("level must be at least 1");
    }
}
