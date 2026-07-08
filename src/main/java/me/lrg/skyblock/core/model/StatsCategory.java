package me.lrg.skyblock.core.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Stats表示用カテゴリ。
 *
 * 注意:
 * - DBには保存しない
 * - /stats <category> の表示分けに使う
 */
public enum StatsCategory {

    COMBAT("combat", "戦闘Stats"),
    MINING("mining", "採掘Stats"),
    FARMING("farming", "農業Stats"),
    FORAGING("foraging", "伐採Stats"),
    FISHING("fishing", "釣りStats"),
    WISDOM("wisdom", "Wisdom系Stats"),
    LUCK("luck", "運Stats");

    private final String key;
    private final String displayName;

    StatsCategory(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<StatsCategory> fromKey(String key) {
        return Arrays.stream(values())
                .filter(category -> category.key.equalsIgnoreCase(key))
                .findFirst();
    }
}