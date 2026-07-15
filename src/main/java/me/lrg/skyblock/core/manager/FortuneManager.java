package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.config.FortuneTargetSettings;
import me.lrg.skyblock.core.model.FortuneCategory;
import me.lrg.skyblock.core.model.FortuneTargetRule;
import me.lrg.skyblock.core.model.StatsType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class FortuneManager {

    private final StatsManager statsManager;
    private final FortuneTargetSettings targetSettings;

    public FortuneManager(StatsManager statsManager, FortuneTargetSettings targetSettings) {
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
        this.targetSettings = Objects.requireNonNull(targetSettings, "targetSettings");
    }

    public int calculateDropAmount(int baseAmount, double fortune) {
        if (baseAmount <= 0) {
            return 0;
        }

        double safeFortune = targetSettings.clampFortune(fortune);
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

    public double getMiningFortune(Player player, Material material) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");

        Optional<FortuneTargetRule> ruleOptional = targetSettings.findRule(material);

        if (ruleOptional.isEmpty() || !ruleOptional.get().isFortuneEnabled()) {
            return 0.0;
        }

        FortuneCategory category = ruleOptional.get().getCategory();

        if (category != FortuneCategory.ORE
                && category != FortuneCategory.BLOCK
                && category != FortuneCategory.GEMSTONE
                && category != FortuneCategory.DWARVEN_METAL) {
            return 0.0;
        }

        double fortune = statsManager.getExtraStat(player, StatsType.MINING_FORTUNE);

        fortune += switch (category) {
            case ORE -> statsManager.getExtraStat(player, StatsType.ORE_FORTUNE);
            case BLOCK -> statsManager.getExtraStat(player, StatsType.BLOCK_FORTUNE);
            case GEMSTONE -> statsManager.getExtraStat(player, StatsType.GEMSTONE_FORTUNE);
            case DWARVEN_METAL -> statsManager.getExtraStat(player, StatsType.DWARVEN_METAL_FORTUNE);
            default -> 0.0;
        };

        return targetSettings.clampFortune(fortune);
    }

    public double getFarmingFortune(Player player, Material material) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");

        Optional<FortuneTargetRule> ruleOptional = targetSettings.findRule(material);

        if (ruleOptional.isEmpty()
                || !ruleOptional.get().isFortuneEnabled()
                || ruleOptional.get().getCategory() != FortuneCategory.FARMING) {
            return 0.0;
        }

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

        return targetSettings.clampFortune(fortune);
    }

    public double getForagingFortune(Player player, Material material) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");

        Optional<FortuneTargetRule> ruleOptional = targetSettings.findRule(material);

        if (ruleOptional.isEmpty()
                || !ruleOptional.get().isFortuneEnabled()
                || ruleOptional.get().getCategory() != FortuneCategory.FORAGING) {
            return 0.0;
        }

        return targetSettings.clampFortune(
                statsManager.getExtraStat(player, StatsType.FORAGING_FORTUNE)
        );
    }

    public Optional<FortuneTargetRule> getTargetRule(Material material) {
        return targetSettings.findRule(material);
    }

    public boolean isFortuneTarget(Material material) {
        return targetSettings.isFortuneTarget(material);
    }

    public boolean shouldApplyFortune(Material blockMaterial, Material dropMaterial) {
        return targetSettings.acceptsFortuneDrop(blockMaterial, dropMaterial);
    }

    private boolean rollChance(double chancePercent) {
        double clampedChance = Math.max(0.0, Math.min(chancePercent, 100.0));
        return ThreadLocalRandom.current().nextDouble(100.0) < clampedChance;
    }
}
