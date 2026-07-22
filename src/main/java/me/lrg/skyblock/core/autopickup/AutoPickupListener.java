package me.lrg.skyblock.core.autopickup;

import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Fortuneなど専用リスナーで処理されない通常ドロップをAuto Pickupへ流す。
 */
public final class AutoPickupListener implements Listener {
    private final AutoPickupManager autoPickupManager;

    public AutoPickupListener(AutoPickupManager autoPickupManager) {
        this.autoPickupManager = Objects.requireNonNull(autoPickupManager, "autoPickupManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isDropEligible(player)) {
            return;
        }
        if (!autoPickupManager.isUnlocked(player)) {
            return;
        }

        // Fortune系リスナーがsetDropItems(false)にした場合は、そのリスナー側で直接配送済み。
        if (event.isDropItems()) {
            Collection<ItemStack> drops = event.getBlock().getDrops(
                    player.getInventory().getItemInMainHand(),
                    player
            );
            event.setDropItems(false);
            autoPickupManager.collect(player, drops, event.getBlock().getLocation());
        }

        int experience = event.getExpToDrop();
        if (autoPickupManager.collectExperience(player, experience)) {
            event.setExpToDrop(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null || !isDropEligible(killer) || !autoPickupManager.isUnlocked(killer)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        autoPickupManager.collect(killer, drops, event.getEntity().getLocation());

        int experience = event.getDroppedExp();
        if (autoPickupManager.collectExperience(killer, experience)) {
            event.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        if (!isDropEligible(player)) {
            return;
        }
        if (!autoPickupManager.isUnlocked(player)) {
            return;
        }
        if (!(event.getCaught() instanceof Item item)) {
            return;
        }

        ItemStack stack = item.getItemStack().clone();
        item.remove();
        autoPickupManager.collect(player, List.of(stack), player.getLocation());

        int experience = event.getExpToDrop();
        if (autoPickupManager.collectExperience(player, experience)) {
            event.setExpToDrop(0);
        }
    }

    private boolean isDropEligible(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR;
    }
}
