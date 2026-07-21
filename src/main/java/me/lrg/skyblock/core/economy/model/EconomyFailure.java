package me.lrg.skyblock.core.economy.model;

public enum EconomyFailure {
    NONE,
    INVALID_AMOUNT,
    PLAYER_NOT_FOUND,
    INSUFFICIENT_WALLET,
    INSUFFICIENT_BANK,
    BANK_CAPACITY,
    MAX_BANK_LEVEL,
    DAILY_LIMIT,
    DUPLICATE_OPERATION,
    DATABASE_ERROR
}
