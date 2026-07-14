package me.lrg.skyblock.core.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * Fortune系Listenerで共通利用する道具処理。
 */
public final class FortuneToolUtil {

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
        if (tool == null || tool.getType() == Material.AIR) {
            return 0;
        }

        return Math.max(0, tool.getEnchantmentLevel(Enchantment.FORTUNE));
    }

    /**
     * Hypixel風に、幸運エンチャントをMining Fortune値へ変換する。
     * I=10, II=20, III=30, IV=45, V以降は1レベルごとに+15。
     */
    public static double getMiningFortuneFromEnchant(ItemStack tool) {
        int level = getVanillaFortuneLevel(tool);

        return switch (level) {
            case 0 -> 0.0;
            case 1 -> 10.0;
            case 2 -> 20.0;
            case 3 -> 30.0;
            case 4 -> 45.0;
            default -> 45.0 + ((level - 4) * 15.0);
        };
    }
}
