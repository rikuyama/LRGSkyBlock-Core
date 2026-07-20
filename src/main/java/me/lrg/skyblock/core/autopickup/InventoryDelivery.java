package me.lrg.skyblock.core.autopickup;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

/** インベントリへ追加し、入り切らない分を足元へ落とす共通配送処理。 */
public final class InventoryDelivery {
    public void deliver(Player player, Collection<ItemStack> items, Location fallbackLocation) {
        for (ItemStack original : items) {
            if (original == null || original.getAmount() <= 0) continue;
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(original.clone());
            overflow.values().forEach(stack -> fallbackLocation.getWorld().dropItemNaturally(fallbackLocation, stack));
        }
    }
}
