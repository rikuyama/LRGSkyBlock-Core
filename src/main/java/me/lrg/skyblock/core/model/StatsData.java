package me.lrg.skyblock.core.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StatsData {

    private final UUID uuid;

    private double health;
    private double mana;
    private double strength;
    private double defense;
    private double speed;
    private double criticalChance;
    private double magicFind;

    private final EnumMap<StatsType, Double> extraStats = new EnumMap<>(StatsType.class);
    private volatile boolean dirty;

    public StatsData(
            UUID uuid,
            double health,
            double mana,
            double strength,
            double defense,
            double speed,
            double criticalChance,
            double magicFind
    ) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.health = health;
        this.mana = mana;
        this.strength = strength;
        this.defense = defense;
        this.speed = speed;
        this.criticalChance = criticalChance;
        this.magicFind = magicFind;

        for (StatsType statsType : StatsType.values()) {
            extraStats.put(statsType, statsType.getDefaultValue());
        }

        this.dirty = false;
    }

    public UUID getUuid() { return uuid; }
    public double getHealth() { return health; }
    public double getMana() { return mana; }
    public double getStrength() { return strength; }
    public double getDefense() { return defense; }
    public double getSpeed() { return speed; }
    public double getCriticalChance() { return criticalChance; }
    public double getMagicFind() { return magicFind; }

    public void setHealth(double health) {
        if (Double.compare(this.health, health) != 0) {
            this.health = health;
            markDirty();
        }
    }

    public void setMana(double mana) {
        if (Double.compare(this.mana, mana) != 0) {
            this.mana = mana;
            markDirty();
        }
    }

    public void setStrength(double strength) {
        if (Double.compare(this.strength, strength) != 0) {
            this.strength = strength;
            markDirty();
        }
    }

    public void setDefense(double defense) {
        if (Double.compare(this.defense, defense) != 0) {
            this.defense = defense;
            markDirty();
        }
    }

    public void setSpeed(double speed) {
        if (Double.compare(this.speed, speed) != 0) {
            this.speed = speed;
            markDirty();
        }
    }

    public void setCriticalChance(double criticalChance) {
        if (Double.compare(this.criticalChance, criticalChance) != 0) {
            this.criticalChance = criticalChance;
            markDirty();
        }
    }

    public void setMagicFind(double magicFind) {
        if (Double.compare(this.magicFind, magicFind) != 0) {
            this.magicFind = magicFind;
            markDirty();
        }
    }

    public double getExtraStat(StatsType statsType) {
        Objects.requireNonNull(statsType, "statsType");
        return extraStats.getOrDefault(statsType, statsType.getDefaultValue());
    }

    public void setExtraStat(StatsType statsType, double value) {
        Objects.requireNonNull(statsType, "statsType");
        double currentValue = getExtraStat(statsType);
        if (Double.compare(currentValue, value) != 0) {
            extraStats.put(statsType, value);
            markDirty();
        }
    }

    public Map<StatsType, Double> getExtraStats() {
        return Map.copyOf(extraStats);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }
}
