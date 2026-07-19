package me.lrg.skyblock.core.wardrobe.listener;

import me.lrg.skyblock.core.wardrobe.gui.WardrobeGui;
import me.lrg.skyblock.core.wardrobe.gui.WardrobeHolder;
import me.lrg.skyblock.core.wardrobe.manager.WardrobeManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class WardrobeListener implements Listener {
    private final WardrobeManager wardrobeManager;
    private final WardrobeGui wardrobeGui;

    public WardrobeListener(WardrobeManager wardrobeManager, WardrobeGui wardrobeGui) {
        this.wardrobeManager = wardrobeManager;
        this.wardrobeGui = wardrobeGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WardrobeHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        int setSlot = wardrobeGui.toSetSlot(event.getRawSlot());
        if (setSlot == -1) {
            return;
        }

        if (event.isShiftClick()) {
            if (wardrobeManager.storeEquippedArmor(player, setSlot)) {
                player.sendMessage(ChatColor.GREEN + "✔ Wardrobe Set " + setSlot + " を保存しました。");
                wardrobeGui.refresh(player, event.getView().getTopInventory());
            } else {
                player.sendMessage(ChatColor.RED + "空のセットを選び、防具を装備した状態で保存してください。");
            }
            return;
        }

        WardrobeManager.SwapResult result = wardrobeManager.equipSet(player, setSlot);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatColor.GREEN + "✔ Wardrobe Set " + setSlot + " に着替えました。");
                wardrobeGui.refresh(player, event.getView().getTopInventory());
            }
            case EMPTY_SLOT -> player.sendMessage(ChatColor.RED + "このWardrobe Setは空です。Shift+クリックで保存できます。");
            case NOT_LOADED -> player.sendMessage(ChatColor.YELLOW + "Wardrobeデータを読み込み中です。");
        }
    }
}
