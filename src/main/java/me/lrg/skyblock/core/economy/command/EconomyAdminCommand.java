package me.lrg.skyblock.core.economy.command;

import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.model.PlayerIdentity;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class EconomyAdminCommand implements CommandExecutor {
    private final EconomyService economyService;

    public EconomyAdminCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lrgskyblock.command.economyadmin")) {
            sender.sendMessage("§c権限がありません。");
            return true;
        }
        if (args.length != 3 || (!args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take"))) {
            sender.sendMessage("§e/economyadmin <give|take> <player> <amount>");
            return true;
        }
        PlayerIdentity target = economyService.findPlayer(args[1]).orElse(null);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません。");
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[2].replace(",", ""));
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c金額が不正です。");
            return true;
        }
        EconomyResult result = args[0].equalsIgnoreCase("give")
                ? economyService.depositWallet(target.uuid(), amount, "admin_give", EconomyOperationIds.create("admin-give", target.uuid()))
                : economyService.withdrawWallet(target.uuid(), amount, "admin_take", EconomyOperationIds.create("admin-take", target.uuid()));
        sender.sendMessage(result.success() ? "§a操作が完了しました。" : "§c操作に失敗しました: " + result.failure().name());
        return true;
    }
}
