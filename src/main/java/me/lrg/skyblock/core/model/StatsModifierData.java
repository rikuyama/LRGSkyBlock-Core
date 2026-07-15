package me.lrg.skyblock.core.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class StatsModifierData {

    private final EnumMap<BaseStatsType, Double> baseStats = new EnumMap<>(BaseStatsType.class);
    private final EnumMap<StatsType, Double> extraStats = new EnumMap<>(StatsType.class);

    public synchronized double getBaseStat(BaseStatsType statsType) {
        Objects.requireNonNull(statsType, "statsType");
        return baseStats.getOrDefault(statsType, 0.0);
    }

    public synchronized void setBaseStat(BaseStatsType statsType, double value) {
        Objects.requireNonNull(statsType, "statsType");

        if (Double.compare(value, 0.0) == 0) {
            baseStats.remove(statsType);
            return;
        }

        baseStats.put(statsType, value);
    }

    public synchronized double getExtraStat(StatsType statsType) {
        Objects.requireNonNull(statsType, "statsType");
        return extraStats.getOrDefault(statsType, 0.0);
    }

    public synchronized void setExtraStat(StatsType statsType, double value) {
        Objects.requireNonNull(statsType, "statsType");

        if (Double.compare(value, 0.0) == 0) {
            extraStats.remove(statsType);
            return;
        }

        extraStats.put(statsType, value);
    }

    public synchronized Map<BaseStatsType, Double> getBaseStats() {
        return Map.copyOf(baseStats);
    }

    public synchronized Map<StatsType, Double> getExtraStats() {
        return Map.copyOf(extraStats);
    }

    public synchronized boolean isEmpty() {
        return baseStats.isEmpty() && extraStats.isEmpty();
    }
}
