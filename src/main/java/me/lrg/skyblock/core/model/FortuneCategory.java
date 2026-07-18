package me.lrg.skyblock.core.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public enum FortuneCategory {
    MINING(Material.DIAMOND_ORE, "採掘"),
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
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ORE", "BLOCK", "GEMSTONE", "DWARVEN_METAL", "MINING" -> Optional.of(MINING);
            case "FARMING" -> Optional.of(FARMING);
            case "FORAGING", "WOOD" -> Optional.of(FORAGING);
            default -> Optional.empty();
        };
    }
}
