package me.lrg.skyblock.core.playerlevel.command;

import me.lrg.skyblock.core.playerlevel.api.PlayerLevelXpReason;
import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;
import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerLevelCommand implements CommandExecutor, TabCompleter {
    private final PlayerLevelManager manager;
    private final PlayerLevelUnlockManager unlockManager;

    public PlayerLevelCommand(PlayerLevelManager manager, PlayerLevelUnlockManager unlockManager) {
        this.manager = manager;
        this.unlockManager = unlockManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "addxp" -> addXp(sender, args);
                case "removexp" -> removeXp(sender, args);
                case "set" -> setLevel(sender, args);
                default -> show(sender, args[0]);
            };
        }
        return show(sender, null);
    }

    private boolean show(CommandSender sender, String targetName) {
        Player target;
        if (targetName == null && sender instanceof Player player) {
            target = player;
        } else {
            target = targetName == null ? null : Bukkit.getPlayerExact(targetName);
        }
        if (target == null) {
            sender.sendMessage("プレイヤーが見つかりません。");
            return true;
        }

        int level = manager.getLevel(target.getUniqueId());
        long xp = manager.getCurrentXp(target.getUniqueId());
        long required = manager.getRequiredXp(level);
        int bars = required <= 0 ? 10 : (int) Math.min(10, xp * 10 / required);
        String progress = "■".repeat(bars) + "□".repeat(10 - bars);
        sender.sendMessage(Component.text("=== " + target.getName() + " のPlayer Level ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Lv." + level + "  " + xp + " / " + required + " XP", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(progress, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("解放機能:", NamedTextColor.AQUA));
        unlockManager.getUnlocks().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByValue())
                .forEach(entry -> {
                    boolean unlocked = unlockManager.isUnlocked(target.getUniqueId(), entry.getKey());
                    String status = unlocked ? "解放済み" : "Lv." + entry.getValue() + "で解放";
                    sender.sendMessage(Component.text("  " + unlockManager.getDisplayName(entry.getKey()) + ": " + status,
                            unlocked ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                });
        sender.sendMessage(Component.text("ステータスボーナス: Health +" + (long) me.lrg.skyblock.core.playerlevel.formula.PlayerLevelStatFormula.healthBonus(level)
                + " / Strength +" + (long) me.lrg.skyblock.core.playerlevel.formula.PlayerLevelStatFormula.strengthBonus(level), NamedTextColor.RED));
        return true;
    }

    private boolean addXp(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) return true;
        if (args.length < 3) {
            sender.sendMessage("使用方法: /level addxp <player> <xp>");
            return true;
        }
        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return true;
        try {
            long amount = parseNonNegativeLong(args[2]);
            var result = manager.addXp(target.getUniqueId(), amount, PlayerLevelXpReason.ADMIN);
            sender.sendMessage(result.success() ? target.getName() + "に" + amount + " XP追加しました。" : "XPを追加できませんでした。");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("XPには0以上の整数を指定してください。");
        }
        return true;
    }

    private boolean removeXp(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) return true;
        if (args.length < 3) {
            sender.sendMessage("使用方法: /level removexp <player> <xp>");
            return true;
        }
        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return true;
        try {
            long amount = parseNonNegativeLong(args[2]);
            boolean success = manager.removeXp(target.getUniqueId(), amount);
            sender.sendMessage(success ? target.getName() + "から" + amount + " XP削除しました。" : "XPを削除できませんでした。");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("XPには0以上の整数を指定してください。");
        }
        return true;
    }

    private boolean setLevel(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) return true;
        if (args.length < 3) {
            sender.sendMessage("使用方法: /level set <player> <level>");
            return true;
        }
        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return true;
        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1) throw new IllegalArgumentException();
            boolean success = manager.setLevel(target.getUniqueId(), level);
            sender.sendMessage(success ? target.getName() + "のPlayer Levelを" + level + "に設定しました。" : "レベルを設定できませんでした。");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("levelには1以上の整数を指定してください。");
        }
        return true;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission("lrgskyblock.command.level.admin")) return true;
        sender.sendMessage("権限がありません。");
        return false;
    }

    private Player requireOnlinePlayer(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) sender.sendMessage("プレイヤーが見つかりません。");
        return target;
    }

    private long parseNonNegativeLong(String value) {
        long parsed = Long.parseLong(value);
        if (parsed < 0) throw new IllegalArgumentException();
        return parsed;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("lrgskyblock.command.level.admin")) {
            return filter(List.of("set", "addxp", "removexp"), args[0]);
        }
        if (args.length == 2 && sender.hasPermission("lrgskyblock.command.level.admin")
                && List.of("set", "addxp", "removexp").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        }
        return result;
    }
}
