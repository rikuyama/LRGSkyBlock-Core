package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.BaseStatsType;
import me.lrg.skyblock.core.model.CalculatedStats;
import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.model.StatsLayer;
import me.lrg.skyblock.core.model.StatsModifierData;
import me.lrg.skyblock.core.model.StatsType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class StatsCalculationManager {

    private final ConcurrentMap<UUID, ConcurrentMap<StatsLayer, StatsModifierData>> modifiersByPlayer =
            new ConcurrentHashMap<>();

    public CalculatedStats calculate(StatsData baseStats) {
        Objects.requireNonNull(baseStats, "baseStats");

        UUID uuid = baseStats.getUuid();
        ConcurrentMap<StatsLayer, StatsModifierData> layers = modifiersByPlayer.get(uuid);

        double health = baseStats.getHealth();
        double mana = baseStats.getMana();
        double strength = baseStats.getStrength();
        double defense = baseStats.getDefense();
        double speed = baseStats.getSpeed();
        double criticalChance = baseStats.getCriticalChance();
        double magicFind = baseStats.getMagicFind();

        EnumMap<StatsType, Double> extraStats = new EnumMap<>(StatsType.class);
        for (StatsType statsType : StatsType.values()) {
            extraStats.put(statsType, baseStats.getExtraStat(statsType));
        }

        if (layers != null) {
            for (StatsLayer layer : StatsLayer.values()) {
                StatsModifierData modifierData = layers.get(layer);
                if (modifierData == null) {
                    continue;
                }

                health += modifierData.getBaseStat(BaseStatsType.HEALTH);
                mana += modifierData.getBaseStat(BaseStatsType.MANA);
                strength += modifierData.getBaseStat(BaseStatsType.STRENGTH);
                defense += modifierData.getBaseStat(BaseStatsType.DEFENSE);
                speed += modifierData.getBaseStat(BaseStatsType.SPEED);
                criticalChance += modifierData.getBaseStat(BaseStatsType.CRITICAL_CHANCE);
                magicFind += modifierData.getBaseStat(BaseStatsType.MAGIC_FIND);

                for (Map.Entry<StatsType, Double> entry : modifierData.getExtraStats().entrySet()) {
                    extraStats.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }

        return new CalculatedStats(
                uuid,
                sanitizeNonNegative(health),
                sanitizeNonNegative(mana),
                sanitizeNonNegative(strength),
                sanitizeNonNegative(defense),
                sanitizeNonNegative(speed),
                clamp(criticalChance, 0.0, 100.0),
                sanitizeNonNegative(magicFind),
                sanitizeExtraStats(extraStats)
        );
    }

    public void setBaseStatModifier(UUID uuid, StatsLayer layer, BaseStatsType statsType, double value) {
        getOrCreateLayer(uuid, layer).setBaseStat(statsType, value);
        removeEmptyLayer(uuid, layer);
    }

    public void setExtraStatModifier(UUID uuid, StatsLayer layer, StatsType statsType, double value) {
        getOrCreateLayer(uuid, layer).setExtraStat(statsType, value);
        removeEmptyLayer(uuid, layer);
    }

    public void replaceLayer(UUID uuid, StatsLayer layer, StatsModifierData modifierData) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(modifierData, "modifierData");

        if (modifierData.isEmpty()) {
            clearLayer(uuid, layer);
            return;
        }

        modifiersByPlayer
                .computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>())
                .put(layer, modifierData);
    }

    public void clearLayer(UUID uuid, StatsLayer layer) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(layer, "layer");

        ConcurrentMap<StatsLayer, StatsModifierData> layers = modifiersByPlayer.get(uuid);
        if (layers == null) {
            return;
        }

        layers.remove(layer);
        if (layers.isEmpty()) {
            modifiersByPlayer.remove(uuid, layers);
        }
    }

    public void clearPlayer(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        modifiersByPlayer.remove(uuid);
    }

    public void clear() {
        modifiersByPlayer.clear();
    }

    public Map<StatsLayer, StatsModifierData> getLayerSnapshot(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        ConcurrentMap<StatsLayer, StatsModifierData> layers = modifiersByPlayer.get(uuid);
        if (layers == null || layers.isEmpty()) {
            return Map.of();
        }

        EnumMap<StatsLayer, StatsModifierData> snapshot = new EnumMap<>(StatsLayer.class);
        for (Map.Entry<StatsLayer, StatsModifierData> entry : layers.entrySet()) {
            StatsModifierData copy = new StatsModifierData();
            entry.getValue().getBaseStats().forEach(copy::setBaseStat);
            entry.getValue().getExtraStats().forEach(copy::setExtraStat);
            snapshot.put(entry.getKey(), copy);
        }
        return Map.copyOf(snapshot);
    }

    public Optional<StatsModifierData> getLayerSnapshot(UUID uuid, StatsLayer layer) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(layer, "layer");
        return Optional.ofNullable(getLayerSnapshot(uuid).get(layer));
    }

    private StatsModifierData getOrCreateLayer(UUID uuid, StatsLayer layer) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(layer, "layer");

        ConcurrentMap<StatsLayer, StatsModifierData> layers = modifiersByPlayer
                .computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>());

        return layers.computeIfAbsent(layer, ignored -> new StatsModifierData());
    }

    private void removeEmptyLayer(UUID uuid, StatsLayer layer) {
        ConcurrentMap<StatsLayer, StatsModifierData> layers = modifiersByPlayer.get(uuid);
        if (layers == null) {
            return;
        }

        synchronized (layers) {
            StatsModifierData modifierData = layers.get(layer);
            if (modifierData != null && modifierData.isEmpty()) {
                layers.remove(layer);
            }
            if (layers.isEmpty()) {
                modifiersByPlayer.remove(uuid, layers);
            }
        }
    }

    private EnumMap<StatsType, Double> sanitizeExtraStats(EnumMap<StatsType, Double> values) {
        EnumMap<StatsType, Double> sanitized = new EnumMap<>(StatsType.class);
        for (StatsType statsType : StatsType.values()) {
            sanitized.put(statsType, sanitizeNonNegative(values.getOrDefault(statsType, statsType.getDefaultValue())));
        }
        return sanitized;
    }

    private double sanitizeNonNegative(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, value);
    }

    private double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
