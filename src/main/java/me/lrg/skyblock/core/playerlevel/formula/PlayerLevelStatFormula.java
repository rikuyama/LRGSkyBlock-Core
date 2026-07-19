package me.lrg.skyblock.core.playerlevel.formula;

public final class PlayerLevelStatFormula {
    private static final double HEALTH_PER_LEVEL_UP = 5.0;
    private static final double STRENGTH_PER_FIVE_LEVELS = 1.0;

    private PlayerLevelStatFormula() {}

    public static double healthBonus(int level) {
        validate(level);
        return (level - 1L) * HEALTH_PER_LEVEL_UP;
    }

    public static double strengthBonus(int level) {
        validate(level);
        return (level / 5L) * STRENGTH_PER_FIVE_LEVELS;
    }

    private static void validate(int level) {
        if (level < 1) throw new IllegalArgumentException("level must be at least 1");
    }
}
