package me.lrg.skyblock.core.economy.command;

import me.lrg.skyblock.core.economy.gui.BankGui;
import me.lrg.skyblock.core.economy.model.BankAccount;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BalanceCommand implements CommandExecutor {
    private final EconomyService economyService;

    public BalanceCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cプレイヤーのみ使用できます。");
            return true;
        }
        BankAccount account = economyService.getBankAccount(player.getUniqueId());
        sender.sendMessage("§6Wallet: §e" + BankGui.format(economyService.getWalletBalance(player.getUniqueId())) + " Coins");
        sender.sendMessage("§6Bank: §e" + BankGui.format(account.balance()) + " Coins §7(Lv." + account.level() + ")");
        return true;
    }
}
