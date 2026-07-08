package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.model.StatsCategory;
import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.model.StatsType;
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
 * /stats all
 * /stats combat
 * /stats mining
 * /stats farming
 * /stats foraging
 * /stats fishing
 * /stats wisdom
 * /stats luck
 * /stats <player>
 * /stats <player> <category>
 * /stats set <player> <stat> <value>
 */
public class StatsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_USE = "lrgskyblock.command.stats";
    private static final String PERMISSION_ADMIN = "lrgskyblock.command.stats.admin";

    private static final List<String> BASE_STAT_NAMES = List.of(
            "health",
            "mana",
            "strength",
            "defense",
            "speed",
            "critical_chance",
            "magic_find"
    );

    private static final List<String> DISPLAY_MODES = List.of(
            "all",
            "combat",
            "mining",
            "farming",
            "foraging",
            "fishing",
            "wisdom",
            "luck"
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
            showOwnBaseStats(sender);
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

        if (isDisplayMode(args[0])) {
            showOwnStatsByMode(sender, args[0]);
            return true;
        }

        if (args.length >= 2 && isDisplayMode(args[1])) {
            showTargetStatsByMode(sender, args[0], args[1]);
            return true;
        }

        showTargetBaseStats(sender, args[0]);
        return true;
    }

    private void showOwnBaseStats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cコンソールから使う場合は /stats <プレイヤー名> を使ってください。");
            return;
        }

        showBaseStats(sender, player);
    }

    private void showOwnStatsByMode(CommandSender sender, String mode) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cコンソールから使う場合は /stats <プレイヤー名> <カテゴリ> を使ってください。");
            return;
        }

        showStatsByMode(sender, player, mode);
    }

    private void showTargetBaseStats(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c指定したプレイヤーがオンラインではありません。");
            return;
        }

        showBaseStats(sender, target);
    }

    private void showTargetStatsByMode(CommandSender sender, String targetName, String mode) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c指定したプレイヤーがオンラインではありません。");
            return;
        }

        showStatsByMode(sender, target, mode);
    }

    private void showStatsByMode(CommandSender sender, Player target, String mode) {
        if (mode.equalsIgnoreCase("all")) {
            showAllStats(sender, target);
            return;
        }

        Optional<StatsCategory> categoryOptional = StatsCategory.fromKey(mode);

        if (categoryOptional.isEmpty()) {
            sender.sendMessage("§c存在しないStatsカテゴリです。");
            sendUsage(sender);
            return;
        }

        showCategoryStats(sender, target, categoryOptional.get());
    }

    private void showBaseStats(CommandSender sender, Player target) {
        Optional<StatsData> statsDataOptional = statsManager.getStatsData(target.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            sender.sendMessage("§c対象プレイヤーのStatsDataがまだ読み込まれていません。");
            return;
        }

        StatsData statsData = statsDataOptional.get();

        sender.sendMessage("§6==== §e" + target.getName() + " の基本ステータス §6====");
        sendBaseStats(sender, statsData);
        sender.sendMessage("§7他カテゴリ: /stats combat, /stats mining, /stats farming, /stats fishing");
        sender.sendMessage("§7全表示: /stats all");
    }

    private void showAllStats(CommandSender sender, Player target) {
        Optional<StatsData> statsDataOptional = statsManager.getStatsData(target.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            sender.sendMessage("§c対象プレイヤーのStatsDataがまだ読み込まれていません。");
            return;
        }

        StatsData statsData = statsDataOptional.get();

        sender.sendMessage("§6==== §e" + target.getName() + " の全ステータス §6====");

        sender.sendMessage("§e[基本ステータス]");
        sendBaseStats(sender, statsData);

        for (StatsCategory category : StatsCategory.values()) {
            sender.sendMessage("§e[" + category.getDisplayName() + "]");
            sendCategoryLines(sender, statsData, category);
        }

        sender.sendMessage("§7※ 未実装のStatsは現在、保存・表示のみです。");
    }

    private void showCategoryStats(CommandSender sender, Player target, StatsCategory category) {
        Optional<StatsData> statsDataOptional = statsManager.getStatsData(target.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            sender.sendMessage("§c対象プレイヤーのStatsDataがまだ読み込まれていません。");
            return;
        }

        StatsData statsData = statsDataOptional.get();

        sender.sendMessage("§6==== §e" + target.getName() + " の " + category.getDisplayName() + " §6====");

        if (category == StatsCategory.COMBAT) {
            sender.sendMessage("§e[基本戦闘Stats]");
            sender.sendMessage(formatBaseStatLine("ヘルス", statsData.getHealth(), true));
            sender.sendMessage(formatBaseStatLine("マナ", statsData.getMana(), true));
            sender.sendMessage(formatBaseStatLine("ストレングス", statsData.getStrength(), true));
            sender.sendMessage(formatBaseStatLine("ディフェンス", statsData.getDefense(), true));
            sender.sendMessage(formatBaseStatLine("クリティカル率", statsData.getCriticalChance(), true) + "§f%");
        }

        if (category == StatsCategory.LUCK) {
            sender.sendMessage("§e[基本運Stats]");
            sender.sendMessage(formatBaseStatLine("マジックファインド", statsData.getMagicFind(), false));
        }

        sendCategoryLines(sender, statsData, category);
        sender.sendMessage("§7※ 未実装のStatsは現在、保存・表示のみです。");
    }

    private void sendBaseStats(CommandSender sender, StatsData statsData) {
        sender.sendMessage(formatBaseStatLine("ヘルス", statsData.getHealth(), true));
        sender.sendMessage(formatBaseStatLine("マナ", statsData.getMana(), true));
        sender.sendMessage(formatBaseStatLine("ストレングス", statsData.getStrength(), true));
        sender.sendMessage(formatBaseStatLine("ディフェンス", statsData.getDefense(), true));
        sender.sendMessage(formatBaseStatLine("スピード", statsData.getSpeed(), true));
        sender.sendMessage(formatBaseStatLine("クリティカル率", statsData.getCriticalChance(), true) + "§f%");
        sender.sendMessage(formatBaseStatLine("マジックファインド", statsData.getMagicFind(), false));
    }

    private void sendCategoryLines(CommandSender sender, StatsData statsData, StatsCategory category) {
        boolean hasAny = false;

        for (StatsType statsType : StatsType.values()) {
            if (statsType.getCategory() != category) {
                continue;
            }

            hasAny = true;
            sender.sendMessage(formatExtraStatLine(statsType, statsData.getExtraStat(statsType)));
        }

        if (!hasAny) {
            sender.sendMessage("§7このカテゴリにはStatsがありません。");
        }
    }

    private String formatBaseStatLine(String displayName, double value, boolean implemented) {
        String status = implemented ? "§a実装済み" : "§7未実装";
        return "§7" + displayName + ": §f" + format(value) + " §8[" + status + "§8]";
    }

    private String formatExtraStatLine(StatsType statsType, double value) {
        return "§7" + statsType.getDisplayName()
                + ": §f" + format(value)
                + " §8[" + statsType.getImplementationStatusText() + "§8]";
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

        Double value = parseValue(sender, args[3]);

        if (value == null) {
            return;
        }

        boolean success = setStatValue(target, statName, value);

        if (!success) {
            sender.sendMessage("§c存在しないステータスです。");
            sender.sendMessage("§7/stats help で使用可能Statsを確認してください。");
            return;
        }

        statsManager.applyStatsToPlayer(target);

        sender.sendMessage("§a" + target.getName() + " の " + getDisplayName(statName) + " を " + format(value) + " に設定しました。");
        showBaseStats(sender, target);
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
            case "critical_chance" -> statsData.setCriticalChance(value);
            case "magic_find" -> statsData.setMagicFind(value);
            default -> {
                Optional<StatsType> statsTypeOptional = StatsType.fromKey(statName);

                if (statsTypeOptional.isEmpty()) {
                    return false;
                }

                statsData.setExtraStat(statsTypeOptional.get(), value);
            }
        }

        return true;
    }

    private String getDisplayName(String statName) {
        return switch (statName) {
            case "health" -> "ヘルス";
            case "mana" -> "マナ";
            case "strength" -> "ストレングス";
            case "defense" -> "ディフェンス";
            case "speed" -> "スピード";
            case "critical_chance" -> "クリティカル率";
            case "magic_find" -> "マジックファインド";
            default -> StatsType.fromKey(statName)
                    .map(StatsType::getDisplayName)
                    .orElse(statName);
        };
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

    private boolean isDisplayMode(String input) {
        return DISPLAY_MODES.stream()
                .anyMatch(mode -> mode.equalsIgnoreCase(input));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eStatsコマンド:");
        sender.sendMessage("§7/stats §f- 自分の基本ステータスを確認");
        sender.sendMessage("§7/stats all §f- 自分の全ステータスを確認");
        sender.sendMessage("§7/stats <カテゴリ> §f- 自分のカテゴリ別Statsを確認");
        sender.sendMessage("§7/stats <プレイヤー名> §f- 指定プレイヤーの基本ステータスを確認");
        sender.sendMessage("§7/stats <プレイヤー名> <カテゴリ> §f- 指定プレイヤーのカテゴリ別Statsを確認");
        sender.sendMessage("§7/stats set <プレイヤー名> <stat> <value> §f- ステータスを変更");

        sender.sendMessage("§eカテゴリ:");
        sender.sendMessage("§7all, combat, mining, farming, foraging, fishing, wisdom, luck");

        sender.sendMessage("§e基本Stats:");
        sender.sendMessage("§7health=ヘルス, mana=マナ, strength=ストレングス, defense=ディフェンス, speed=スピード, critical_chance=クリティカル率, magic_find=マジックファインド");

        sender.sendMessage("§e追加Stats:");
        sender.sendMessage("§7" + String.join(", ", getExtraStatKeys()));

        sender.sendMessage("§7※ 実装済みStatsはゲーム内効果があります。");
        sender.sendMessage("§7※ 未実装Statsは現在、保存・表示のみです。");
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
            completions.addAll(DISPLAY_MODES);

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
            if (args[0].equalsIgnoreCase("set")) {
                if (!sender.hasPermission(PERMISSION_ADMIN)) {
                    return Collections.emptyList();
                }

                List<String> playerNames = Bukkit.getOnlinePlayers()
                        .stream()
                        .map(Player::getName)
                        .toList();

                return filterStartsWith(playerNames, args[1]);
            }

            List<String> playerNames = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .toList();

            if (playerNames.stream().anyMatch(name -> name.equalsIgnoreCase(args[0]))) {
                return filterStartsWith(DISPLAY_MODES, args[1]);
            }

            return Collections.emptyList();
        }

        if (args.length == 3) {
            if (!args[0].equalsIgnoreCase("set")) {
                return Collections.emptyList();
            }

            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return Collections.emptyList();
            }

            return filterStartsWith(getAllStatKeys(), args[2]);
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
                    "30",
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

    private List<String> getAllStatKeys() {
        List<String> keys = new ArrayList<>(BASE_STAT_NAMES);
        keys.addAll(getExtraStatKeys());
        return keys;
    }

    private List<String> getExtraStatKeys() {
        List<String> keys = new ArrayList<>();

        for (StatsType statsType : StatsType.values()) {
            keys.add(statsType.getKey());
        }

        return keys;
    }

    private List<String> filterStartsWith(List<String> values, String input) {
        String lowerInput = input.toLowerCase();

        return values.stream()
                .filter(value -> value.toLowerCase().startsWith(lowerInput))
                .toList();
    }
}