package me.lrg.skyblock.core.playerlevel.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class PlayerLevelLoadedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public PlayerLevelLoadedEvent(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
