package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HealthListener implements Listener {

    private final JavaPlugin plugin;
    private final StatsManager statsManager;

    public HealthListener(JavaPlugin plugin, StatsManager statsManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        syncNextTick(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        syncNextTick(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        statsManager.markDead(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            statsManager.resetCurrentHealth(player);
            statsManager.applyStatsToPlayer(player);
        });
    }

    private void syncNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            statsManager.syncCurrentHealthFromBukkit(player);
        });
    }
}
