package me.lrg.skyblock.core.wardrobe.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class WardrobeSet {
    private final ItemStack[] armor;

    public WardrobeSet(ItemStack[] armor) {
        if (armor == null || armor.length != 4) {
            throw new IllegalArgumentException("armor must contain exactly 4 items");
        }
        this.armor = cloneArmor(armor);
    }

    public static WardrobeSet empty() {
        return new WardrobeSet(new ItemStack[4]);
    }

    public ItemStack[] getArmor() {
        return cloneArmor(armor);
    }

    public boolean isEmpty() {
        return Arrays.stream(armor).allMatch(WardrobeSet::isAir);
    }

    public int itemCount() {
        return (int) Arrays.stream(armor).filter(item -> !isAir(item)).count();
    }

    private static ItemStack[] cloneArmor(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) {
            copy[index] = source[index] == null ? null : source[index].clone();
        }
        return copy;
    }

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}
