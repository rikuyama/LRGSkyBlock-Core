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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double internalDamage = Math.max(0.0, event.getFinalDamage());
        if (internalDamage <= 0.0) {
            return;
        }

        double visualScale = statsManager.getVisualHealthScale(player);
        event.setDamage(Math.max(0.0, event.getDamage() * visualScale));

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }

            statsManager.damage(player, internalDamage);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double internalHealing = Math.max(0.0, event.getAmount());
        if (internalHealing <= 0.0) {
            return;
        }

        double visualScale = statsManager.getVisualHealthScale(player);
        event.setAmount(Math.max(0.0, internalHealing * visualScale));

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }

            statsManager.heal(player, internalHealing);
        });
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
}
