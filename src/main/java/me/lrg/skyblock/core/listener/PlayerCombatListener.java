package me.lrg.skyblock.core.listener;

import me.lrg.skyblock.core.manager.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Objects;

/**
 * プレイヤーの戦闘処理を扱うListener。
 *
 * このクラスの役割:
 * - プレイヤーの攻撃時にStrengthをダメージへ反映する
 *
 * 注意:
 * - SQLは書かない
 * - StatsDataの取得や計算はStatsManagerに任せる
 */
public class PlayerCombatListener implements Listener {

    private final StatsManager statsManager;

    public PlayerCombatListener(StatsManager statsManager) {
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        double baseDamage = event.getDamage();
        double finalDamage = statsManager.calculateStrengthDamage(attacker, baseDamage);

        event.setDamage(finalDamage);
    }
}