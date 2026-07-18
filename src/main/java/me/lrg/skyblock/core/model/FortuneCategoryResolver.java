package me.lrg.skyblock.core.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public final class FortuneCategoryResolver {
    private FortuneCategoryResolver() {}

    public static Optional<FortuneCategory> resolve(Material material) {
        if (material == null || !material.isBlock()) return Optional.empty();
        String name = material.name().toUpperCase(Locale.ROOT);

        if (isOre(name)) return Optional.of(FortuneCategory.ORE);
        if (isGemstone(name)) return Optional.of(FortuneCategory.GEMSTONE);
        if (isDwarvenMetal(name)) return Optional.of(FortuneCategory.DWARVEN_METAL);
        if (isFarming(name)) return Optional.of(FortuneCategory.FARMING);
        if (isForaging(name)) return Optional.of(FortuneCategory.FORAGING);
        if (isMiningBlock(name)) return Optional.of(FortuneCategory.BLOCK);
        return Optional.empty();
    }

    private static boolean isOre(String name) {
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }

    private static boolean isGemstone(String name) {
        return name.contains("AMETHYST") && !name.equals("BUDDING_AMETHYST");
    }

    private static boolean isDwarvenMetal(String name) {
        return name.equals("RAW_IRON_BLOCK") || name.equals("RAW_GOLD_BLOCK") || name.equals("RAW_COPPER_BLOCK");
    }

    private static boolean isFarming(String name) {
        return switch (name) {
            case "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "PUMPKIN", "MELON",
                 "SUGAR_CANE", "BAMBOO", "BAMBOO_SAPLING", "CACTUS", "COCOA",
                 "NETHER_WART", "BROWN_MUSHROOM", "RED_MUSHROOM", "MUSHROOM_STEM" -> true;
            default -> false;
        };
    }

    private static boolean isForaging(String name) {
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    private static boolean isMiningBlock(String name) {
        return switch (name) {
            case "STONE", "COBBLESTONE", "DEEPSLATE", "COBBLED_DEEPSLATE", "ANDESITE", "DIORITE",
                 "GRANITE", "TUFF", "CALCITE", "BASALT", "SMOOTH_BASALT", "BLACKSTONE",
                 "NETHERRACK", "END_STONE" -> true;
            default -> false;
        };
    }
}
