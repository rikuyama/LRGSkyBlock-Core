package me.lrg.skyblock.core.autopickup;

import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Objects;

public final class AutoPickupListener implements Listener {
    private final PlayerLevelUnlockManager unlockManager;
    private final InventoryDelivery delivery;

    public AutoPickupListener(PlayerLevelUnlockManager unlockManager, InventoryDelivery delivery) {
        this.unlockManager = Objects.requireNonNull(unlockManager, "unlockManager");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!unlockManager.isUnlocked(event.getPlayer().getUniqueId(), PlayerLevelUnlockManager.AUTO_PICKUP)) return;

        if (event.isDropItems()) {
            var drops = event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand(), event.getPlayer());
            event.setDropItems(false);
            delivery.deliver(event.getPlayer(), drops, event.getBlock().getLocation());
        } else {
            // Fortune listeners run before this listener and may already have spawned their calculated drops.
            var spawnedDrops = event.getBlock().getWorld()
                    .getNearbyEntitiesByType(Item.class, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 1.5)
                    .stream()
                    .filter(item -> item.getTicksLived() <= 1)
                    .toList();
            if (!spawnedDrops.isEmpty()) {
                var stacks = spawnedDrops.stream().map(Item::getItemStack).toList();
                spawnedDrops.forEach(Item::remove);
                delivery.deliver(event.getPlayer(), stacks, event.getBlock().getLocation());
            }
        }

        int experience = event.getExpToDrop();
        if (experience > 0) {
            event.setExpToDrop(0);
            event.getPlayer().giveExp(experience);
        }
    }
}
