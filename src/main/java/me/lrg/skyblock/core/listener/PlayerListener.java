package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

/**
 * プレイヤー参加・退出イベントを受け取るListener。
 */
public class PlayerListener implements Listener {

    private final PlayerManager playerManager;

    public PlayerListener(PlayerManager playerManager) {
        this.playerManager = Objects.requireNonNull(playerManager, "playerManager");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("§a[LRG] PlayerJoinEvent は動いています。");
        playerManager.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerManager.saveAndRemovePlayer(event.getPlayer().getUniqueId());
    }
}