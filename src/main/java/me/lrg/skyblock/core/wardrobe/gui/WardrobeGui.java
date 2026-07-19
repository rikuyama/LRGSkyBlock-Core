package me.lrg.skyblock.core.wardrobe.gui;

import me.lrg.skyblock.core.wardrobe.manager.WardrobeManager;
import me.lrg.skyblock.core.wardrobe.model.WardrobeSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class WardrobeGui {
    public static final int SIZE = 27;
    public static final int FIRST_SET_SLOT = 9;

    private final WardrobeManager wardrobeManager;

    public WardrobeGui(WardrobeManager wardrobeManager) {
        this.wardrobeManager = wardrobeManager;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new WardrobeHolder(), SIZE, ChatColor.DARK_GRAY + "Wardrobe");
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int index = 0; index < SIZE; index++) {
            inventory.setItem(index, filler);
        }
        for (int setSlot = 1; setSlot <= WardrobeManager.SLOT_COUNT; setSlot++) {
            inventory.setItem(toInventorySlot(setSlot), createSetIcon(player, setSlot));
        }
        player.openInventory(inventory);
    }

    public void refresh(Player player, Inventory inventory) {
        for (int setSlot = 1; setSlot <= WardrobeManager.SLOT_COUNT; setSlot++) {
            inventory.setItem(toInventorySlot(setSlot), createSetIcon(player, setSlot));
        }
    }

    public int toSetSlot(int rawSlot) {
        int setSlot = rawSlot - FIRST_SET_SLOT + 1;
        return setSlot >= 1 && setSlot <= WardrobeManager.SLOT_COUNT ? setSlot : -1;
    }

    private int toInventorySlot(int setSlot) {
        return FIRST_SET_SLOT + setSlot - 1;
    }

    private ItemStack createSetIcon(Player player, int slot) {
        WardrobeSet set = wardrobeManager.getSet(player.getUniqueId(), slot).orElse(null);
        boolean occupied = set != null && !set.isEmpty();
        List<String> lore = new ArrayList<>();
        if (occupied) {
            lore.add(ChatColor.GRAY + "保存アイテム: " + ChatColor.WHITE + set.itemCount() + "/4");
            lore.add("");
            lore.add(ChatColor.YELLOW + "クリック: このセットと着替える");
        } else {
            lore.add(ChatColor.GRAY + "空のセットです。");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Shift+クリック: 現在の防具を保存");
        }
        Material material = occupied ? Material.ARMOR_STAND : Material.GRAY_DYE;
        return item(material, ChatColor.GOLD + "Wardrobe Set " + slot, lore);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
