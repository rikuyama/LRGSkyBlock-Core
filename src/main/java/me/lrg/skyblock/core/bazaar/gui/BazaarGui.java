package me.lrg.skyblock.core.bazaar.gui;

import me.lrg.skyblock.core.bazaar.config.BazaarMessages;
import me.lrg.skyblock.core.bazaar.manager.BazaarManager;
import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import me.lrg.skyblock.core.bazaar.order.model.BazaarItemClaim;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrder;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderSide;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class BazaarGui {
    public static final List<Integer> PRODUCT_SLOTS = List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43);
    private static final List<String> CATEGORIES = List.of("FARMING", "MINING", "COMBAT", "FORAGING", "OTHER");
    private static final List<Integer> CATEGORY_SLOTS = List.of(11,12,13,14,15);
    private final BazaarManager manager;
    private final BazaarMessages messages;

    public BazaarGui(BazaarManager manager, BazaarMessages messages) { this.manager = manager; this.messages = messages; }
    public void openPlayer(Player player) { openHome(player); }

    public void openHome(Player player) {
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.HOME), 54, messages.text("bazaar.gui.home-title"));
        fillBorder(inv);
        for (int i = 0; i < CATEGORIES.size(); i++) inv.setItem(CATEGORY_SLOTS.get(i), categoryIcon(CATEGORIES.get(i)));
        inv.setItem(31, named(Material.CHEST, messages.text("bazaar.gui.all-products"), List.of(messages.text("bazaar.gui.click-open"))));
        inv.setItem(45, named(Material.OAK_SIGN, messages.text("bazaar.gui.search-name"), List.of(messages.text("bazaar.gui.search-lore"))));
        inv.setItem(47, named(Material.BOOK, messages.text("bazaar.gui.my-orders"), List.of(messages.text("bazaar.gui.click-open"))));
        inv.setItem(51, named(Material.ENDER_CHEST, messages.text("bazaar.gui.claims"), List.of(messages.text("bazaar.gui.claim-summary", Map.of("coins", Long.toString(manager.claimableCoins(player.getUniqueId())), "items", Integer.toString(manager.itemClaims(player.getUniqueId()).size()))), messages.text("bazaar.gui.click-open"))));
        inv.setItem(49, named(Material.BARRIER, messages.text("bazaar.gui.close"), List.of()));
        player.openInventory(inv);
    }

    public void openProducts(Player player, String category, int page, String query) {
        List<BazaarItem> filtered = filtered(category, query);
        int pages = Math.max(1, (filtered.size() + PRODUCT_SLOTS.size() - 1) / PRODUCT_SLOTS.size());
        int safePage = Math.min(Math.max(0, page), pages - 1);
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.PRODUCTS, category, null, safePage, query, null, 0), 54,
                messages.text("bazaar.gui.products-title", Map.of("category", category == null ? messages.text("bazaar.gui.all-products") : messages.category(category), "page", Integer.toString(safePage + 1), "pages", Integer.toString(pages))));
        fillBorder(inv);
        int start = safePage * PRODUCT_SLOTS.size();
        for (int i = 0; i < PRODUCT_SLOTS.size() && start + i < filtered.size(); i++) inv.setItem(PRODUCT_SLOTS.get(i), productDisplay(filtered.get(start + i)));
        if (safePage > 0) inv.setItem(45, named(Material.ARROW, messages.text("bazaar.gui.previous-page"), List.of()));
        inv.setItem(46, named(Material.OAK_SIGN, messages.text("bazaar.gui.search-name"), List.of(query.isBlank() ? messages.text("bazaar.gui.search-lore") : messages.text("bazaar.gui.search-current", Map.of("query", query)))));
        inv.setItem(49, named(Material.ARROW, messages.text("bazaar.gui.back"), List.of()));
        if (safePage + 1 < pages) inv.setItem(53, named(Material.ARROW, messages.text("bazaar.gui.next-page"), List.of()));
        player.openInventory(inv);
    }

    public void openDetail(Player player, BazaarItem item, String category, int page, String query) {
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.DETAIL, category, item.id(), page, query, null, 0), 36,
                messages.text("bazaar.gui.detail-title", Map.of("item", displayName(item.template()))));
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(13, productDisplay(item));
        inv.setItem(10, action(Material.EMERALD_BLOCK, "bazaar.gui.instant-buy", manager.bestSellPrice(item.id()).orElse(0), manager.availableVolume(item.id(), BazaarOrderSide.SELL)));
        inv.setItem(16, action(Material.GOLD_BLOCK, "bazaar.gui.instant-sell", manager.bestBuyPrice(item.id()).orElse(0), manager.availableVolume(item.id(), BazaarOrderSide.BUY)));
        inv.setItem(20, named(Material.LIME_DYE, messages.text("bazaar.gui.create-buy-order"), List.of(messages.text("bazaar.gui.order-input-lore"), messages.text("bazaar.gui.click-open"))));
        inv.setItem(24, named(Material.ORANGE_DYE, messages.text("bazaar.gui.create-sell-order"), List.of(messages.text("bazaar.gui.owned", Map.of("amount", Integer.toString(manager.countMatching(player, item.template())))), messages.text("bazaar.gui.order-input-lore"), messages.text("bazaar.gui.click-open"))));
        inv.setItem(31, named(Material.ARROW, messages.text("bazaar.gui.back"), List.of()));
        player.openInventory(inv);
    }

    public void openQuantity(Player player, BazaarItem item, BazaarHolder.Action action, String category, int page, String query) {
        String titleKey = action == BazaarHolder.Action.INSTANT_BUY ? "bazaar.gui.buy-title" : "bazaar.gui.sell-title";
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.QUANTITY, category, item.id(), page, query, action, 0), 45,
                messages.text(titleKey, Map.of("item", displayName(item.template()))));
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        int[] amounts = {1,16,64}; int[] slots = {11,13,15};
        for (int i = 0; i < amounts.length; i++) inv.setItem(slots[i], quantityIcon(action, amounts[i]));
        int max = action == BazaarHolder.Action.INSTANT_BUY ? manager.maxInstantBuy(player, item) : Math.min(manager.countMatching(player, item.template()), manager.availableVolume(item.id(), BazaarOrderSide.BUY));
        inv.setItem(31, quantityIcon(action, Math.max(1, max), messages.text(action == BazaarHolder.Action.INSTANT_BUY ? "bazaar.gui.maximum-buy" : "bazaar.gui.sell-all")));
        inv.setItem(40, named(Material.ARROW, messages.text("bazaar.gui.back"), List.of()));
        player.openInventory(inv);
    }

    public void openConfirm(Player player, BazaarItem item, BazaarHolder.Action action, int amount, String category, int page, String query) {
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.CONFIRM, category, item.id(), page, query, action, amount), 27, messages.text("bazaar.gui.confirm-title"));
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(11, named(Material.LIME_TERRACOTTA, messages.text("bazaar.gui.confirm"), List.of(messages.text("bazaar.gui.amount", Map.of("amount", Integer.toString(amount))), messages.text("bazaar.gui.market-order-warning"))));
        ItemStack center = item.template(); center.setAmount(Math.min(center.getMaxStackSize(), Math.max(1, amount))); inv.setItem(13, center);
        inv.setItem(15, named(Material.RED_TERRACOTTA, messages.text("bazaar.gui.cancel"), List.of()));
        player.openInventory(inv);
    }

    public void openMyOrders(Player player) {
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.MY_ORDERS), 54, messages.text("bazaar.gui.my-orders-title"));
        List<BazaarOrder> orders = manager.playerOrders(player.getUniqueId());
        for (int i = 0; i < Math.min(45, orders.size()); i++) {
            BazaarOrder order = orders.get(i); BazaarItem item = manager.find(order.itemId()).orElse(null); if (item == null) continue;
            ItemStack stack = item.template(); ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(BazaarMessages.color((order.side() == BazaarOrderSide.BUY ? "&a買い注文: " : "&6売り注文: ") + displayName(stack)));
            meta.setLore(List.of(messages.text("bazaar.gui.order-id", Map.of("id", Long.toString(order.id()))), messages.text("bazaar.gui.order-price", Map.of("price", Long.toString(order.price()))), messages.text("bazaar.gui.order-remaining", Map.of("amount", Integer.toString(order.remainingAmount()))), messages.text("bazaar.gui.click-cancel-order")));
            stack.setItemMeta(meta); inv.setItem(i, stack);
        }
        inv.setItem(49, named(Material.ARROW, messages.text("bazaar.gui.back"), List.of()));
        player.openInventory(inv);
    }

    public void openClaims(Player player) {
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.CLAIMS), 54, messages.text("bazaar.gui.claims-title"));
        long coins = manager.claimableCoins(player.getUniqueId());
        inv.setItem(4, named(Material.GOLD_INGOT, messages.text("bazaar.gui.claim-coins"), List.of(messages.text("bazaar.gui.claim-coins-amount", Map.of("coins", Long.toString(coins))), messages.text("bazaar.gui.click-claim"))));
        List<BazaarItemClaim> claims = manager.itemClaims(player.getUniqueId());
        for (int i = 0; i < Math.min(45, claims.size()); i++) {
            BazaarItemClaim claim = claims.get(i); BazaarItem item = manager.find(claim.itemId()).orElse(null); if (item == null) continue;
            ItemStack stack = item.template(); ItemMeta meta = stack.getItemMeta();
            meta.setLore(List.of(messages.text("bazaar.gui.claim-item-amount", Map.of("amount", Long.toString(claim.amount()))), messages.text("bazaar.gui.click-claim"))); stack.setItemMeta(meta); inv.setItem(9 + i, stack);
        }
        inv.setItem(49, named(Material.ARROW, messages.text("bazaar.gui.back"), List.of()));
        player.openInventory(inv);
    }

    public void openAdmin(Player player) {
        Inventory inv = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.ADMIN), 54, messages.text("bazaar.gui.admin-title"));
        int slot = 0; for (BazaarItem item : manager.allItems()) { if (slot >= 45) break; inv.setItem(slot++, adminDisplay(item)); }
        inv.setItem(49, named(Material.LIME_DYE, messages.text("bazaar.gui.register-name"), List.of(messages.text("bazaar.gui.register-lore-1"), messages.text("bazaar.gui.register-lore-2"))));
        inv.setItem(50, named(Material.CLOCK, messages.text("bazaar.gui.reload-name"), List.of(messages.text("bazaar.gui.reload-lore"))));
        player.openInventory(inv);
    }

    public List<BazaarItem> filtered(String category, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        return manager.enabledItems().stream().filter(item -> category == null || category.equalsIgnoreCase(normalizeCategory(item.category())))
                .filter(item -> q.isBlank() || item.id().toLowerCase(Locale.ROOT).contains(q) || displayName(item.template()).toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    private ItemStack action(Material material, String nameKey, long price, int volume) {
        return named(material, messages.text(nameKey), List.of(messages.text("bazaar.gui.best-price", Map.of("price", price <= 0 ? "-" : Long.toString(price))), messages.text("bazaar.gui.market-volume", Map.of("amount", Integer.toString(volume))), messages.text("bazaar.gui.click-open")));
    }
    private ItemStack categoryIcon(String category) { Material material = switch (category) { case "FARMING" -> Material.WHEAT; case "MINING" -> Material.DIAMOND_PICKAXE; case "COMBAT" -> Material.IRON_SWORD; case "FORAGING" -> Material.OAK_LOG; default -> Material.CHEST; }; return named(material, "&a" + messages.category(category), List.of(messages.text("bazaar.gui.product-count", Map.of("count", Long.toString(filtered(category, "").size()))), messages.text("bazaar.gui.click-open"))); }
    private ItemStack productDisplay(BazaarItem item) { ItemStack stack = item.template(); ItemMeta meta = stack.getItemMeta(); List<String> lore = new ArrayList<>(meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : List.of()); if (!lore.isEmpty()) lore.add(""); lore.add(messages.text("bazaar.gui.best-buy", Map.of("price", price(manager.bestBuyPrice(item.id()))))); lore.add(messages.text("bazaar.gui.best-sell", Map.of("price", price(manager.bestSellPrice(item.id()))))); lore.add(""); lore.add(messages.text("bazaar.gui.click-view")); meta.setLore(lore); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); stack.setItemMeta(meta); return stack; }
    private ItemStack adminDisplay(BazaarItem item) { ItemStack stack = productDisplay(item); ItemMeta meta = stack.getItemMeta(); List<String> lore = new ArrayList<>(Objects.requireNonNullElse(meta.getLore(), List.of())); lore.add(messages.text("bazaar.gui.item-id", Map.of("id", item.id()))); lore.add(messages.text("bazaar.gui.category", Map.of("category", messages.category(item.category())))); lore.add(messages.text("bazaar.gui.admin-buy-up")); lore.add(messages.text("bazaar.gui.admin-buy-down")); lore.add(messages.text("bazaar.gui.admin-sell-up")); lore.add(messages.text("bazaar.gui.admin-delete")); lore.add(messages.text(item.enabled() ? "bazaar.gui.enabled" : "bazaar.gui.disabled")); meta.setLore(lore); stack.setItemMeta(meta); return stack; }
    private ItemStack quantityIcon(BazaarHolder.Action action, int amount) { return quantityIcon(action, amount, messages.text("bazaar.gui.quantity", Map.of("amount", Integer.toString(amount)))); }
    private ItemStack quantityIcon(BazaarHolder.Action action, int amount, String name) { return named(action == BazaarHolder.Action.INSTANT_BUY ? Material.EMERALD : Material.GOLD_INGOT, name, List.of(messages.text("bazaar.gui.click-select"))); }
    private String price(OptionalLong price) { return price.isPresent() ? Long.toString(price.getAsLong()) : "-"; }
    private String normalizeCategory(String category) { if (category == null) return "OTHER"; return switch (category.toUpperCase(Locale.ROOT)) { case "FISHING" -> "FORAGING"; case "NETHER" -> "COMBAT"; default -> category.toUpperCase(Locale.ROOT); }; }
    private String displayName(ItemStack stack) { ItemMeta meta = stack.getItemMeta(); return meta.hasDisplayName() ? meta.getDisplayName() : stack.getType().getKey().getKey().replace('_', ' '); }
    private void fillBorder(Inventory inv) { ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()); for (int i = 0; i < inv.getSize(); i++) if (i < 9 || i >= inv.getSize() - 9 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, pane); }
    private void fill(Inventory inv, Material material) { ItemStack pane = named(material, " ", List.of()); for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane); }
    private ItemStack named(Material material, String name, List<String> lore) { ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta(); meta.setDisplayName(BazaarMessages.color(name)); meta.setLore(lore.stream().map(BazaarMessages::color).toList()); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); stack.setItemMeta(meta); return stack; }
}
