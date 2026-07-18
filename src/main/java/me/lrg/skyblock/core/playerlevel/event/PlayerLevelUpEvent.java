package me.lrg.skyblock.core.playerlevel.event;

import me.lrg.skyblock.core.playerlevel.api.PlayerLevelXpReason;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class PlayerLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    private final PlayerLevelXpReason reason;

    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel, PlayerLevelXpReason reason) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Player getPlayer() { return player; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
    public int getLevelsGained() { return newLevel - oldLevel; }
    public PlayerLevelXpReason getReason() { return reason; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
