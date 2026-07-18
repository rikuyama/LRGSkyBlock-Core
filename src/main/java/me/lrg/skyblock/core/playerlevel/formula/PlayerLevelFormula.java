package me.lrg.skyblock.core.playerlevel.formula;

public final class PlayerLevelFormula {

    private static final long BASE_XP = 100L;
    private static final int LEVELS_PER_TIER = 40;
    private static final long XP_INCREASE_PER_TIER = 50L;

    private PlayerLevelFormula() {
    }

    public static long getRequiredXp(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        long tier = (level - 1L) / LEVELS_PER_TIER;
        try {
            return Math.addExact(BASE_XP, Math.multiplyExact(tier, XP_INCREASE_PER_TIER));
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }
}
