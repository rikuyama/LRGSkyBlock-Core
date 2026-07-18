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

import java.util.List;

public final class PlayerLevelCommand implements CommandExecutor, TabCompleter {
    private final PlayerLevelManager manager;
    private final PlayerLevelUnlockManager unlockManager;

    public PlayerLevelCommand(PlayerLevelManager manager, PlayerLevelUnlockManager unlockManager) {
        this.manager = manager;
        this.unlockManager = unlockManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("addxp")) return addXp(sender, args);
        Player target = args.length == 0 && sender instanceof Player p ? p : args.length > 0 ? Bukkit.getPlayerExact(args[0]) : null;
        if (target == null) { sender.sendMessage("プレイヤーが見つかりません。"); return true; }
        int level = manager.getLevel(target.getUniqueId());
        long xp = manager.getCurrentXp(target.getUniqueId());
        long required = manager.getRequiredXp(level);
        int bars = required <= 0 ? 10 : (int)Math.min(10, xp * 10 / required);
        String progress = "■".repeat(bars) + "□".repeat(10 - bars);
        sender.sendMessage(Component.text("=== " + target.getName() + " のPlayer Level ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Lv." + level + "  " + xp + " / " + required + " XP", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(progress, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Auto Pickup: " + (unlockManager.isUnlocked(target.getUniqueId(), PlayerLevelUnlockManager.AUTO_PICKUP) ? "解放済み" : "Lv.6で解放"), NamedTextColor.AQUA));
        return true;
    }

    private boolean addXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lrgskyblock.command.level.admin")) { sender.sendMessage("権限がありません。"); return true; }
        if (args.length < 3) { sender.sendMessage("使用方法: /level addxp <player> <amount>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("プレイヤーが見つかりません。"); return true; }
        try {
            long amount = Long.parseLong(args[2]);
            var result = manager.addXp(target.getUniqueId(), amount, PlayerLevelXpReason.ADMIN);
            sender.sendMessage(result.success() ? amount + " XPを追加しました。" : "XPを追加できませんでした。");
        } catch (NumberFormatException | IllegalArgumentException ex) { sender.sendMessage("amountには0以上の整数を指定してください。"); }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("lrgskyblock.command.level.admin")) return List.of("addxp");
        return List.of();
    }
}
