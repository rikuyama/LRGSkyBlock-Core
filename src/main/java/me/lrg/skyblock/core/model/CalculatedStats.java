package me.lrg.skyblock.core.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CalculatedStats {

    private final UUID uuid;
    private final double health;
    private final double mana;
    private final double strength;
    private final double defense;
    private final double speed;
    private final double criticalChance;
    private final double magicFind;
    private final EnumMap<StatsType, Double> extraStats;

    public CalculatedStats(
            UUID uuid,
            double health,
            double mana,
            double strength,
            double defense,
            double speed,
            double criticalChance,
            double magicFind,
            Map<StatsType, Double> extraStats
    ) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.health = health;
        this.mana = mana;
        this.strength = strength;
        this.defense = defense;
        this.speed = speed;
        this.criticalChance = criticalChance;
        this.magicFind = magicFind;
        this.extraStats = new EnumMap<>(StatsType.class);
        this.extraStats.putAll(Objects.requireNonNull(extraStats, "extraStats"));
    }

    public UUID getUuid() { return uuid; }
    public double getHealth() { return health; }
    public double getMana() { return mana; }
    public double getStrength() { return strength; }
    public double getDefense() { return defense; }
    public double getSpeed() { return speed; }
    public double getCriticalChance() { return criticalChance; }
    public double getMagicFind() { return magicFind; }

    public double getExtraStat(StatsType statsType) {
        Objects.requireNonNull(statsType, "statsType");
        return extraStats.getOrDefault(statsType, statsType.getDefaultValue());
    }

    public Map<StatsType, Double> getExtraStats() {
        return Map.copyOf(extraStats);
    }
}
