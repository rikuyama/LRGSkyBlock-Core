package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.StatsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Objects;

public final class StatsCacheCommand implements CommandExecutor {

    private static final String PERMISSION = "lrgskyblock.command.stats.admin";

    private final StatsManager statsManager;

    public StatsCacheCommand(StatsManager statsManager) {
        this.statsManager = Objects.requireNonNull(statsManager, "statsManager");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cStatsキャッシュを確認する権限がありません。");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("save")) {
            statsManager.saveDirtyStatsAsynchronously();
            sender.sendMessage("§a変更済みStatsの非同期保存を開始しました。");
            return true;
        }

        sender.sendMessage("§6==== §eStatsキャッシュ状態 §6====");
        sender.sendMessage("§7キャッシュ人数: §f" + statsManager.getCachedPlayerCount());
        sender.sendMessage("§7未保存人数: §f" + statsManager.getDirtyPlayerCount());
        sender.sendMessage("§7読み込み中: §f" + statsManager.getLoadingPlayerCount());
        sender.sendMessage("§7保存中: §f" + statsManager.getSavingPlayerCount());
        sender.sendMessage("§7読み込み失敗: §f" + statsManager.getFailedPlayerCount());
        sender.sendMessage("§7手動保存: §f/statscache save");
        return true;
    }
}
