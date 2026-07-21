package me.lrg.skyblock.core.economy.service;

/** Pure bank-interest calculation used by both scheduling and tests. */
public final class InterestCalculator {
    private InterestCalculator() {
    }

    public static long calculateInterest(long balance, double rate, long capacity) {
        if (balance < 0L || capacity <= balance || !Double.isFinite(rate) || rate <= 0.0D) {
            return 0L;
        }
        double calculated = Math.floor(balance * rate);
        if (calculated <= 0.0D) {
            return 0L;
        }
        long interest = calculated >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) calculated;
        return Math.max(0L, Math.min(interest, capacity - balance));
    }
}
