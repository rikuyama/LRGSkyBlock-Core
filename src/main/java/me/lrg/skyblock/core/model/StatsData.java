package me.lrg.skyblock.core.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * プレイヤーのステータスデータを保持するModel。
 *
 * 注意:
 * - SQLは書かない
 * - Bukkit APIは使わない
 * - ゲームロジックを詰め込みすぎない
 */
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
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMana() {
        return mana;
    }

    public void setMana(double mana) {
        this.mana = mana;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public double getDefense() {
        return defense;
    }

    public void setDefense(double defense) {
        this.defense = defense;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getCriticalChance() {
        return criticalChance;
    }

    public void setCriticalChance(double criticalChance) {
        this.criticalChance = criticalChance;
    }

    public double getMagicFind() {
        return magicFind;
    }

    public void setMagicFind(double magicFind) {
        this.magicFind = magicFind;
    }

    public double getExtraStat(StatsType statsType) {
        Objects.requireNonNull(statsType, "statsType");
        return extraStats.getOrDefault(statsType, statsType.getDefaultValue());
    }

    public void setExtraStat(StatsType statsType, double value) {
        Objects.requireNonNull(statsType, "statsType");
        extraStats.put(statsType, value);
    }

    public Map<StatsType, Double> getExtraStats() {
        return Map.copyOf(extraStats);
    }
}