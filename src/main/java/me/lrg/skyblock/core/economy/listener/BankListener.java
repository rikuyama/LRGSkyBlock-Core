package me.lrg.skyblock.core.economy.listener;

import me.lrg.skyblock.core.economy.gui.BankGui;
import me.lrg.skyblock.core.economy.gui.BankHolder;
import me.lrg.skyblock.core.economy.model.BankAccount;
import me.lrg.skyblock.core.economy.model.EconomyFailure;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.service.BankInteractionGuard;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class BankListener implements Listener {
    private static final long MONEY_ACTION_COOLDOWN_MILLIS = 750L;

    private final EconomyService economyService;
    private final BankGui bankGui;
    private final BankInteractionGuard interactionGuard = new BankInteractionGuard(MONEY_ACTION_COOLDOWN_MILLIS);

    public BankListener(EconomyService economyService, BankGui bankGui) {
        this.economyService = economyService;
        this.bankGui = bankGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BankHolder holder)) return;

        // Bank画面を開いている間は、Shiftクリック・数字キー・ダブルクリックを含めて常に保護する。
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int topSize = event.getView().getTopInventory().getSize();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= topSize) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (holder.screen() == BankHolder.Screen.HISTORY) {
            if (slot == 49) bankGui.open(player);
            return;
        }
        if (slot == BankGui.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == BankGui.HISTORY) {
            bankGui.openHistory(player);
            return;
        }

        if (!isMoneyAction(slot)) return;
        if (!interactionGuard.tryAcquire(player.getUniqueId(), System.currentTimeMillis())) {
            player.sendMessage("§e操作が速すぎます。少し待ってからもう一度お試しください。");
            return;
        }

        EconomyResult result;
        if (slot == BankGui.UPGRADE) {
            result = economyService.upgradeBank(
                    player.getUniqueId(),
                    EconomyOperationIds.create("bank-upgrade", player.getUniqueId())
            );
            sendResult(player, result, "銀行をアップグレードしました。");
            bankGui.open(player);
            return;
        }

        long wallet = economyService.getWalletBalance(player.getUniqueId());
        BankAccount account = economyService.getBankAccount(player.getUniqueId());
        if (slot == BankGui.DEPOSIT_10) result = deposit(player, wallet / 10L);
        else if (slot == BankGui.DEPOSIT_50) result = deposit(player, wallet / 2L);
        else if (slot == BankGui.DEPOSIT_ALL) result = deposit(player, wallet);
        else if (slot == BankGui.WITHDRAW_10) result = withdraw(player, account.balance() / 10L);
        else if (slot == BankGui.WITHDRAW_50) result = withdraw(player, account.balance() / 2L);
        else if (slot == BankGui.WITHDRAW_ALL) result = withdraw(player, account.balance());
        else return;

        sendResult(player, result, "銀行取引が完了しました。");
        bankGui.open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BankHolder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        interactionGuard.clear(event.getPlayer().getUniqueId());
    }

    private boolean isMoneyAction(int slot) {
        return slot == BankGui.UPGRADE
                || slot == BankGui.DEPOSIT_10
                || slot == BankGui.DEPOSIT_50
                || slot == BankGui.DEPOSIT_ALL
                || slot == BankGui.WITHDRAW_10
                || slot == BankGui.WITHDRAW_50
                || slot == BankGui.WITHDRAW_ALL;
    }

    private EconomyResult deposit(Player player, long amount) {
        return economyService.depositBank(
                player.getUniqueId(),
                amount,
                EconomyOperationIds.create("bank-deposit", player.getUniqueId())
        );
    }

    private EconomyResult withdraw(Player player, long amount) {
        return economyService.withdrawBank(
                player.getUniqueId(),
                amount,
                EconomyOperationIds.create("bank-withdraw", player.getUniqueId())
        );
    }

    private void sendResult(Player player, EconomyResult result, String success) {
        if (result.success()) {
            player.sendMessage("§a" + success);
            return;
        }
        player.sendMessage("§c" + failureMessage(result.failure()));
    }

    private String failureMessage(EconomyFailure failure) {
        return switch (failure) {
            case INVALID_AMOUNT -> "取引金額が正しくありません。";
            case PLAYER_NOT_FOUND -> "プレイヤーデータが見つかりません。";
            case INSUFFICIENT_WALLET -> "手持ちコインが不足しています。";
            case INSUFFICIENT_BANK -> "銀行残高が不足しています。";
            case BANK_CAPACITY -> "銀行の預入上限を超えています。";
            case MAX_BANK_LEVEL -> "銀行は既に最大レベルです。";
            case DAILY_LIMIT -> "本日の取引上限に達しています。";
            case DUPLICATE_OPERATION -> "同じ取引が既に処理されています。";
            case DATABASE_ERROR -> "データベース処理に失敗しました。時間をおいて再度お試しください。";
            case NONE -> "処理できませんでした。";
        };
    }
}
