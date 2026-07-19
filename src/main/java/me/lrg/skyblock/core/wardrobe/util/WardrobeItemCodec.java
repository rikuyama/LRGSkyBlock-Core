package me.lrg.skyblock.core.wardrobe.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public final class WardrobeItemCodec {
    private WardrobeItemCodec() {
    }

    public static String encode(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(value));
    }
}
