package me.lrg.skyblock.core.playerlevel.listener;

import me.lrg.skyblock.core.playerlevel.event.PlayerLevelUpEvent;
import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PlayerLevelEffectListener implements Listener {
    private static final String BORDER = "━━━━━━━━━━━━━━━━━━━━━━";
    private static final int CENTER_WIDTH = 22;

    private final JavaPlugin plugin;
    private final PlayerLevelUnlockManager unlockManager;

    public PlayerLevelEffectListener(JavaPlugin plugin, PlayerLevelUnlockManager unlockManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.unlockManager = Objects.requireNonNull(unlockManager, "unlockManager");
    }

    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        var player = event.getPlayer();

        if (plugin.getConfig().getBoolean("player-level.effects.message", true)) {
            sendLevelUpMessage(event);
        }
        if (plugin.getConfig().getBoolean("player-level.effects.sound", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.15f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.6f);
                }
            }, 3L);
        }
        if (plugin.getConfig().getBoolean("player-level.effects.particles", true)) {
            player.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1, 0),
                    24,
                    0.6,
                    0.8,
                    0.6,
                    0.05
            );
        }
    }

    private void sendLevelUpMessage(PlayerLevelUpEvent event) {
        var player = event.getPlayer();
        player.sendMessage(Component.text(BORDER, NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(centered("レベルアップ！", NamedTextColor.GOLD, true));
        player.sendMessage(Component.empty());
        player.sendMessage(centered("Player Level", NamedTextColor.AQUA, true));
        player.sendMessage(centered(event.getOldLevel() + " → " + event.getNewLevel(), NamedTextColor.YELLOW, true));

        List<String> unlocked = newlyUnlocked(event.getOldLevel(), event.getNewLevel());
        if (!unlocked.isEmpty()) {
            player.sendMessage(Component.empty());
            for (String displayName : unlocked) {
                player.sendMessage(Component.text("🎉 「", NamedTextColor.YELLOW)
                        .append(Component.text(displayName, NamedTextColor.AQUA, TextDecoration.BOLD))
                        .append(Component.text("」を解放しました！", NamedTextColor.YELLOW)));
            }
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text(BORDER, NamedTextColor.DARK_GRAY));
    }

    private List<String> newlyUnlocked(int oldLevel, int newLevel) {
        List<String> unlocked = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : unlockManager.getUnlocks().entrySet()) {
            int requiredLevel = entry.getValue();
            if (oldLevel < requiredLevel && newLevel >= requiredLevel) {
                unlocked.add(unlockManager.getDisplayName(entry.getKey()));
            }
        }
        return unlocked;
    }

    private Component centered(String text, NamedTextColor color, boolean bold) {
        int spaces = Math.max(0, (CENTER_WIDTH - visualWidth(text)) / 2);
        TextComponent component = Component.text(" ".repeat(spaces) + text, color);
        return bold ? component.decorate(TextDecoration.BOLD) : component;
    }

    private int visualWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            width += character <= 0x7F ? 1 : 2;
        }
        return width;
    }
}
