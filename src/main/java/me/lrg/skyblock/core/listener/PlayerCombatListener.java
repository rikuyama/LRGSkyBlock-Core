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
 * - 攻撃者のStrength / Critical Chance / Crit Damageをダメージへ反映する
 * - 防御者のDefenseを被ダメージ軽減へ反映する
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
        double damage = event.getDamage();

        if (event.getDamager() instanceof Player attacker) {
            damage = statsManager.calculateAttackDamage(attacker, damage);
        }

        if (event.getEntity() instanceof Player defender) {
            damage = statsManager.calculateDefenseDamage(defender, damage);
        }

        event.setDamage(damage);
    }
}