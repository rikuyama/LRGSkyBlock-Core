package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.CoinManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Coins確認・操作用コマンド。
 *
 * 使用例:
 * /coins
 * /coins help
 * /coins add <player> <amount>
 * /coins remove <player> <amount>
 * /coins set <player> <amount>
 *
 * 注意:
 * - SQLは書かない
 * - Coins操作はCoinManagerへ任せる
 * - 現段階ではオンラインプレイヤーのみ対象
 */
public class CoinCommand implements CommandExecutor {

    private static final String PERMISSION_USE = "lrgskyblock.command.coins";
    private static final String PERMISSION_ADMIN = "lrgskyblock.command.coins.admin";

    private final CoinManager coinManager;

    public CoinCommand(CoinManager coinManager) {
        this.coinManager = Objects.requireNonNull(coinManager, "coinManager");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage("§cこのコマンドを使う権限がありません。");
            return true;
        }

        if (args.length == 0) {
            showOwnCoins(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendUsage(sender);
            case "add" -> addCoins(sender, args);
            case "remove" -> removeCoins(sender, args);
            case "set" -> setCoins(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void showOwnCoins(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cコンソールから使う場合は /coins help を確認してください。");
            return;
        }

        Optional<Long> coinsOptional = coinManager.getCoins(player.getUniqueId());

        if (coinsOptional.isEmpty()) {
            player.sendMessage("§cプレイヤーデータがまだ読み込まれていません。");
            return;
        }

        player.sendMessage("§e現在のCoins: §6" + coinsOptional.get());
    }

    private void addCoins(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }

        CommandInput input = parseAdminInput(sender, args);

        if (input == null) {
            return;
        }

        boolean success = coinManager.addCoins(input.target().getUniqueId(), input.amount());

        if (!success) {
            sender.sendMessage("§cCoinsの追加に失敗しました。");
            return;
        }

        sender.sendMessage("§a" + input.target().getName() + " に " + input.amount() + " Coinsを追加しました。");
        sendTargetCoins(sender, input.target());
    }

    private void removeCoins(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }

        CommandInput input = parseAdminInput(sender, args);

        if (input == null) {
            return;
        }

        boolean success = coinManager.removeCoins(input.target().getUniqueId(), input.amount());

        if (!success) {
            sender.sendMessage("§cCoinsの削除に失敗しました。所持Coinsが足りない可能性があります。");
            return;
        }

        sender.sendMessage("§a" + input.target().getName() + " から " + input.amount() + " Coinsを削除しました。");
        sendTargetCoins(sender, input.target());
    }

    private void setCoins(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }

        CommandInput input = parseAdminInput(sender, args);

        if (input == null) {
            return;
        }

        boolean success = coinManager.setCoins(input.target().getUniqueId(), input.amount());

        if (!success) {
            sender.sendMessage("§cCoinsの設定に失敗しました。");
            return;
        }

        sender.sendMessage("§a" + input.target().getName() + " のCoinsを " + input.amount() + " に設定しました。");
        sendTargetCoins(sender, input.target());
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            return true;
        }

        sender.sendMessage("§cCoinsを操作する権限がありません。");
        return false;
    }

    private CommandInput parseAdminInput(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cプレイヤー名と金額を入力してください。");
            sendUsage(sender);
            return null;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c指定したプレイヤーがオンラインではありません。");
            return null;
        }

        Long amount = parseAmount(sender, args[2]);

        if (amount == null) {
            return null;
        }

        return new CommandInput(target, amount);
    }

    private Long parseAmount(CommandSender sender, String text) {
        try {
            long amount = Long.parseLong(text);

            if (amount < 0L) {
                sender.sendMessage("§c金額にマイナスは使えません。");
                return null;
            }

            return amount;
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c金額は数字で入力してください。");
            return null;
        }
    }

    private void sendTargetCoins(CommandSender sender, Player target) {
        UUID uuid = target.getUniqueId();

        Optional<Long> coinsOptional = coinManager.getCoins(uuid);

        if (coinsOptional.isEmpty()) {
            sender.sendMessage("§c対象プレイヤーのデータがまだ読み込まれていません。");
            return;
        }

        sender.sendMessage("§e" + target.getName() + " の現在のCoins: §6" + coinsOptional.get());
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eCoinsコマンド:");
        sender.sendMessage("§7/coins §f- 自分のCoinsを確認");
        sender.sendMessage("§7/coins help §f- ヘルプを表示");
        sender.sendMessage("§7/coins add <プレイヤー名> <金額> §f- Coinsを追加");
        sender.sendMessage("§7/coins remove <プレイヤー名> <金額> §f- Coinsを削除");
        sender.sendMessage("§7/coins set <プレイヤー名> <金額> §f- Coinsを設定");
    }

    private record CommandInput(Player target, long amount) {
    }
}