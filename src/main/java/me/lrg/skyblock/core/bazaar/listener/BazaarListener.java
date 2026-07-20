package me.lrg.skyblock.core.bazaar.listener;

import me.lrg.skyblock.core.bazaar.config.BazaarMessages;
import me.lrg.skyblock.core.bazaar.gui.BazaarGui;
import me.lrg.skyblock.core.bazaar.gui.BazaarHolder;
import me.lrg.skyblock.core.bazaar.manager.BazaarManager;
import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;

public final class BazaarListener implements Listener {
    private final BazaarManager manager;
    private final BazaarGui gui;
    private final BazaarMessages messages;

    public BazaarListener(BazaarManager manager, BazaarGui gui, BazaarMessages messages) {
        this.manager = manager;
        this.gui = gui;
        this.messages = messages;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BazaarHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder.admin()) {
            if (slot == 49) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) {
                    player.sendMessage(messages.text("bazaar.messages.hold-item"));
                    return;
                }
                BazaarItem added = manager.addTemplate(held);
                player.sendMessage(messages.text("bazaar.messages.added", Map.of("id", added.id())));
                gui.openAdmin(player);
                return;
            }
            if (slot == 50) {
                manager.reload();
                gui.openAdmin(player);
                return;
            }
            BazaarItem item = itemAt(slot, new ArrayList<>(manager.allItems()));
            if (item == null) return;
            if (event.isShiftClick() && event.isRightClick()) {
                manager.delete(item);
                player.sendMessage(messages.text("bazaar.messages.deleted", Map.of("id", item.id())));
            } else if (event.isShiftClick() && event.isLeftClick()) {
                item.setSellPrice(item.sellPrice() + 1);
                manager.save(item);
            } else if (event.isLeftClick()) {
                item.setBuyPrice(item.buyPrice() + 1);
                manager.save(item);
            } else if (event.isRightClick()) {
                item.setBuyPrice(item.buyPrice() - 1);
                manager.save(item);
            }
            gui.openAdmin(player);
            return;
        }

        BazaarItem item = itemAt(slot, manager.enabledItems());
        if (item == null) return;
        int amount = event.isShiftClick() ? 64 : 1;
        if (event.isLeftClick()) {
            if (manager.buy(player, item, amount)) {
                player.sendMessage(messages.text("bazaar.messages.bought", Map.of("id", item.id(), "amount", Integer.toString(amount))));
            } else {
                player.sendMessage(messages.text("bazaar.messages.buy-failed"));
            }
        } else if (event.isRightClick()) {
            int sold = manager.sell(player, item, amount);
            if (sold > 0) {
                player.sendMessage(messages.text("bazaar.messages.sold", Map.of("id", item.id(), "amount", Integer.toString(sold))));
            } else {
                player.sendMessage(messages.text("bazaar.messages.sell-failed"));
            }
        }
        gui.openPlayer(player);
    }

    private BazaarItem itemAt(int slot, java.util.List<BazaarItem> items) {
        return slot >= 0 && slot < Math.min(45, items.size()) ? items.get(slot) : null;
    }
}
