package me.lrg.skyblock.core.playerlevel.formula;

public final class PlayerLevelFormula {

    private static final long XP_PER_LEVEL = 100L;

    private PlayerLevelFormula() {
    }

    public static long getRequiredXp(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }

        try {
            return Math.multiplyExact(XP_PER_LEVEL, level);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }
}
