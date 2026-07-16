package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ManaListener implements Listener {

    private final JavaPlugin plugin;
    private final StatsManager statsManager;

    public ManaListener(JavaPlugin plugin, StatsManager statsManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        statsManager.clearCurrentMana(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            statsManager.resetCurrentMana(player);
            statsManager.applyStatsToPlayer(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        statsManager.removeCurrentMana(event.getPlayer().getUniqueId());
    }
}
