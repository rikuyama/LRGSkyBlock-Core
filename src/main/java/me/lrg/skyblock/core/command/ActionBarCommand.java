package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.ActionBarSettingsManager;
import me.lrg.skyblock.core.manager.StatsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ActionBarCommand implements CommandExecutor, TabCompleter {

    private final ActionBarSettingsManager settingsManager;
    private final StatsManager statsManager;

    public ActionBarCommand(ActionBarSettingsManager settingsManager, StatsManager statsManager) {
        this.settingsManager = Objects.requireNonNull(settingsManager, "settingsManager");
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendStatus(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                settingsManager.setEnabled(player, true);
                player.sendMessage("§aHealth・ManaのActionBar表示を有効にしました。");
                statsManager.sendStatsActionBar(player);
            }
            case "off" -> {
                settingsManager.setEnabled(player, false);
                player.sendActionBar(net.kyori.adventure.text.Component.empty());
                player.sendMessage("§eHealth・ManaのActionBar表示を無効にしました。");
            }
            case "toggle" -> {
                boolean enabled = settingsManager.toggle(player);
                player.sendMessage(enabled
                        ? "§aHealth・ManaのActionBar表示を有効にしました。"
                        : "§eHealth・ManaのActionBar表示を無効にしました。"
                );
                if (enabled) {
                    statsManager.sendStatsActionBar(player);
                } else {
                    player.sendActionBar(net.kyori.adventure.text.Component.empty());
                }
            }
            default -> player.sendMessage("§c使い方: /actionbar <on|off|toggle|status>");
        }

        return true;
    }

    private void sendStatus(Player player) {
        boolean enabled = settingsManager.isEnabled(player);
        player.sendMessage("§6ActionBar表示: " + (enabled ? "§aON" : "§cOFF"));
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        return List.of("on", "off", "toggle", "status").stream()
                .filter(value -> value.startsWith(input))
                .toList();
    }
}
