package me.lrg.skyblock.core.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RankCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final RankNameTagManager nameTagManager;
    private final RankResolver resolver = new RankResolver();
    private final Map<UUID, PermissionAttachment> debugAttachments = new java.util.HashMap<>();

    public RankCommand(JavaPlugin plugin, RankNameTagManager nameTagManager) {
        this.plugin = plugin;
        this.nameTagManager = nameTagManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("使用方法: /rank <player> または /rank set <player> <rank>");
                return true;
            }
            return show(sender, player);
        }
        if (args[0].equalsIgnoreCase("set")) return set(sender, args);
        if (args[0].equalsIgnoreCase("reset")) return reset(sender, args);
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("プレイヤーが見つかりません。");
            return true;
        }
        return show(sender, target);
    }

    private boolean show(CommandSender sender, Player target) {
        RankProfile profile = resolver.resolve(target::hasPermission);
        sender.sendMessage(Component.text(target.getName() + " のランク: ", NamedTextColor.GRAY)
                .append(Component.text(profile.rank().name(), NamedTextColor.AQUA)));
        return true;
    }

    private boolean set(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lrgskyblock.command.rank.admin")) {
            sender.sendMessage("権限がありません。");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("使用方法: /rank set <player> <rank>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("プレイヤーが見つかりません。");
            return true;
        }
        try {
            PlayerRank rank = PlayerRank.parse(args[2]);
            PermissionAttachment old = debugAttachments.remove(target.getUniqueId());
            if (old != null) target.removeAttachment(old);
            PermissionAttachment attachment = target.addAttachment(plugin);
            for (PlayerRank candidate : PlayerRank.values()) {
                if (candidate.isVisible()) attachment.setPermission(candidate.permission(), candidate == rank);
            }
            attachment.setPermission(RankResolver.LEGACY_YOUTUBE_PERMISSION, false);
            debugAttachments.put(target.getUniqueId(), attachment);
            target.recalculatePermissions();
            nameTagManager.update(target);
            sender.sendMessage(target.getName() + "のデバッグランクを" + rank.name() + "に設定しました。再起動で解除されます。");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("ランク: member, vip, vip+, mvp, mvp+, mvp++, youtube, staff, admin");
        }
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lrgskyblock.command.rank.admin")) {
            sender.sendMessage("権限がありません。");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("使用方法: /rank reset <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("プレイヤーが見つかりません。");
            return true;
        }
        PermissionAttachment attachment = debugAttachments.remove(target.getUniqueId());
        if (attachment != null) target.removeAttachment(attachment);
        target.recalculatePermissions();
        nameTagManager.update(target);
        sender.sendMessage(target.getName() + "のデバッグランクを解除しました。");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("set", "reset"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(List.of("member", "vip", "vip+", "mvp", "mvp+", "mvp++", "youtube", "staff", "admin"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> source, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : source) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }
}
