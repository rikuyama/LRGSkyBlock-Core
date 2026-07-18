package me.lrg.skyblock.core.playerlevel.model;

import me.lrg.skyblock.core.playerlevel.formula.PlayerLevelFormula;

import java.util.Objects;
import java.util.UUID;

public final class PlayerLevelData {

    private final UUID uuid;
    private int level;
    private long currentXp;
    private long totalXp;
    private volatile boolean dirty;

    public PlayerLevelData(UUID uuid, int level, long currentXp, long totalXp) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.level = requireValidLevel(level);
        this.currentXp = requireNonNegative(currentXp, "currentXp");
        this.totalXp = requireNonNegative(totalXp, "totalXp");
        normalizeCurrentXp();
        this.dirty = false;
    }

    public static PlayerLevelData createDefault(UUID uuid) {
        return new PlayerLevelData(uuid, 1, 0L, 0L);
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public long getCurrentXp() {
        return currentXp;
    }

    public long getTotalXp() {
        return totalXp;
    }

    public long getRequiredXp() {
        return PlayerLevelFormula.getRequiredXp(level);
    }

    public int addXp(long amount) {
        requireNonNegative(amount, "amount");
        if (amount == 0L) {
            return 0;
        }

        currentXp = saturatedAdd(currentXp, amount);
        totalXp = saturatedAdd(totalXp, amount);

        int levelsGained = normalizeCurrentXp();
        dirty = true;
        return levelsGained;
    }

    public void removeXp(long amount) {
        requireNonNegative(amount, "amount");
        long next = Math.max(0L, currentXp - Math.min(currentXp, amount));
        if (next != currentXp) {
            currentXp = next;
            dirty = true;
        }
    }

    public int setCurrentXp(long xp) {
        requireNonNegative(xp, "xp");
        if (currentXp == xp) {
            return 0;
        }

        currentXp = xp;
        int levelsGained = normalizeCurrentXp();
        dirty = true;
        return levelsGained;
    }

    public void setLevel(int level) {
        int validLevel = requireValidLevel(level);
        if (this.level != validLevel) {
            this.level = validLevel;
            normalizeCurrentXp();
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    private int normalizeCurrentXp() {
        int gained = 0;
        while (level < Integer.MAX_VALUE) {
            long requiredXp = PlayerLevelFormula.getRequiredXp(level);
            if (currentXp < requiredXp) {
                break;
            }
            currentXp -= requiredXp;
            level++;
            gained++;
        }
        return gained;
    }

    private static int requireValidLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        return level;
    }

    private static long requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }
}
