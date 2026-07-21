package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
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

public final class CoinCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION_USE = "lrgskyblock.command.coins";
    private static final String PERMISSION_ADMIN = "lrgskyblock.command.coins.admin";
    private final EconomyService economyService;

    public CoinCommand(EconomyService economyService) {
        this.economyService = Objects.requireNonNull(economyService, "economyService");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage("§cこのコマンドを使う権限がありません。");
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cコンソールから使う場合は /coins help を確認してください。");
                return true;
            }
            sender.sendMessage("§e現在のCoins: §6" + economyService.getWalletBalance(player.getUniqueId()));
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sendUsage(sender);
            return true;
        }
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage("§cCoinsを操作する権限がありません。");
            return true;
        }
        if (args.length != 3) {
            sendUsage(sender);
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c指定したプレイヤーがオンラインではありません。");
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[2].replace(",", ""));
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c金額は数字で入力してください。");
            return true;
        }
        EconomyResult result = switch (args[0].toLowerCase()) {
            case "add" -> economyService.depositWallet(target.getUniqueId(), amount, "coins_admin_add",
                    EconomyOperationIds.create("coins-add", target.getUniqueId()));
            case "remove" -> economyService.withdrawWallet(target.getUniqueId(), amount, "coins_admin_remove",
                    EconomyOperationIds.create("coins-remove", target.getUniqueId()));
            case "set" -> economyService.setWallet(target.getUniqueId(), amount, "coins_admin_set",
                    EconomyOperationIds.create("coins-set", target.getUniqueId()));
            default -> null;
        };
        if (result == null) {
            sendUsage(sender);
            return true;
        }
        sender.sendMessage(result.success()
                ? "§a" + target.getName() + " のCoinsを更新しました。現在: §6" + result.walletBalance()
                : "§cCoins操作に失敗しました: " + result.failure().name());
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/coins §7- 自分のCoinsを確認");
        sender.sendMessage("§e/coins <add|remove|set> <player> <amount>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION_USE)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            values.add("help");
            if (sender.hasPermission(PERMISSION_ADMIN)) values.addAll(List.of("add", "remove", "set"));
            return filter(values, args[0]);
        }
        if (args.length == 2 && sender.hasPermission(PERMISSION_ADMIN)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && sender.hasPermission(PERMISSION_ADMIN)) {
            return filter(List.of("1", "10", "100", "1000", "10000"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase();
        return values.stream().filter(value -> value.toLowerCase().startsWith(lower)).toList();
    }
}
