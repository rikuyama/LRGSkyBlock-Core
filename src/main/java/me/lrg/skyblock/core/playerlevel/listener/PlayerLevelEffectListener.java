package me.lrg.skyblock.core.playerlevel.listener;

import me.lrg.skyblock.core.playerlevel.event.PlayerLevelUpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PlayerLevelEffectListener implements Listener {
    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        var player = event.getPlayer();
        player.sendMessage(Component.text("レベルアップ！ ", NamedTextColor.GOLD)
                .append(Component.text(event.getOldLevel() + " → " + event.getNewLevel(), NamedTextColor.YELLOW)));
        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("LEVEL UP!", NamedTextColor.GOLD),
                Component.text("Lv." + event.getNewLevel(), NamedTextColor.YELLOW)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 24, 0.6, 0.8, 0.6, 0.05);
    }
}
