package me.lrg.skyblock.core.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * 追加Statsの種類。
 *
 * 既存の基本Stats:
 * - health
 * - mana
 * - strength
 * - defense
 * - speed
 * - critical_chance
 * - magic_find
 *
 * 追加Stats:
 * - player_extra_stats テーブルに保存する
 *
 * 表記方針:
 * - Fortune / Wisdom / Ferocity など、Hypixel用語っぽいものはカタカナ寄せ
 * - Ore / Spread / Speed など、直訳の方が自然なものは日本語にする
 * - Intelligenceは使わず、LRG SkyBlockではMana表記に統一する
 */
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
    ORE_FORTUNE("ore_fortune", "鉱石フォーチュン", 0.0, true, StatsCategory.MINING),
    BLOCK_FORTUNE("block_fortune", "ブロックフォーチュン", 0.0, true, StatsCategory.MINING),
    DWARVEN_METAL_FORTUNE("dwarven_metal_fortune", "ドワーフ金属フォーチュン", 0.0, true, StatsCategory.MINING),
    GEMSTONE_FORTUNE("gemstone_fortune", "ジェムストーンフォーチュン", 0.0, true, StatsCategory.MINING),

    FARMING_FORTUNE("farming_fortune", "ファーミングフォーチュン", 0.0, true, StatsCategory.FARMING),
    WHEAT_FORTUNE("wheat_fortune", "小麦フォーチュン", 0.0, true, StatsCategory.FARMING),
    CARROT_FORTUNE("carrot_fortune", "ニンジンフォーチュン", 0.0, true, StatsCategory.FARMING),
    POTATO_FORTUNE("potato_fortune", "ジャガイモフォーチュン", 0.0, true, StatsCategory.FARMING),
    PUMPKIN_FORTUNE("pumpkin_fortune", "カボチャフォーチュン", 0.0, true, StatsCategory.FARMING),
    SUGAR_CANE_FORTUNE("sugar_cane_fortune", "サトウキビフォーチュン", 0.0, true, StatsCategory.FARMING),
    BAMBOO_FORTUNE("bamboo_fortune", "竹フォーチュン", 0.0, true, StatsCategory.FARMING),
    MELON_SLICE_FORTUNE("melon_slice_fortune", "スイカフォーチュン", 0.0, true, StatsCategory.FARMING),
    CACTUS_FORTUNE("cactus_fortune", "サボテンフォーチュン", 0.0, true, StatsCategory.FARMING),
    COCOA_BEANS_FORTUNE("cocoa_beans_fortune", "カカオ豆フォーチュン", 0.0, true, StatsCategory.FARMING),
    MUSHROOM_FORTUNE("mushroom_fortune", "キノコフォーチュン", 0.0, true, StatsCategory.FARMING),
    NETHER_WART_FORTUNE("nether_wart_fortune", "ネザーウォートフォーチュン", 0.0, true, StatsCategory.FARMING),

    FORAGING_FORTUNE("foraging_fortune", "伐採フォーチュン", 0.0, false, StatsCategory.FORAGING),

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