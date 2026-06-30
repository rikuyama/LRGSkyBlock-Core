package me.lrg.skyblock.core.command;

import me.lrg.skyblock.core.manager.CoinManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Coins確認・操作用のデバッグコマンド。
 *
 * 使用例:
 * /coins
 * /coins add 100
 * /coins remove 50
 * /coins set 1000
 *
 * 注意:
 * - 本番用ではなく開発確認用
 * - SQLは書かない
 * - Coins操作はCoinManagerへ任せる
 */
public class CoinCommand implements CommandExecutor {

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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        if (args.length == 0) {
            showCoins(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> addCoins(player, args);
            case "remove" -> removeCoins(player, args);
            case "set" -> setCoins(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void showCoins(Player player) {
        UUID uuid = player.getUniqueId();

        Optional<Long> coinsOptional = coinManager.getCoins(uuid);

        if (coinsOptional.isEmpty()) {
            player.sendMessage("§cプレイヤーデータがまだ読み込まれていません。");
            return;
        }

        player.sendMessage("§e現在のCoins: §6" + coinsOptional.get());
    }

    private void addCoins(Player player, String[] args) {
        Long amount = parseAmount(player, args);

        if (amount == null) {
            return;
        }

        boolean success = coinManager.addCoins(player.getUniqueId(), amount);

        if (!success) {
            player.sendMessage("§cCoinsの追加に失敗しました。");
            return;
        }

        player.sendMessage("§a" + amount + " Coinsを追加しました。");
        showCoins(player);
    }

    private void removeCoins(Player player, String[] args) {
        Long amount = parseAmount(player, args);

        if (amount == null) {
            return;
        }

        boolean success = coinManager.removeCoins(player.getUniqueId(), amount);

        if (!success) {
            player.sendMessage("§cCoinsの削除に失敗しました。所持Coinsが足りない可能性があります。");
            return;
        }

        player.sendMessage("§a" + amount + " Coinsを削除しました。");
        showCoins(player);
    }

    private void setCoins(Player player, String[] args) {
        Long amount = parseAmount(player, args);

        if (amount == null) {
            return;
        }

        boolean success = coinManager.setCoins(player.getUniqueId(), amount);

        if (!success) {
            player.sendMessage("§cCoinsの設定に失敗しました。");
            return;
        }

        player.sendMessage("§aCoinsを " + amount + " に設定しました。");
        showCoins(player);
    }

    private Long parseAmount(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c金額を入力してください。");
            sendUsage(player);
            return null;
        }

        try {
            long amount = Long.parseLong(args[1]);

            if (amount < 0L) {
                player.sendMessage("§c金額にマイナスは使えません。");
                return null;
            }

            return amount;
        } catch (NumberFormatException exception) {
            player.sendMessage("§c金額は数字で入力してください。");
            return null;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("§e使い方:");
        player.sendMessage("§7/coins");
        player.sendMessage("§7/coins add <金額>");
        player.sendMessage("§7/coins remove <金額>");
        player.sendMessage("§7/coins set <金額>");
    }
}