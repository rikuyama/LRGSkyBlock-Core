package me.lrg.skyblock.core.bazaar.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BazaarHolder implements InventoryHolder {
    public enum Screen {
        PRODUCTS,
        DETAIL,
        QUANTITY,
        ORDER_AMOUNT,
        PRICE,
        CONFIRM,
        MY_ORDERS,
        CLAIMS,
        HISTORY,
        SETTINGS,
        GRAPHS,
        ADMIN
    }

    public enum Action { INSTANT_BUY, INSTANT_SELL, CREATE_BUY, CREATE_SELL }

    private final Screen screen;
    private final String category;
    private final String itemId;
    private final int page;
    private final String query;
    private final Action action;
    private final int amount;
    private final long price;
    private final long orderId;

    public BazaarHolder(Screen screen) {
        this(screen, "FARMING", null, 0, "", null, 0, 0L, 0L);
    }

    public BazaarHolder(
            Screen screen,
            String category,
            String itemId,
            int page,
            String query,
            Action action,
            int amount,
            long price,
            long orderId
    ) {
        this.screen = screen;
        this.category = category == null ? "FARMING" : category;
        this.itemId = itemId;
        this.page = Math.max(0, page);
        this.query = query == null ? "" : query;
        this.action = action;
        this.amount = Math.max(0, amount);
        this.price = Math.max(0L, price);
        this.orderId = Math.max(0L, orderId);
    }

    public Screen screen() { return screen; }
    public String category() { return category; }
    public String itemId() { return itemId; }
    public int page() { return page; }
    public String query() { return query; }
    public Action action() { return action; }
    public int amount() { return amount; }
    public long price() { return price; }
    public long orderId() { return orderId; }

    @Override
    public Inventory getInventory() { return null; }
}
