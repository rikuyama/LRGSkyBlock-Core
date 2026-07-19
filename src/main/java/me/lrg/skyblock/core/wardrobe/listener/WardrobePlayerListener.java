package me.lrg.skyblock.core.wardrobe.listener;

import me.lrg.skyblock.core.wardrobe.manager.WardrobeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class WardrobePlayerListener implements Listener {
    private final WardrobeManager wardrobeManager;

    public WardrobePlayerListener(WardrobeManager wardrobeManager) {
        this.wardrobeManager = wardrobeManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        wardrobeManager.load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        wardrobeManager.unload(event.getPlayer().getUniqueId());
    }
}
