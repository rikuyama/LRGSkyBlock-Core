package me.lrg.skyblock.core.bazaar.listener;

import me.lrg.skyblock.core.bazaar.config.BazaarMessages;
import me.lrg.skyblock.core.bazaar.gui.BazaarGui;
import me.lrg.skyblock.core.bazaar.gui.BazaarHolder;
import me.lrg.skyblock.core.bazaar.manager.BazaarManager;
import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BazaarListener implements Listener {
    private final JavaPlugin plugin;
    private final BazaarManager manager;
    private final BazaarGui gui;
    private final BazaarMessages messages;
    private final Set<UUID> awaitingSearch = ConcurrentHashMap.newKeySet();

    public BazaarListener(JavaPlugin plugin, BazaarManager manager, BazaarGui gui, BazaarMessages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
        this.messages = messages;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BazaarHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.screen()) {
            case HOME -> clickHome(player, slot);
            case PRODUCTS -> clickProducts(player, holder, slot);
            case DETAIL -> clickDetail(player, holder, slot);
            case QUANTITY -> clickQuantity(player, holder, slot);
            case CONFIRM -> clickConfirm(player, holder, slot);
            case ADMIN -> clickAdmin(player, event, slot);
        }
    }

    private void clickHome(Player player, int slot) {
        Map<Integer, String> categories = Map.of(11, "FARMING", 12, "MINING", 13, "COMBAT", 14, "FORAGING", 15, "OTHER");
        if (categories.containsKey(slot)) gui.openProducts(player, categories.get(slot), 0, "");
        else if (slot == 31) gui.openProducts(player, null, 0, "");
        else if (slot == 45) beginSearch(player);
        else if (slot == 49) player.closeInventory();
    }

    private void clickProducts(Player player, BazaarHolder holder, int slot) {
        if (slot == 45 && holder.page() > 0) { gui.openProducts(player, holder.category(), holder.page() - 1, holder.query()); return; }
        if (slot == 46) { beginSearch(player); return; }
        if (slot == 49) { gui.openHome(player); return; }
        if (slot == 53) { gui.openProducts(player, holder.category(), holder.page() + 1, holder.query()); return; }
        int index = BazaarGui.PRODUCT_SLOTS.indexOf(slot);
        if (index < 0) return;
        List<BazaarItem> items = gui.filtered(holder.category(), holder.query());
        int absolute = holder.page() * BazaarGui.PRODUCT_SLOTS.size() + index;
        if (absolute < items.size()) gui.openDetail(player, items.get(absolute), holder.category(), holder.page(), holder.query());
    }

    private void clickDetail(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) { gui.openHome(player); return; }
        if (slot == 11) gui.openQuantity(player, item, BazaarHolder.Action.BUY, holder.category(), holder.page(), holder.query());
        else if (slot == 15) gui.openQuantity(player, item, BazaarHolder.Action.SELL, holder.category(), holder.page(), holder.query());
        else if (slot == 22) gui.openProducts(player, holder.category(), holder.page(), holder.query());
    }

    private void clickQuantity(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) { gui.openHome(player); return; }
        int amount = switch (slot) {
            case 11 -> 1;
            case 13 -> 16;
            case 15 -> 64;
            case 31 -> holder.action() == BazaarHolder.Action.BUY ? manager.maxAffordable(player, item) : manager.countMatching(player, item.template());
            default -> 0;
        };
        if (slot == 40) { gui.openDetail(player, item, holder.category(), holder.page(), holder.query()); return; }
        if (amount <= 0) { player.sendMessage(messages.text(holder.action() == BazaarHolder.Action.BUY ? "bazaar.messages.buy-failed" : "bazaar.messages.sell-failed")); return; }
        gui.openConfirm(player, item, holder.action(), amount, holder.category(), holder.page(), holder.query());
    }

    private void clickConfirm(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) { gui.openHome(player); return; }
        if (slot == 15) { gui.openQuantity(player, item, holder.action(), holder.category(), holder.page(), holder.query()); return; }
        if (slot != 11) return;
        if (holder.action() == BazaarHolder.Action.BUY) {
            if (manager.buy(player, item, holder.amount())) player.sendMessage(messages.text("bazaar.messages.bought", Map.of("id", item.id(), "amount", Integer.toString(holder.amount()))));
            else player.sendMessage(messages.text("bazaar.messages.buy-failed"));
        } else {
            int sold = manager.sell(player, item, holder.amount());
            if (sold > 0) player.sendMessage(messages.text("bazaar.messages.sold", Map.of("id", item.id(), "amount", Integer.toString(sold))));
            else player.sendMessage(messages.text("bazaar.messages.sell-failed"));
        }
        gui.openDetail(player, item, holder.category(), holder.page(), holder.query());
    }

    private void clickAdmin(Player player, InventoryClickEvent event, int slot) {
        if (slot == 49) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR) { player.sendMessage(messages.text("bazaar.messages.hold-item")); return; }
            BazaarItem added = manager.addTemplate(held);
            player.sendMessage(messages.text("bazaar.messages.added", Map.of("id", added.id())));
            gui.openAdmin(player); return;
        }
        if (slot == 50) { manager.reload(); gui.openAdmin(player); return; }
        List<BazaarItem> items = new ArrayList<>(manager.allItems());
        if (slot < 0 || slot >= Math.min(45, items.size())) return;
        BazaarItem item = items.get(slot);
        if (event.isShiftClick() && event.isRightClick()) { manager.delete(item); player.sendMessage(messages.text("bazaar.messages.deleted", Map.of("id", item.id()))); }
        else if (event.isShiftClick() && event.isLeftClick()) { item.setSellPrice(item.sellPrice() + 1); manager.save(item); }
        else if (event.isLeftClick()) { item.setBuyPrice(item.buyPrice() + 1); manager.save(item); }
        else if (event.isRightClick()) { item.setBuyPrice(item.buyPrice() - 1); manager.save(item); }
        gui.openAdmin(player);
    }

    private void beginSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(messages.text("bazaar.messages.search-prompt"));
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingSearch.remove(player.getUniqueId())) return;
        event.setCancelled(true);
        String query = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (query.equalsIgnoreCase("cancel") || query.equalsIgnoreCase("キャンセル")) {
                player.sendMessage(messages.text("bazaar.messages.search-cancelled"));
                gui.openHome(player);
                return;
            }
            player.sendMessage(messages.text("bazaar.messages.search-result", Map.of("query", query)));
            gui.openProducts(player, null, 0, query);
        });
    }
}
