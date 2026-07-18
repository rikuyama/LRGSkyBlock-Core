package me.lrg.skyblock.core.gui;

import me.lrg.skyblock.core.config.FortuneTargetSettings;
import me.lrg.skyblock.core.model.FortuneCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FortuneGui {
    public static final Component TITLE = Component.text("Fortuneカテゴリ設定", NamedTextColor.DARK_GREEN);
    private final FortuneTargetSettings settings;

    public FortuneGui(FortuneTargetSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        FortuneCategory[] categories = FortuneCategory.values();
        for (int i = 0; i < categories.length; i++) {
            inventory.setItem(10 + i, categoryItem(categories[i]));
        }
        inventory.setItem(22, infoItem(player));
        inventory.setItem(26, removeOverrideItem(player));
        player.openInventory(inventory);
    }

    public FortuneCategory categoryAt(int slot) {
        int index = slot - 10;
        FortuneCategory[] categories = FortuneCategory.values();
        return index >= 0 && index < categories.length ? categories[index] : null;
    }

    private ItemStack categoryItem(FortuneCategory category) {
        boolean enabled = settings.isCategoryEnabled(category);
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(category.getDisplayName(), enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("カテゴリID: " + category.name(), NamedTextColor.GRAY));
        lore.add(Component.text("状態: " + (enabled ? "有効" : "無効"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Component.empty());
        lore.add(Component.text("左クリック: 有効/無効を切替", NamedTextColor.YELLOW));
        lore.add(Component.text("右クリック: 手持ちブロックを登録", NamedTextColor.AQUA));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Material material = held == null ? Material.AIR : held.getType();
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("手持ちブロック情報", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Material: " + material.name(), NamedTextColor.GRAY));
        settings.resolveCategory(material).ifPresentOrElse(
                category -> lore.add(Component.text("現在カテゴリ: " + category.name(), NamedTextColor.GREEN)),
                () -> lore.add(Component.text("現在カテゴリ: 対象外", NamedTextColor.RED))
        );
        lore.add(Component.text("例外登録: " + (settings.getCategoryOverride(material).isPresent() ? "あり" : "なし"), NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack removeOverrideItem(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("手持ちブロックの例外を削除", NamedTextColor.RED));
        Material material = player.getInventory().getItemInMainHand().getType();
        meta.lore(List.of(
                Component.text(material.name(), NamedTextColor.GRAY),
                Component.text("クリックで自動分類へ戻す", NamedTextColor.YELLOW)
        ));
        item.setItemMeta(meta);
        return item;
    }
}
