package me.lrg.skyblock.core.economy.model;

public record EconomyResult(
        boolean success,
        EconomyFailure failure,
        long amount,
        long fee,
        long walletBalance,
        long bankBalance
) {
    public static EconomyResult success(long amount, long fee, long walletBalance, long bankBalance) {
        return new EconomyResult(true, EconomyFailure.NONE, amount, fee, walletBalance, bankBalance);
    }

    public static EconomyResult failure(EconomyFailure failure) {
        return new EconomyResult(false, failure, 0L, 0L, -1L, -1L);
    }
}
