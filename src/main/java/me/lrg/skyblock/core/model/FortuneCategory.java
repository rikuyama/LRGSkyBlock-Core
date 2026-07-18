package me.lrg.skyblock.core.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public enum FortuneCategory {
    ORE(Material.DIAMOND_ORE, "鉱石"),
    BLOCK(Material.STONE, "石・通常ブロック"),
    GEMSTONE(Material.AMETHYST_CLUSTER, "宝石"),
    DWARVEN_METAL(Material.RAW_IRON_BLOCK, "金属"),
    FARMING(Material.WHEAT, "農業"),
    FORAGING(Material.OAK_LOG, "伐採");

    private final Material icon;
    private final String displayName;

    FortuneCategory(Material icon, String displayName) {
        this.icon = icon;
        this.displayName = displayName;
    }

    public Material getIcon() { return icon; }
    public String getDisplayName() { return displayName; }

    public static Optional<FortuneCategory> fromString(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
