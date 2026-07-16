package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.model.BaseStatsType;
import me.lrg.skyblock.core.model.CalculatedStats;
import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.model.StatsLayer;
import me.lrg.skyblock.core.model.StatsModifierData;
import me.lrg.skyblock.core.model.StatsType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class StatsDebugCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "lrgskyblock.command.lrg.stats";
    private static final List<String> ACTIONS = List.of("view", "set", "add", "reset", "recalculate");
    private static final List<String> BASE_KEYS = List.of(
            "health", "mana", "strength", "defense", "speed", "critical_chance", "magic_find"
    );

    private final StatsManager statsManager;

    public StatsDebugCommand(StatsManager statsManager) {
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cこのコマンドを使う権限がありません。");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("stats")) {
            sendUsage(sender);
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(args[2]);
        if (target == null) {
            sender.sendMessage("§c指定したプレイヤーが見つかりません。");
            return true;
        }

        return switch (action) {
            case "view" -> handleView(sender, target, args);
            case "set" -> handleSet(sender, target, args, false);
            case "add" -> handleSet(sender, target, args, true);
            case "reset" -> handleReset(sender, target, args);
            case "recalculate" -> handleRecalculate(sender, target);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleView(CommandSender sender, OfflinePlayer target, String[] args) {
        String statKey = args.length >= 4 ? args[3].toLowerCase(Locale.ROOT) : null;
        withStats(sender, target, statsData -> {
            Player online = target.getPlayer();
            if (online != null && online.isOnline()) {
                showOnlineBreakdown(sender, online, statsData, statKey);
            } else {
                showOfflineBase(sender, target, statsData, statKey);
            }
        });
        return true;
    }

    private boolean handleSet(CommandSender sender, OfflinePlayer target, String[] args, boolean add) {
        if (args.length < 5) {
            sender.sendMessage("§c使い方: /lrg stats " + (add ? "add" : "set") + " <player> <stat> <value>");
            return true;
        }

        String key = args[3].toLowerCase(Locale.ROOT);
        Double parsed = parseNonNegative(sender, args[4]);
        if (parsed == null) {
            return true;
        }

        withStats(sender, target, statsData -> {
            double newValue = add ? getValue(statsData, key).orElse(Double.NaN) + parsed : parsed;
            if (!Double.isFinite(newValue) || !setValue(statsData, key, newValue)) {
                sender.sendMessage("§c存在しないStatsです: " + key);
                return;
            }

            statsManager.saveStatsForDebug(statsData, success -> {
                if (!success) {
                    sender.sendMessage("§cStatsの保存に失敗しました。");
                    return;
                }
                sender.sendMessage("§a" + target.getName() + " の " + key + " を " + format(newValue) + " にしました。");
            });
        });
        return true;
    }

    private boolean handleReset(CommandSender sender, OfflinePlayer target, String[] args) {
        String key = args.length >= 4 ? args[3].toLowerCase(Locale.ROOT) : "all";

        withStats(sender, target, statsData -> {
            StatsData defaults = statsManager.createDefaultStatsForDebug(target.getUniqueId());
            boolean success;

            if (key.equals("all")) {
                copyAll(defaults, statsData);
                success = true;
            } else {
                Optional<Double> defaultValue = getValue(defaults, key);
                success = defaultValue.isPresent() && setValue(statsData, key, defaultValue.get());
            }

            if (!success) {
                sender.sendMessage("§c存在しないStatsです: " + key);
                return;
            }

            statsManager.saveStatsForDebug(statsData, saved -> sender.sendMessage(
                    saved ? "§aStatsを初期値へ戻しました。" : "§cStatsの保存に失敗しました。"
            ));
        });
        return true;
    }

    private boolean handleRecalculate(CommandSender sender, OfflinePlayer target) {
        if (!target.isOnline()) {
            sender.sendMessage("§eオフラインプレイヤーはBase Statsのみ保存されています。再計算はログイン後に行われます。");
            return true;
        }

        boolean success = statsManager.recalculateStats(target.getUniqueId());
        sender.sendMessage(success ? "§a最終Statsを再計算して適用しました。" : "§cStatsが読み込まれていません。");
        return true;
    }

    private void showOnlineBreakdown(CommandSender sender, Player player, StatsData base, String statKey) {
        Optional<CalculatedStats> calculatedOptional = statsManager.getCalculatedStats(player.getUniqueId());
        if (calculatedOptional.isEmpty()) {
            sender.sendMessage("§c最終Statsを計算できませんでした。");
            return;
        }

        sender.sendMessage("§6==== §e" + player.getName() + " Stats内訳 §6====");
        if (statKey != null) {
            showSingleBreakdown(sender, player.getUniqueId(), base, calculatedOptional.get(), statKey);
            return;
        }

        for (String key : BASE_KEYS) {
            showSingleBreakdown(sender, player.getUniqueId(), base, calculatedOptional.get(), key);
        }
        for (StatsType type : StatsType.values()) {
            showSingleBreakdown(sender, player.getUniqueId(), base, calculatedOptional.get(), type.getKey());
        }
    }

    private void showSingleBreakdown(
            CommandSender sender,
            UUID uuid,
            StatsData base,
            CalculatedStats calculated,
            String key
    ) {
        Optional<Double> baseValue = getValue(base, key);
        Optional<Double> finalValue = getCalculatedValue(calculated, key);
        if (baseValue.isEmpty() || finalValue.isEmpty()) {
            sender.sendMessage("§c存在しないStatsです: " + key);
            return;
        }

        sender.sendMessage("§e" + key + " §7Base=§f" + format(baseValue.get()) + " §7Final=§a" + format(finalValue.get()));
        Map<StatsLayer, StatsModifierData> layers = statsManager.getStatsLayerSnapshot(uuid);
        for (StatsLayer layer : StatsLayer.values()) {
            StatsModifierData modifier = layers.get(layer);
            if (modifier == null) {
                continue;
            }
            double value = getModifierValue(modifier, key);
            if (Double.compare(value, 0.0) != 0) {
                sender.sendMessage("§8  " + layer.name() + ": §b" + signed(value));
            }
        }
    }

    private void showOfflineBase(CommandSender sender, OfflinePlayer player, StatsData data, String statKey) {
        sender.sendMessage("§6==== §e" + player.getName() + " の保存済みBase Stats §6====");
        if (statKey != null) {
            Optional<Double> value = getValue(data, statKey);
            sender.sendMessage(value.map(v -> "§e" + statKey + ": §f" + format(v))
                    .orElse("§c存在しないStatsです: " + statKey));
            return;
        }
        for (String key : BASE_KEYS) {
            sender.sendMessage("§e" + key + ": §f" + format(getValue(data, key).orElse(0.0)));
        }
        for (StatsType type : StatsType.values()) {
            sender.sendMessage("§e" + type.getKey() + ": §f" + format(data.getExtraStat(type)));
        }
    }

    private void withStats(CommandSender sender, OfflinePlayer target, Consumer<StatsData> consumer) {
        sender.sendMessage("§7Statsを読み込んでいます...");
        statsManager.loadStatsForDebug(target.getUniqueId(), optional -> {
            if (optional.isEmpty()) {
                sender.sendMessage("§cStatsDataが見つからないか、DB読み込みに失敗しました。");
                return;
            }
            consumer.accept(optional.get());
        });
    }

    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private Optional<Double> getValue(StatsData data, String key) {
        return switch (key) {
            case "health" -> Optional.of(data.getHealth());
            case "mana" -> Optional.of(data.getMana());
            case "strength" -> Optional.of(data.getStrength());
            case "defense" -> Optional.of(data.getDefense());
            case "speed" -> Optional.of(data.getSpeed());
            case "critical_chance" -> Optional.of(data.getCriticalChance());
            case "magic_find" -> Optional.of(data.getMagicFind());
            default -> StatsType.fromKey(key).map(data::getExtraStat);
        };
    }

    private Optional<Double> getCalculatedValue(CalculatedStats data, String key) {
        return switch (key) {
            case "health" -> Optional.of(data.getHealth());
            case "mana" -> Optional.of(data.getMana());
            case "strength" -> Optional.of(data.getStrength());
            case "defense" -> Optional.of(data.getDefense());
            case "speed" -> Optional.of(data.getSpeed());
            case "critical_chance" -> Optional.of(data.getCriticalChance());
            case "magic_find" -> Optional.of(data.getMagicFind());
            default -> StatsType.fromKey(key).map(data::getExtraStat);
        };
    }

    private double getModifierValue(StatsModifierData modifier, String key) {
        return switch (key) {
            case "health" -> modifier.getBaseStat(BaseStatsType.HEALTH);
            case "mana" -> modifier.getBaseStat(BaseStatsType.MANA);
            case "strength" -> modifier.getBaseStat(BaseStatsType.STRENGTH);
            case "defense" -> modifier.getBaseStat(BaseStatsType.DEFENSE);
            case "speed" -> modifier.getBaseStat(BaseStatsType.SPEED);
            case "critical_chance" -> modifier.getBaseStat(BaseStatsType.CRITICAL_CHANCE);
            case "magic_find" -> modifier.getBaseStat(BaseStatsType.MAGIC_FIND);
            default -> StatsType.fromKey(key).map(modifier::getExtraStat).orElse(0.0);
        };
    }

    private boolean setValue(StatsData data, String key, double value) {
        switch (key) {
            case "health" -> data.setHealth(value);
            case "mana" -> data.setMana(value);
            case "strength" -> data.setStrength(value);
            case "defense" -> data.setDefense(value);
            case "speed" -> data.setSpeed(value);
            case "critical_chance" -> data.setCriticalChance(Math.min(100.0, value));
            case "magic_find" -> data.setMagicFind(value);
            default -> {
                Optional<StatsType> type = StatsType.fromKey(key);
                if (type.isEmpty()) {
                    return false;
                }
                data.setExtraStat(type.get(), value);
            }
        }
        return true;
    }

    private void copyAll(StatsData source, StatsData target) {
        target.setHealth(source.getHealth());
        target.setMana(source.getMana());
        target.setStrength(source.getStrength());
        target.setDefense(source.getDefense());
        target.setSpeed(source.getSpeed());
        target.setCriticalChance(source.getCriticalChance());
        target.setMagicFind(source.getMagicFind());
        for (StatsType type : StatsType.values()) {
            target.setExtraStat(type, source.getExtraStat(type));
        }
    }

    private Double parseNonNegative(CommandSender sender, String text) {
        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value) || value < 0.0) {
                sender.sendMessage("§c0以上の有限な数値を指定してください。");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c数値を指定してください。");
            return null;
        }
    }

    private String signed(double value) {
        return (value > 0.0 ? "+" : "") + format(value);
    }

    private String format(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/lrg stats view <player> [stat]");
        sender.sendMessage("§e/lrg stats set <player> <stat> <value>");
        sender.sendMessage("§e/lrg stats add <player> <stat> <value>");
        sender.sendMessage("§e/lrg stats reset <player> [stat|all]");
        sender.sendMessage("§e/lrg stats recalculate <player>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("stats"), args[0]);
        }
        if (!args[0].equalsIgnoreCase("stats")) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return filter(ACTIONS, args[1]);
        }
        if (args.length == 3) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return filter(names, args[2]);
        }
        if (args.length == 4) {
            if (args[1].equalsIgnoreCase("recalculate")) {
                return Collections.emptyList();
            }
            List<String> keys = new ArrayList<>(BASE_KEYS);
            for (StatsType type : StatsType.values()) {
                keys.add(type.getKey());
            }
            if (args[1].equalsIgnoreCase("reset")) {
                keys.add("all");
            }
            return filter(keys, args[3]);
        }
        if (args.length == 5 && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add"))) {
            return filter(List.of("0", "10", "30", "50", "100", "250", "500"), args[4]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
