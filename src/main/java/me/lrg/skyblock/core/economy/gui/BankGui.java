package me.lrg.skyblock.core.economy.gui;

import me.lrg.skyblock.core.economy.config.EconomySettings;
import me.lrg.skyblock.core.economy.model.BankAccount;
import me.lrg.skyblock.core.economy.model.BankLevel;
import me.lrg.skyblock.core.economy.model.EconomyTransaction;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class BankGui {
    public static final int DEPOSIT_10 = 20;
    public static final int DEPOSIT_50 = 21;
    public static final int DEPOSIT_ALL = 22;
    public static final int WITHDRAW_10 = 24;
    public static final int WITHDRAW_50 = 25;
    public static final int WITHDRAW_ALL = 26;
    public static final int UPGRADE = 31;
    public static final int HISTORY = 32;
    public static final int CLOSE = 49;

    private final EconomyService economyService;

    public BankGui(EconomyService economyService) {
        this.economyService = economyService;
    }

    public void open(Player player) {
        BankAccount account = economyService.getBankAccount(player.getUniqueId());
        EconomySettings settings = economyService.settings();
        BankLevel level = settings.level(account.level());
        long wallet = economyService.getWalletBalance(player.getUniqueId());
        BankLevel next = settings.nextLevel(account.level());

        Inventory inventory = Bukkit.createInventory(new BankHolder(BankHolder.Screen.HOME), 54, "§8Bank");
        fill(inventory);
        inventory.setItem(13, item(Material.GOLD_BLOCK, "§6銀行口座", List.of(
                "§7Wallet: §6" + format(wallet) + " Coins",
                "§7Bank: §6" + format(account.balance()) + " Coins",
                "§7Capacity: §e" + format(level.capacity()),
                "§7Level: §b" + account.level(),
                "",
                "§7利息: §a毎日 " + String.format("%02d:%02d", settings.interestHour(), settings.interestMinute())
                        + " に " + formatPercent(settings.interestRate())
        )));
        inventory.setItem(DEPOSIT_10, amountButton(Material.LIME_DYE, "§a10%を預ける", wallet / 10L));
        inventory.setItem(DEPOSIT_50, amountButton(Material.LIME_DYE, "§a50%を預ける", wallet / 2L));
        inventory.setItem(DEPOSIT_ALL, amountButton(Material.EMERALD, "§a全額預ける", wallet));
        inventory.setItem(WITHDRAW_10, amountButton(Material.RED_DYE, "§c10%を引き出す", account.balance() / 10L));
        inventory.setItem(WITHDRAW_50, amountButton(Material.RED_DYE, "§c50%を引き出す", account.balance() / 2L));
        inventory.setItem(WITHDRAW_ALL, amountButton(Material.REDSTONE, "§c全額引き出す", account.balance()));
        inventory.setItem(UPGRADE, item(Material.NETHER_STAR,
                next == null ? "§c最大銀行レベル" : "§e銀行をレベル " + next.level() + " へアップグレード",
                next == null ? List.of("§7これ以上アップグレードできません。") : List.of(
                        "§7費用: §6" + format(next.upgradeCost()) + " Coins",
                        "§7新しい上限: §e" + format(next.capacity()),
                        "", "§eクリックして購入"
                )));
        inventory.setItem(HISTORY, item(Material.WRITABLE_BOOK, "§e取引履歴", List.of("§7最近の入出金を確認します。", "", "§eクリックして開く")));
        inventory.setItem(CLOSE, item(Material.BARRIER, "§c閉じる", List.of()));
        player.openInventory(inventory);
    }

    public void openHistory(Player player) {
        Inventory inventory = Bukkit.createInventory(new BankHolder(BankHolder.Screen.HISTORY), 54, "§8Bank History");
        fill(inventory);
        List<EconomyTransaction> history = economyService.history(player.getUniqueId(), 28);
        int slot = 10;
        for (EconomyTransaction transaction : history) {
            if (slot >= 44) break;
            if (slot % 9 == 8) slot += 2;
            boolean incoming = player.getUniqueId().equals(transaction.target());
            Material material = incoming ? Material.LIME_DYE : Material.RED_DYE;
            String sign = incoming ? "+" : "-";
            List<String> lore = new ArrayList<>();
            lore.add("§7種類: §f" + transaction.type());
            lore.add("§7金額: " + (incoming ? "§a" : "§c") + sign + format(transaction.amount()));
            if (transaction.fee() > 0L) lore.add("§7手数料: §c" + format(transaction.fee()));
            lore.add("§7理由: §f" + transaction.reason());
            ZoneId zone = economyService.settings().zoneId();
            lore.add("§8" + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(zone).format(transaction.createdAt()));
            inventory.setItem(slot++, item(material, incoming ? "§a入金" : "§c出金", lore));
        }
        if (history.isEmpty()) inventory.setItem(22, item(Material.PAPER, "§7履歴はありません", List.of()));
        inventory.setItem(49, item(Material.ARROW, "§a戻る", List.of()));
        player.openInventory(inventory);
    }

    private ItemStack amountButton(Material material, String name, long amount) {
        return item(material, name, List.of("§7金額: §6" + format(Math.max(0L, amount)), "", "§eクリックして実行"));
    }

    private void fill(Inventory inventory) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String formatPercent(double rate) {
        return String.format("%.2f%%", rate * 100.0D).replace(".00%", "%");
    }

    public static String format(long amount) {
        return String.format("%,d", amount);
    }
}
