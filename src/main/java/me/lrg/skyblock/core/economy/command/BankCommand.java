package me.lrg.skyblock.core.economy.command;

import me.lrg.skyblock.core.economy.gui.BankGui;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BankCommand implements CommandExecutor {
    private final EconomyService economyService;
    private final BankGui bankGui;

    public BankCommand(EconomyService economyService, BankGui bankGui) {
        this.economyService = economyService;
        this.bankGui = bankGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cプレイヤーのみ使用できます。");
            return true;
        }
        if (args.length == 0) {
            bankGui.open(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("history")) {
            bankGui.openHistory(player);
            return true;
        }
        if (args.length != 2) {
            player.sendMessage("§e/bank [deposit|withdraw] <amount|all>");
            return true;
        }
        long amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = args[0].equalsIgnoreCase("deposit")
                    ? economyService.getWalletBalance(player.getUniqueId())
                    : economyService.getBankAccount(player.getUniqueId()).balance();
        } else {
            try {
                amount = Long.parseLong(args[1].replace(",", ""));
            } catch (NumberFormatException exception) {
                player.sendMessage("§c金額が不正です。");
                return true;
            }
        }
        EconomyResult result;
        if (args[0].equalsIgnoreCase("deposit")) {
            result = economyService.depositBank(player.getUniqueId(), amount,
                    EconomyOperationIds.create("bank-deposit-command", player.getUniqueId()));
        } else if (args[0].equalsIgnoreCase("withdraw")) {
            result = economyService.withdrawBank(player.getUniqueId(), amount,
                    EconomyOperationIds.create("bank-withdraw-command", player.getUniqueId()));
        } else {
            player.sendMessage("§e/bank [deposit|withdraw] <amount|all>");
            return true;
        }
        player.sendMessage(result.success()
                ? "§a銀行取引が完了しました。残高: §6" + BankGui.format(result.bankBalance())
                : "§c銀行取引に失敗しました: " + result.failure().name());
        return true;
    }
}
