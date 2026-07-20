package me.lrg.skyblock.core.bazaar.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public final class BazaarItemCodec {
    private BazaarItemCodec() {}

    public static String encode(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack decode(String value) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(value));
    }
}
