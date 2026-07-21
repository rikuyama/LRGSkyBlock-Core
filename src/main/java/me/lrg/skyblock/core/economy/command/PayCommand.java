package me.lrg.skyblock.core.economy.command;

import me.lrg.skyblock.core.economy.gui.BankGui;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.model.PlayerIdentity;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PayCommand implements CommandExecutor, TabCompleter {
    private final EconomyService economyService;

    public PayCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cプレイヤーのみ使用できます。");
            return true;
        }
        if (args.length != 2) {
            player.sendMessage("§e/pay <player> <amount>");
            return true;
        }
        PlayerIdentity target = economyService.findPlayer(args[0]).orElse(null);
        if (target == null) {
            player.sendMessage("§c指定したプレイヤーが見つかりません。");
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1].replace(",", ""));
        } catch (NumberFormatException exception) {
            player.sendMessage("§c金額が不正です。");
            return true;
        }
        EconomyResult result = economyService.transfer(player.getUniqueId(), target.uuid(), amount,
                EconomyOperationIds.create("pay", player.getUniqueId()));
        if (!result.success()) {
            player.sendMessage("§c送金に失敗しました: " + result.failure().name());
            return true;
        }
        player.sendMessage("§a" + target.name() + " に §6" + BankGui.format(amount) + " Coins §a送金しました。"
                + (result.fee() > 0L ? " §7手数料: " + BankGui.format(result.fee()) : ""));
        Player onlineTarget = Bukkit.getPlayer(target.uuid());
        if (onlineTarget != null) onlineTarget.sendMessage("§a" + player.getName() + " から §6" + BankGui.format(amount) + " Coins §a受け取りました。");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(prefix)) names.add(player.getName());
        }
        return names;
    }
}
