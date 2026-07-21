package me.lrg.skyblock.core.economy.listener;

import me.lrg.skyblock.core.economy.gui.BankGui;
import me.lrg.skyblock.core.economy.gui.BankHolder;
import me.lrg.skyblock.core.economy.model.BankAccount;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class BankListener implements Listener {
    private final EconomyService economyService;
    private final BankGui bankGui;

    public BankListener(EconomyService economyService, BankGui bankGui) {
        this.economyService = economyService;
        this.bankGui = bankGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BankHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

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
        if (slot == BankGui.UPGRADE) {
            EconomyResult result = economyService.upgradeBank(player.getUniqueId(),
                    EconomyOperationIds.create("bank-upgrade", player.getUniqueId()));
            sendResult(player, result, "銀行をアップグレードしました。");
            bankGui.open(player);
            return;
        }

        long wallet = economyService.getWalletBalance(player.getUniqueId());
        BankAccount account = economyService.getBankAccount(player.getUniqueId());
        EconomyResult result = null;
        if (slot == BankGui.DEPOSIT_10) result = deposit(player, wallet / 10L);
        else if (slot == BankGui.DEPOSIT_50) result = deposit(player, wallet / 2L);
        else if (slot == BankGui.DEPOSIT_ALL) result = deposit(player, wallet);
        else if (slot == BankGui.WITHDRAW_10) result = withdraw(player, account.balance() / 10L);
        else if (slot == BankGui.WITHDRAW_50) result = withdraw(player, account.balance() / 2L);
        else if (slot == BankGui.WITHDRAW_ALL) result = withdraw(player, account.balance());

        if (result != null) {
            sendResult(player, result, "銀行取引が完了しました。");
            bankGui.open(player);
        }
    }

    private EconomyResult deposit(Player player, long amount) {
        return economyService.depositBank(player.getUniqueId(), amount,
                EconomyOperationIds.create("bank-deposit", player.getUniqueId()));
    }

    private EconomyResult withdraw(Player player, long amount) {
        return economyService.withdrawBank(player.getUniqueId(), amount,
                EconomyOperationIds.create("bank-withdraw", player.getUniqueId()));
    }

    private void sendResult(Player player, EconomyResult result, String success) {
        if (result.success()) player.sendMessage("§a" + success);
        else player.sendMessage("§c処理できませんでした: " + result.failure().name());
    }
}
