package me.lrg.skyblock.core.rank;

import me.lrg.skyblock.core.playerlevel.event.PlayerLevelLoadedEvent;
import me.lrg.skyblock.core.playerlevel.event.PlayerLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public final class RankNameTagListener implements Listener {
    private final RankNameTagManager manager;

    public RankNameTagListener(RankNameTagManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.update(event.getPlayer());
    }

    @EventHandler
    public void onLevelLoaded(PlayerLevelLoadedEvent event) {
        manager.update(event.getPlayer());
    }

    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        manager.update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.remove(event.getPlayer());
    }
}
