package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.model.StatsData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stats確認・管理用コマンド。
 *
 * 使用例:
 * /stats
 * /stats help
 * /stats <player>
 * /stats set <player> <stat> <value>
 *
 * 注意:
 * - SQLは書かない
 * - Stats操作はStatsManagerへ任せる
 * - 現段階ではオンラインプレイヤーのみ対象
 */
public class StatsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_USE = "lrgskyblock.command.stats";
    private static final String PERMISSION_ADMIN = "lrgskyblock.command.stats.admin";

    private static final List<String> STAT_NAMES = List.of(
            "health",
            "mana",
            "strength",
            "defense",
            "speed"
    );

    private final StatsManager statsManager;

    public StatsCommand(StatsManager statsManager) {
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage("§cこのコマンドを使う権限がありません。");
            return true;
        }

        if (args.length == 0) {
            showOwnStats(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            setStats(sender, args);
            return true;
        }

        showTargetStats(sender, args[0]);
        return true;
    }

    private void showOwnStats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cコンソールから使う場合は /stats <プレイヤー名> を使ってください。");
            return;
        }

        showStats(sender, player);
    }

    private void showTargetStats(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c指定したプレイヤーがオンラインではありません。");
            return;
        }

        showStats(sender, target);
    }

    private void showStats(CommandSender sender, Player target) {
        Optional<StatsData> statsDataOptional = statsManager.getStatsData(target.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            sender.sendMessage("§c対象プレイヤーのStatsDataがまだ読み込まれていません。");
            return;
        }

        StatsData statsData = statsDataOptional.get();

        sender.sendMessage("§6==== §e" + target.getName() + " のStats §6====");
        sender.sendMessage("§cHealth: §f" + format(statsData.getHealth()));
        sender.sendMessage("§bMana: §f" + format(statsData.getMana()));
        sender.sendMessage("§4Strength: §f" + format(statsData.getStrength()));
        sender.sendMessage("§aDefense: §f" + format(statsData.getDefense()));
        sender.sendMessage("§fSpeed: §f" + format(statsData.getSpeed()));
    }

    private void setStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage("§cStatsを変更する権限がありません。");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§c使い方: /stats set <プレイヤー名> <stat> <value>");
            sendUsage(sender);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c指定したプレイヤーがオンラインではありません。");
            return;
        }

        String statName = args[2].toLowerCase();

        if (!STAT_NAMES.contains(statName)) {
            sender.sendMessage("§c存在しないステータスです。");
            sender.sendMessage("§7使用可能: health, mana, strength, defense, speed");
            return;
        }

        Double value = parseValue(sender, args[3]);

        if (value == null) {
            return;
        }

        boolean success = setStatValue(target, statName, value);

        if (!success) {
            sender.sendMessage("§cStatsの変更に失敗しました。");
            return;
        }

        statsManager.applyStatsToPlayer(target);

        sender.sendMessage("§a" + target.getName() + " の " + statName + " を " + format(value) + " に設定しました。");
        showStats(sender, target);
    }

    private boolean setStatValue(Player target, String statName, double value) {
        Optional<StatsData> statsDataOptional = statsManager.getStatsData(target.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            return false;
        }

        StatsData statsData = statsDataOptional.get();

        switch (statName) {
            case "health" -> statsData.setHealth(value);
            case "mana" -> statsData.setMana(value);
            case "strength" -> statsData.setStrength(value);
            case "defense" -> statsData.setDefense(value);
            case "speed" -> statsData.setSpeed(value);
            default -> {
                return false;
            }
        }

        return true;
    }

    private Double parseValue(CommandSender sender, String text) {
        try {
            double value = Double.parseDouble(text);

            if (value < 0.0) {
                sender.sendMessage("§c値にマイナスは使えません。");
                return null;
            }

            return value;
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c値は数字で入力してください。");
            return null;
        }
    }

    private String format(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }

        return String.format("%.2f", value);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eStatsコマンド:");
        sender.sendMessage("§7/stats §f- 自分のStatsを確認");
        sender.sendMessage("§7/stats help §f- ヘルプを表示");
        sender.sendMessage("§7/stats <プレイヤー名> §f- 指定プレイヤーのStatsを確認");
        sender.sendMessage("§7/stats set <プレイヤー名> <stat> <value> §f- Statsを変更");
        sender.sendMessage("§7使用可能stat: health, mana, strength, defense, speed");
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!sender.hasPermission(PERMISSION_USE)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();

            completions.add("help");

            Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .forEach(completions::add);

            if (sender.hasPermission(PERMISSION_ADMIN)) {
                completions.add("set");
            }

            return filterStartsWith(completions, args[0]);
        }

        if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("set")) {
                return Collections.emptyList();
            }

            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return Collections.emptyList();
            }

            List<String> playerNames = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .toList();

            return filterStartsWith(playerNames, args[1]);
        }

        if (args.length == 3) {
            if (!args[0].equalsIgnoreCase("set")) {
                return Collections.emptyList();
            }

            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return Collections.emptyList();
            }

            return filterStartsWith(STAT_NAMES, args[2]);
        }

        if (args.length == 4) {
            if (!args[0].equalsIgnoreCase("set")) {
                return Collections.emptyList();
            }

            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return Collections.emptyList();
            }

            List<String> values = List.of(
                    "0",
                    "10",
                    "50",
                    "100",
                    "200",
                    "500",
                    "1000"
            );

            return filterStartsWith(values, args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> values, String input) {
        String lowerInput = input.toLowerCase();

        return values.stream()
                .filter(value -> value.toLowerCase().startsWith(lowerInput))
                .toList();
    }
}