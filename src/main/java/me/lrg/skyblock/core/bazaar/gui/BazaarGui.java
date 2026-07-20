package me.lrg.skyblock.core.bazaar.gui;

import me.lrg.skyblock.core.bazaar.config.BazaarMessages;
import me.lrg.skyblock.core.bazaar.manager.BazaarManager;
import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BazaarGui {
    private final BazaarManager manager;
    private final BazaarMessages messages;

    public BazaarGui(BazaarManager manager, BazaarMessages messages) {
        this.manager = manager;
        this.messages = messages;
    }

    public void openPlayer(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(false), 54, messages.text("bazaar.gui.player-title"));
        int slot = 0;
        for (BazaarItem item : manager.enabledItems()) {
            if (slot >= 45) break;
            inventory.setItem(slot++, display(item, false));
        }
        inventory.setItem(49, named(Material.EMERALD,
                messages.text("bazaar.gui.controls-name"),
                List.of(messages.text("bazaar.gui.controls-lore"))));
        player.openInventory(inventory);
    }

    public void openAdmin(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(true), 54, messages.text("bazaar.gui.admin-title"));
        int slot = 0;
        for (BazaarItem item : manager.allItems()) {
            if (slot >= 45) break;
            inventory.setItem(slot++, display(item, true));
        }
        inventory.setItem(49, named(Material.LIME_DYE,
                messages.text("bazaar.gui.register-name"),
                List.of(messages.text("bazaar.gui.register-lore-1"), messages.text("bazaar.gui.register-lore-2"))));
        inventory.setItem(50, named(Material.CLOCK,
                messages.text("bazaar.gui.reload-name"),
                List.of(messages.text("bazaar.gui.reload-lore"))));
        player.openInventory(inventory);
    }

    private ItemStack display(BazaarItem item, boolean admin) {
        ItemStack stack = item.template();
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(messages.text("bazaar.gui.item-id", Map.of("id", item.id())));
        lore.add(messages.text("bazaar.gui.category", Map.of("category", messages.category(item.category()))));
        lore.add(messages.text("bazaar.gui.buy-price", Map.of("price", Long.toString(item.buyPrice()))));
        lore.add(messages.text("bazaar.gui.sell-price", Map.of("price", Long.toString(item.sellPrice()))));
        lore.add("");
        if (admin) {
            lore.add(messages.text("bazaar.gui.admin-buy-up"));
            lore.add(messages.text("bazaar.gui.admin-buy-down"));
            lore.add(messages.text("bazaar.gui.admin-sell-up"));
            lore.add(messages.text("bazaar.gui.admin-delete"));
            lore.add(messages.text(item.enabled() ? "bazaar.gui.enabled" : "bazaar.gui.disabled"));
        } else {
            lore.add(messages.text("bazaar.gui.player-buy"));
            lore.add(messages.text("bazaar.gui.player-sell"));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
