package me.lrg.skyblock.core.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public final class FortuneCategoryResolver {
    private FortuneCategoryResolver() {}

    public static Optional<FortuneCategory> resolve(Material material) {
        if (material == null || !material.isBlock()) return Optional.empty();
        String name = material.name().toUpperCase(Locale.ROOT);
        if (isFarming(name)) return Optional.of(FortuneCategory.FARMING);
        if (isForaging(name)) return Optional.of(FortuneCategory.FORAGING);
        if (isMining(name)) return Optional.of(FortuneCategory.MINING);
        return Optional.empty();
    }

    private static boolean isFarming(String name) {
        return switch (name) {
            case "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "PUMPKIN", "MELON",
                 "SUGAR_CANE", "BAMBOO", "BAMBOO_SAPLING", "CACTUS", "COCOA",
                 "NETHER_WART", "BROWN_MUSHROOM", "RED_MUSHROOM", "MUSHROOM_STEM",
                 "CRIMSON_FUNGUS", "WARPED_FUNGUS", "TORCHFLOWER_CROP", "PITCHER_CROP",
                 "SWEET_BERRY_BUSH", "KELP", "KELP_PLANT", "CAVE_VINES", "CAVE_VINES_PLANT",
                 "CHORUS_FLOWER", "CHORUS_PLANT" -> true;
            default -> false;
        };
    }

    private static boolean isForaging(String name) {
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    private static boolean isMining(String name) {
        if (name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS")) return true;
        if (name.contains("AMETHYST") && !name.equals("BUDDING_AMETHYST")) return true;
        return switch (name) {
            case "RAW_IRON_BLOCK", "RAW_GOLD_BLOCK", "RAW_COPPER_BLOCK",
                 "STONE", "COBBLESTONE", "DEEPSLATE", "COBBLED_DEEPSLATE", "ANDESITE", "DIORITE",
                 "GRANITE", "TUFF", "CALCITE", "BASALT", "SMOOTH_BASALT", "BLACKSTONE",
                 "NETHERRACK", "END_STONE" -> true;
            default -> false;
        };
    }
}
