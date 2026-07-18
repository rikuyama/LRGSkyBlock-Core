package me.lrg.skyblock.core.config;

import me.lrg.skyblock.core.model.FortuneCategory;
import me.lrg.skyblock.core.model.FortuneCategoryResolver;
import me.lrg.skyblock.core.model.FortuneTargetRule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public final class FortuneTargetSettings {
    private static final String FILE_NAME = "fortune-targets.yml";

    private final JavaPlugin plugin;
    private final File file;
    private final Map<FortuneCategory, Boolean> categoryEnabled = new EnumMap<>(FortuneCategory.class);
    private final Map<Material, FortuneCategory> categoryOverrides = new EnumMap<>(Material.class);
    private final Map<Material, Boolean> enabledOverrides = new EnumMap<>(Material.class);
    private double maximumFortune;

    private FortuneTargetSettings(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public static FortuneTargetSettings load(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) plugin.saveResource(FILE_NAME, false);

        FortuneTargetSettings settings = new FortuneTargetSettings(plugin, file);
        settings.reload();
        return settings;
    }

    public synchronized void reload() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        categoryEnabled.clear();
        categoryOverrides.clear();
        enabledOverrides.clear();

        for (FortuneCategory category : FortuneCategory.values()) {
            categoryEnabled.put(category, config.getBoolean("categories." + category.name() + ".enabled", true));
        }

        ConfigurationSection overrides = config.getConfigurationSection("overrides");
        if (overrides != null) {
            for (String materialName : overrides.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isBlock()) {
                    plugin.getLogger().warning("無効なFortune例外Materialを無視しました: " + materialName);
                    continue;
                }
                FortuneCategory.fromString(config.getString("overrides." + materialName + ".category"))
                        .ifPresent(category -> categoryOverrides.put(material, category));
                if (config.contains("overrides." + materialName + ".enabled")) {
                    enabledOverrides.put(material, config.getBoolean("overrides." + materialName + ".enabled"));
                }
            }
        }

        double configuredMaximum = config.getDouble("settings.maximum-fortune", 10000.0);
        maximumFortune = configuredMaximum <= 0.0 ? Double.MAX_VALUE : configuredMaximum;
        plugin.getLogger().info("Fortuneカテゴリ " + categoryEnabled.size() + " 件、例外 " + categoryOverrides.size() + " 件を読み込みました。");
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.maximum-fortune", maximumFortune == Double.MAX_VALUE ? 0.0 : maximumFortune);
        for (FortuneCategory category : FortuneCategory.values()) {
            config.set("categories." + category.name() + ".enabled", isCategoryEnabled(category));
        }
        for (Material material : categoryOverrides.keySet()) {
            config.set("overrides." + material.name() + ".category", categoryOverrides.get(material).name());
            if (enabledOverrides.containsKey(material)) {
                config.set("overrides." + material.name() + ".enabled", enabledOverrides.get(material));
            }
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "fortune-targets.yml の保存に失敗しました。", exception);
        }
    }

    public synchronized Optional<FortuneCategory> resolveCategory(Material material) {
        Objects.requireNonNull(material, "material");
        FortuneCategory override = categoryOverrides.get(material);
        return override != null ? Optional.of(override) : FortuneCategoryResolver.resolve(material);
    }

    public synchronized Optional<FortuneTargetRule> findRule(Material material) {
        return resolveCategory(material).map(category -> new FortuneTargetRule(
                material, category, 1, isMaterialEnabled(material, category), true, true, true, List.of(), ""
        ));
    }

    public synchronized boolean isMaterialEnabled(Material material, FortuneCategory category) {
        return enabledOverrides.getOrDefault(material, isCategoryEnabled(category));
    }

    public synchronized boolean isCategoryEnabled(FortuneCategory category) {
        return categoryEnabled.getOrDefault(category, true);
    }

    public synchronized void setCategoryEnabled(FortuneCategory category, boolean enabled) {
        categoryEnabled.put(category, enabled);
        save();
    }

    public synchronized void setCategoryOverride(Material material, FortuneCategory category) {
        if (material == null || !material.isBlock()) return;
        categoryOverrides.put(material, category);
        save();
    }

    public synchronized void removeOverride(Material material) {
        categoryOverrides.remove(material);
        enabledOverrides.remove(material);
        save();
    }

    public synchronized Optional<FortuneCategory> getCategoryOverride(Material material) {
        return Optional.ofNullable(categoryOverrides.get(material));
    }

    public boolean isFortuneTarget(Material material) {
        return findRule(material).map(FortuneTargetRule::isFortuneEnabled).orElse(false);
    }

    public boolean acceptsFortuneDrop(Material blockMaterial, Material dropMaterial) {
        return isFortuneTarget(blockMaterial) && dropMaterial != null && dropMaterial != Material.AIR;
    }

    public double clampFortune(double fortune) { return Math.max(0.0, Math.min(fortune, maximumFortune)); }
    public double getMaximumFortune() { return maximumFortune; }
    public Collection<FortuneTargetRule> getRules() {
        return categoryOverrides.keySet().stream().map(this::findRule).flatMap(Optional::stream).toList();
    }
}
