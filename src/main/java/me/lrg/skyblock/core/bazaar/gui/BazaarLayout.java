package me.lrg.skyblock.core.bazaar.gui;

import java.util.List;
import java.util.Map;

/** Fixed 6-row slot map matching the current Bazaar interaction layout. */
public final class BazaarLayout {
    private BazaarLayout() {
    }

    public static final Map<Integer, String> CATEGORY_BY_SLOT = Map.of(
            0, "FARMING",
            9, "MINING",
            18, "COMBAT",
            27, "FORAGING",
            36, "OTHER"
    );

    public static final List<Integer> PRODUCT_SLOTS = List.of(
            11, 12, 13, 14, 15, 16,
            20, 21, 22, 23, 24, 25,
            29, 30, 31, 32, 33, 34,
            38, 39, 40, 41, 42, 43
    );

    public static final int SEARCH = 45;
    public static final int SELL_INVENTORY = 47;
    public static final int SELL_SACKS = 48;
    public static final int CLOSE = 49;
    public static final int MANAGE_ORDERS = 50;
    public static final int VIEW_MODE = 52;

    public static final int INSTANT_BUY = 11;
    public static final int PRODUCT_CENTER = 13;
    public static final int INSTANT_SELL = 15;
    public static final int CREATE_BUY_ORDER = 29;
    public static final int CREATE_SELL_OFFER = 33;
    public static final int BACK_LEFT = 45;
    public static final int BACK = 46;
    public static final int VIEW_GRAPHS = 51;
    public static final int INSTA_SELL_IGNORE = 52;

    public static final int QUANTITY_ONE = 11;
    public static final int QUANTITY_STACK = 13;
    public static final int QUANTITY_FILL = 15;
    public static final int QUANTITY_CUSTOM = 31;

    public static final int ORDER_AMOUNT_4 = 10;
    public static final int ORDER_AMOUNT_10 = 12;
    public static final int ORDER_AMOUNT_64 = 14;
    public static final int ORDER_AMOUNT_CUSTOM = 16;

    public static final int PRICE_TOP = 10;
    public static final int PRICE_IMPROVE = 12;
    public static final int PRICE_SPREAD = 14;
    public static final int PRICE_CUSTOM = 16;
}
