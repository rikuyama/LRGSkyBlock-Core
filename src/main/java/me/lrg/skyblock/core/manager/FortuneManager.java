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
        if (baseAmount <= 0) return 0;
        double safeFortune = targetSettings.clampFortune(fortune);
        int guaranteedExtraDrops = (int) Math.floor(safeFortune / 100.0);
        double bonusChance = safeFortune % 100.0;
        int result = baseAmount * (1 + guaranteedExtraDrops);
        for (int i = 0; i < baseAmount; i++) {
            if (ThreadLocalRandom.current().nextDouble(100.0) < bonusChance) result++;
        }
        return result;
    }

    public double getFortune(Player player, Material material) {
        Optional<FortuneTargetRule> ruleOptional = enabledRule(material);
        if (ruleOptional.isEmpty()) return 0.0;
        StatsType statType = switch (ruleOptional.get().getCategory()) {
            case MINING -> StatsType.MINING_FORTUNE;
            case FARMING -> StatsType.FARMING_FORTUNE;
            case FORAGING -> StatsType.FORAGING_FORTUNE;
        };
        return targetSettings.clampFortune(statsManager.getExtraStat(player, statType));
    }

    public double getMiningFortune(Player player, Material material) {
        return isCategory(material, FortuneCategory.MINING) ? getFortune(player, material) : 0.0;
    }

    public double getFarmingFortune(Player player, Material material) {
        return isCategory(material, FortuneCategory.FARMING) ? getFortune(player, material) : 0.0;
    }

    public double getForagingFortune(Player player, Material material) {
        return isCategory(material, FortuneCategory.FORAGING) ? getFortune(player, material) : 0.0;
    }

    private boolean isCategory(Material material, FortuneCategory expected) {
        return enabledRule(material).map(FortuneTargetRule::getCategory).filter(expected::equals).isPresent();
    }

    private Optional<FortuneTargetRule> enabledRule(Material material) {
        return targetSettings.findRule(material).filter(FortuneTargetRule::isFortuneEnabled);
    }

    public Optional<FortuneTargetRule> getTargetRule(Material material) { return targetSettings.findRule(material); }
    public boolean isFortuneTarget(Material material) { return targetSettings.isFortuneTarget(material); }
    public boolean isMiningTarget(Material material) { return isCategory(material, FortuneCategory.MINING); }
    public boolean isFarmingTarget(Material material) { return isCategory(material, FortuneCategory.FARMING); }
    public boolean isForagingTarget(Material material) { return isCategory(material, FortuneCategory.FORAGING); }
    public boolean shouldApplyFortune(Material blockMaterial, Material dropMaterial) {
        return targetSettings.acceptsFortuneDrop(blockMaterial, dropMaterial);
    }
}
