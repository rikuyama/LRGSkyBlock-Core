package me.lrg.skyblock.core.config;

import me.lrg.skyblock.core.model.FortuneCategory;
import me.lrg.skyblock.core.model.FortuneTargetRule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class FortuneTargetSettings {

    private static final String FILE_NAME = "fortune-targets.yml";

    private final Map<Material, FortuneTargetRule> rules;
    private final double maximumFortune;

    private FortuneTargetSettings(Map<Material, FortuneTargetRule> rules, double maximumFortune) {
        this.rules = Map.copyOf(rules);
        this.maximumFortune = maximumFortune;
    }

    public static FortuneTargetSettings load(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        File file = new File(plugin.getDataFolder(), FILE_NAME);

        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();
        Map<Material, FortuneTargetRule> rules = new EnumMap<>(Material.class);

        ConfigurationSection targetsSection = config.getConfigurationSection("targets");

        if (targetsSection != null) {
            for (String blockName : targetsSection.getKeys(false)) {
                loadRule(config, targetsSection, blockName, rules, logger);
            }
        }

        double configuredMaximum = config.getDouble("settings.maximum-fortune", 10000.0);
        double maximumFortune = configuredMaximum <= 0.0 ? Double.MAX_VALUE : configuredMaximum;

        logger.info("Fortune対象設定を " + rules.size() + " 件読み込みました。");
        return new FortuneTargetSettings(rules, maximumFortune);
    }

    private static void loadRule(
            YamlConfiguration config,
            ConfigurationSection targetsSection,
            String blockName,
            Map<Material, FortuneTargetRule> rules,
            Logger logger
    ) {
        Material blockMaterial = Material.matchMaterial(blockName);

        if (blockMaterial == null || !blockMaterial.isBlock()) {
            logger.warning("fortune-targets.yml の無効なブロックを無視しました: " + blockName);
            return;
        }

        String path = targetsSection.getCurrentPath() + "." + blockName + ".";
        Optional<FortuneCategory> categoryOptional = FortuneCategory.fromString(
                config.getString(path + "category")
        );

        if (categoryOptional.isEmpty()) {
            logger.warning("Fortuneカテゴリが不正なため無視しました: " + blockName);
            return;
        }

        List<Material> dropMaterials = new ArrayList<>();

        for (String dropName : config.getStringList(path + "fortune-drop-materials")) {
            Material dropMaterial = Material.matchMaterial(dropName);

            if (dropMaterial == null || dropMaterial == Material.AIR) {
                logger.warning("無効なFortuneドロップMaterialを無視しました: " + dropName);
                continue;
            }

            dropMaterials.add(dropMaterial);
        }

        FortuneTargetRule rule = new FortuneTargetRule(
                blockMaterial,
                categoryOptional.get(),
                config.getInt(path + "base-drop-amount", 1),
                config.getBoolean(path + "fortune-enabled", true),
                config.getBoolean(path + "silk-touch-enabled", true),
                config.getBoolean(path + "collection-enabled", true),
                config.getBoolean(path + "skill-experience-enabled", true),
                dropMaterials,
                config.getString(path + "custom-drop-id", "")
        );

        rules.put(blockMaterial, rule);
    }

    public Optional<FortuneTargetRule> findRule(Material material) {
        Objects.requireNonNull(material, "material");
        return Optional.ofNullable(rules.get(material));
    }

    public boolean isFortuneTarget(Material material) {
        return findRule(material)
                .map(FortuneTargetRule::isFortuneEnabled)
                .orElse(false);
    }

    public boolean acceptsFortuneDrop(Material blockMaterial, Material dropMaterial) {
        return findRule(blockMaterial)
                .filter(FortuneTargetRule::isFortuneEnabled)
                .map(rule -> rule.acceptsDrop(dropMaterial))
                .orElse(false);
    }

    public double clampFortune(double fortune) {
        return Math.max(0.0, Math.min(fortune, maximumFortune));
    }

    public double getMaximumFortune() {
        return maximumFortune;
    }

    public Collection<FortuneTargetRule> getRules() {
        return rules.values();
    }
}
