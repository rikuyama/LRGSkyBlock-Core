package me.lrg.skyblock.core.playerlevel.api;

public record PlayerLevelXpResult(
        boolean success,
        int oldLevel,
        int newLevel,
        long addedXp,
        long currentXp,
        long requiredXp,
        PlayerLevelXpReason reason
) {
    public int levelsGained() {
        return Math.max(0, newLevel - oldLevel);
    }
}
