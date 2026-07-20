package me.lrg.skyblock.core.bazaar.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BazaarHolder implements InventoryHolder {
    public enum Screen { HOME, PRODUCTS, DETAIL, QUANTITY, CONFIRM, ADMIN }
    public enum Action { BUY, SELL }

    private final Screen screen;
    private final String category;
    private final String itemId;
    private final int page;
    private final String query;
    private final Action action;
    private final int amount;

    public BazaarHolder(Screen screen) {
        this(screen, null, null, 0, "", null, 0);
    }

    public BazaarHolder(Screen screen, String category, String itemId, int page, String query, Action action, int amount) {
        this.screen = screen;
        this.category = category;
        this.itemId = itemId;
        this.page = Math.max(0, page);
        this.query = query == null ? "" : query;
        this.action = action;
        this.amount = Math.max(0, amount);
    }

    public Screen screen() { return screen; }
    public String category() { return category; }
    public String itemId() { return itemId; }
    public int page() { return page; }
    public String query() { return query; }
    public Action action() { return action; }
    public int amount() { return amount; }
    public boolean admin() { return screen == Screen.ADMIN; }
    @Override public Inventory getInventory() { return null; }
}
