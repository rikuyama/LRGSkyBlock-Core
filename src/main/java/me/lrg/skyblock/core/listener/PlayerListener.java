package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;

    public PlayerListener(PlayerManager playerManager) {

        this.playerManager = playerManager;

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        playerManager.addPlayer(event.getPlayer());

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        playerManager.removePlayer(event.getPlayer());

    }

}