package me.lrg.skyblock.core.playerlevel.listener;

import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.model.BaseStatsType;
import me.lrg.skyblock.core.model.StatsLayer;
import me.lrg.skyblock.core.model.StatsModifierData;
import me.lrg.skyblock.core.playerlevel.event.PlayerLevelLoadedEvent;
import me.lrg.skyblock.core.playerlevel.event.PlayerLevelUpEvent;
import me.lrg.skyblock.core.playerlevel.formula.PlayerLevelStatFormula;
import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public final class PlayerLevelStatsListener implements Listener {
    private final PlayerLevelManager levelManager;
    private final StatsManager statsManager;

    public PlayerLevelStatsListener(PlayerLevelManager levelManager, StatsManager statsManager) {
        this.levelManager = Objects.requireNonNull(levelManager, "levelManager");
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @EventHandler
    public void onLoaded(PlayerLevelLoadedEvent event) {
        apply(event.getPlayer(), levelManager.getLevel(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        apply(event.getPlayer(), event.getNewLevel());
    }

    private void apply(Player player, int level) {
        StatsModifierData modifier = new StatsModifierData();
        modifier.setBaseStat(BaseStatsType.HEALTH, PlayerLevelStatFormula.healthBonus(level));
        modifier.setBaseStat(BaseStatsType.STRENGTH, PlayerLevelStatFormula.strengthBonus(level));
        statsManager.replaceStatsLayer(player.getUniqueId(), StatsLayer.PLAYER_LEVEL, modifier);
    }
}
