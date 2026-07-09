package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.StatsType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fortune系Statsの計算を扱うManager。
 *
 * このクラスの役割:
 * - Fortune値からドロップ数を計算する
 * - 採掘 / 農業 / 伐採ごとのFortune値を取得する
 *
 * 注意:
 * - SQLは書かない
 * - 実際にアイテムを落とす処理は書かない
 * - BlockBreakEventなどのイベント処理はListenerに任せる
 */
public class FortuneManager {

    private final StatsManager statsManager;

    public FortuneManager(StatsManager statsManager) {
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    /**
     * Fortuneを反映した最終ドロップ数を返す。
     *
     * 例:
     * baseAmount = 1, fortune = 0   -> 1個
     * baseAmount = 1, fortune = 100 -> 2個
     * baseAmount = 1, fortune = 200 -> 3個
     * baseAmount = 1, fortune = 250 -> 3個確定 + 50%で4個
     */
    public int calculateDropAmount(int baseAmount, double fortune) {
        if (baseAmount <= 0) {
            return 0;
        }

        double safeFortune = Math.max(0.0, fortune);

        int guaranteedExtraDrops = (int) Math.floor(safeFortune / 100.0);
        double bonusChance = safeFortune % 100.0;

        int result = baseAmount * (1 + guaranteedExtraDrops);

        for (int i = 0; i < baseAmount; i++) {
            if (rollChance(bonusChance)) {
                result++;
            }
        }

        return result;
    }

    /**
     * 採掘用Fortuneを取得する。
     */
    public double getMiningFortune(Player player, Material material) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");

        double fortune = statsManager.getExtraStat(player, StatsType.MINING_FORTUNE);

        if (isOre(material)) {
            fortune += statsManager.getExtraStat(player, StatsType.ORE_FORTUNE);
        }

        if (isBlockFortuneTarget(material)) {
            fortune += statsManager.getExtraStat(player, StatsType.BLOCK_FORTUNE);
        }

        if (isGemstone(material)) {
            fortune += statsManager.getExtraStat(player, StatsType.GEMSTONE_FORTUNE);
        }

        if (isDwarvenMetal(material)) {
            fortune += statsManager.getExtraStat(player, StatsType.DWARVEN_METAL_FORTUNE);
        }

        return Math.max(0.0, fortune);
    }

    /**
     * 農業用Fortuneを取得する。
     *
     * farming_fortune は全体に加算。
     * 作物ごとのFortuneも追加で加算。
     */
    public double getFarmingFortune(Player player, Material material) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");

        double fortune = statsManager.getExtraStat(player, StatsType.FARMING_FORTUNE);

        fortune += switch (material) {
            case WHEAT, WHEAT_SEEDS -> statsManager.getExtraStat(player, StatsType.WHEAT_FORTUNE);
            case CARROT, CARROTS -> statsManager.getExtraStat(player, StatsType.CARROT_FORTUNE);
            case POTATO, POTATOES -> statsManager.getExtraStat(player, StatsType.POTATO_FORTUNE);
            case PUMPKIN, PUMPKIN_SEEDS -> statsManager.getExtraStat(player, StatsType.PUMPKIN_FORTUNE);
            case SUGAR_CANE -> statsManager.getExtraStat(player, StatsType.SUGAR_CANE_FORTUNE);
            case BAMBOO, BAMBOO_SAPLING -> statsManager.getExtraStat(player, StatsType.BAMBOO_FORTUNE);
            case MELON, MELON_SLICE, MELON_SEEDS -> statsManager.getExtraStat(player, StatsType.MELON_SLICE_FORTUNE);
            case CACTUS -> statsManager.getExtraStat(player, StatsType.CACTUS_FORTUNE);
            case COCOA, COCOA_BEANS -> statsManager.getExtraStat(player, StatsType.COCOA_BEANS_FORTUNE);
            case BROWN_MUSHROOM, RED_MUSHROOM, MUSHROOM_STEM -> statsManager.getExtraStat(player, StatsType.MUSHROOM_FORTUNE);
            case NETHER_WART -> statsManager.getExtraStat(player, StatsType.NETHER_WART_FORTUNE);
            default -> 0.0;
        };

        return Math.max(0.0, fortune);
    }

    /**
     * 伐採用Fortuneを取得する。
     */
    public double getForagingFortune(Player player, Material material) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");

        if (!isLog(material)) {
            return 0.0;
        }

        return Math.max(0.0, statsManager.getExtraStat(player, StatsType.FORAGING_FORTUNE));
    }

    private boolean rollChance(double chancePercent) {
        double clampedChance = Math.max(0.0, Math.min(chancePercent, 100.0));
        double roll = ThreadLocalRandom.current().nextDouble(100.0);

        return roll < clampedChance;
    }

    private boolean isOre(Material material) {
        return switch (material) {
            case COAL_ORE,
                 DEEPSLATE_COAL_ORE,
                 IRON_ORE,
                 DEEPSLATE_IRON_ORE,
                 COPPER_ORE,
                 DEEPSLATE_COPPER_ORE,
                 GOLD_ORE,
                 DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE,
                 DEEPSLATE_REDSTONE_ORE,
                 EMERALD_ORE,
                 DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE,
                 DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE,
                 DEEPSLATE_DIAMOND_ORE,
                 NETHER_GOLD_ORE,
                 NETHER_QUARTZ_ORE -> true;
            default -> false;
        };
    }

    private boolean isBlockFortuneTarget(Material material) {
        return switch (material) {
            case STONE,
                 COBBLESTONE,
                 DEEPSLATE,
                 COBBLED_DEEPSLATE,
                 ANDESITE,
                 DIORITE,
                 GRANITE,
                 TUFF,
                 CALCITE,
                 BASALT,
                 BLACKSTONE,
                 NETHERRACK,
                 END_STONE -> true;
            default -> false;
        };
    }

    private boolean isGemstone(Material material) {
        return switch (material) {
            case AMETHYST_BLOCK,
                 BUDDING_AMETHYST,
                 AMETHYST_CLUSTER,
                 SMALL_AMETHYST_BUD,
                 MEDIUM_AMETHYST_BUD,
                 LARGE_AMETHYST_BUD -> true;
            default -> false;
        };
    }

    private boolean isDwarvenMetal(Material material) {
        return switch (material) {
            case IRON_ORE,
                 DEEPSLATE_IRON_ORE,
                 GOLD_ORE,
                 DEEPSLATE_GOLD_ORE,
                 COPPER_ORE,
                 DEEPSLATE_COPPER_ORE,
                 RAW_IRON_BLOCK,
                 RAW_GOLD_BLOCK,
                 RAW_COPPER_BLOCK -> true;
            default -> false;
        };
    }

    private boolean isLog(Material material) {
        return switch (material) {
            case OAK_LOG,
                 SPRUCE_LOG,
                 BIRCH_LOG,
                 JUNGLE_LOG,
                 ACACIA_LOG,
                 DARK_OAK_LOG,
                 MANGROVE_LOG,
                 CHERRY_LOG,
                 CRIMSON_STEM,
                 WARPED_STEM,
                 STRIPPED_OAK_LOG,
                 STRIPPED_SPRUCE_LOG,
                 STRIPPED_BIRCH_LOG,
                 STRIPPED_JUNGLE_LOG,
                 STRIPPED_ACACIA_LOG,
                 STRIPPED_DARK_OAK_LOG,
                 STRIPPED_MANGROVE_LOG,
                 STRIPPED_CHERRY_LOG,
                 STRIPPED_CRIMSON_STEM,
                 STRIPPED_WARPED_STEM -> true;
            default -> false;
        };
    }
}