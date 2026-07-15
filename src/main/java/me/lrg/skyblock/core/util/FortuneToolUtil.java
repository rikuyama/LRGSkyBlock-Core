package me.lrg.skyblock.core.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Fortune系ListenerとStats表示で共通利用する道具処理。
 */
public final class FortuneToolUtil {

    private static final Map<Integer, Double> VANILLA_FORTUNE_TO_MINING_FORTUNE = Map.of(
            1, 10.0,
            2, 20.0,
            3, 30.0,
            4, 45.0
    );

    private static final double FORTUNE_AFTER_LEVEL_FOUR = 15.0;

    private FortuneToolUtil() {
    }

    public static ItemStack createWithoutVanillaFortune(ItemStack originalTool) {
        if (originalTool == null || originalTool.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }

        ItemStack copiedTool = originalTool.clone();
        copiedTool.removeEnchantment(Enchantment.FORTUNE);
        return copiedTool;
    }

    public static boolean hasVanillaFortune(ItemStack tool) {
        return getVanillaFortuneLevel(tool) > 0;
    }

    public static int getVanillaFortuneLevel(ItemStack tool) {
        if (!isMiningTool(tool)) {
            return 0;
        }

        if (hasSilkTouch(tool)) {
            return 0;
        }

        return Math.max(0, tool.getEnchantmentLevel(Enchantment.FORTUNE));
    }

    public static boolean hasSilkTouch(ItemStack tool) {
        return tool != null
                && tool.getType() != Material.AIR
                && tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
    }

    public static boolean isMiningTool(ItemStack tool) {
        if (tool == null || tool.getType() == Material.AIR) {
            return false;
        }

        return switch (tool.getType()) {
            case WOODEN_PICKAXE,
                 STONE_PICKAXE,
                 IRON_PICKAXE,
                 GOLDEN_PICKAXE,
                 DIAMOND_PICKAXE,
                 NETHERITE_PICKAXE -> true;
            default -> false;
        };
    }

    /**
     * Hypixel風に、幸運エンチャントをMining Fortune値へ変換する。
     *
     * I   = +10
     * II  = +20
     * III = +30
     * IV  = +45
     * V以降は1レベルごとに+15
     *
     * ツルハシ以外、またはSilk Touch付きの道具では0を返す。
     */
    public static double getMiningFortuneFromEnchant(ItemStack tool) {
        int level = getVanillaFortuneLevel(tool);

        if (level <= 0) {
            return 0.0;
        }

        Double configuredValue = VANILLA_FORTUNE_TO_MINING_FORTUNE.get(level);

        if (configuredValue != null) {
            return configuredValue;
        }

        double levelFourValue = VANILLA_FORTUNE_TO_MINING_FORTUNE.get(4);
        return levelFourValue + ((level - 4) * FORTUNE_AFTER_LEVEL_FOUR);
    }

    public static Map<Integer, Double> getVanillaFortuneConversionTable() {
        return VANILLA_FORTUNE_TO_MINING_FORTUNE;
    }
}
