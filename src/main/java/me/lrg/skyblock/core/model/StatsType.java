package me.lrg.skyblock.core.model;

import java.util.Arrays;
import java.util.Optional;

public enum StatsType {

    CRIT_DAMAGE("crit_damage", "クリティカルダメージ", 50.0, true, StatsCategory.COMBAT),
    FEROCITY("ferocity", "フェロシティ", 0.0, false, StatsCategory.COMBAT),
    ABILITY_DAMAGE("ability_damage", "アビリティダメージ", 0.0, false, StatsCategory.COMBAT),
    HEALTH_REGEN("health_regen", "体力回復", 100.0, false, StatsCategory.COMBAT),
    VITALITY("vitality", "回復効果", 100.0, false, StatsCategory.COMBAT),

    BREAKING_POWER("breaking_power", "採掘力", 0.0, false, StatsCategory.MINING),
    MINING_SPEED("mining_speed", "採掘速度", 0.0, false, StatsCategory.MINING),
    MINING_SPREAD("mining_spread", "採掘範囲", 0.0, false, StatsCategory.MINING),
    GEMSTONE_SPREAD("gemstone_spread", "ジェムストーン採掘範囲", 0.0, false, StatsCategory.MINING),
    PRISTINE("pristine", "プリスティン", 0.0, false, StatsCategory.MINING),
    MINING_FORTUNE("mining_fortune", "マイニングフォーチュン", 0.0, true, StatsCategory.MINING),

    FARMING_FORTUNE("farming_fortune", "ファーミングフォーチュン", 0.0, true, StatsCategory.FARMING),

    FORAGING_FORTUNE("foraging_fortune", "伐採フォーチュン", 0.0, true, StatsCategory.FORAGING),

    FISHING_SPEED("fishing_speed", "釣り速度", 0.0, false, StatsCategory.FISHING),
    SEA_CREATURE_CHANCE("sea_creature_chance", "海の生物出現率", 20.0, false, StatsCategory.FISHING),
    DOUBLE_HOOK_CHANCE("double_hook_chance", "ダブルフック率", 0.0, false, StatsCategory.FISHING),

    PET_LUCK("pet_luck", "ペット運", 0.0, false, StatsCategory.LUCK),

    COMBAT_WISDOM("combat_wisdom", "戦闘ウィズダム", 0.0, false, StatsCategory.WISDOM),
    FARMING_WISDOM("farming_wisdom", "農業ウィズダム", 0.0, false, StatsCategory.WISDOM),
    FISHING_WISDOM("fishing_wisdom", "釣りウィズダム", 0.0, false, StatsCategory.WISDOM),
    MINING_WISDOM("mining_wisdom", "採掘ウィズダム", 0.0, false, StatsCategory.WISDOM),
    FORAGING_WISDOM("foraging_wisdom", "伐採ウィズダム", 0.0, false, StatsCategory.WISDOM),
    ENCHANTING_WISDOM("enchanting_wisdom", "エンチャントウィズダム", 0.0, false, StatsCategory.WISDOM),
    ALCHEMY_WISDOM("alchemy_wisdom", "醸造ウィズダム", 0.0, false, StatsCategory.WISDOM),
    CARPENTRY_WISDOM("carpentry_wisdom", "大工ウィズダム", 0.0, false, StatsCategory.WISDOM),
    TAMING_WISDOM("taming_wisdom", "ペット育成ウィズダム", 0.0, false, StatsCategory.WISDOM),
    SOCIAL_WISDOM("social_wisdom", "ソーシャルウィズダム", 0.0, false, StatsCategory.WISDOM);

    private final String key;
    private final String displayName;
    private final double defaultValue;
    private final boolean implemented;
    private final StatsCategory category;

    StatsType(
            String key,
            String displayName,
            double defaultValue,
            boolean implemented,
            StatsCategory category
    ) {
        this.key = key;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.implemented = implemented;
        this.category = category;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public boolean isImplemented() {
        return implemented;
    }

    public StatsCategory getCategory() {
        return category;
    }

    public String getImplementationStatusText() {
        if (implemented) {
            return "§a実装済み";
        }

        return "§7未実装";
    }

    public static Optional<StatsType> fromKey(String key) {
        return Arrays.stream(values())
                .filter(statsType -> statsType.key.equalsIgnoreCase(key))
                .findFirst();
    }
}
