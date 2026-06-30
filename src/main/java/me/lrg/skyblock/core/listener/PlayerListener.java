package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.PlayerManager;
import me.lrg.skyblock.core.manager.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * プレイヤー参加・退出イベントを受け取るListener。
 *
 * このクラスの役割:
 * - PlayerJoinEvent を受け取る
 * - PlayerQuitEvent を受け取る
 * - 実際の処理は各Managerに渡す
 *
 * 注意:
 * - SQLは書かない
 * - DB処理は書かない
 * - 複雑なゲームロジックは書かない
 */
public class PlayerListener implements Listener {

    private final PlayerManager playerManager;
    private final StatsManager statsManager;

    public PlayerListener(PlayerManager playerManager, StatsManager statsManager) {
        this.playerManager = Objects.requireNonNull(playerManager, "playerManager");
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        playerManager.loadPlayer(player);
        statsManager.loadStats(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        playerManager.saveAndRemovePlayer(uuid);
        statsManager.saveAndRemoveStats(uuid);
    }
}