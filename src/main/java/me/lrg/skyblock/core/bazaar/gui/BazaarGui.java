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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BazaarGui {
    public static final List<Integer> PRODUCT_SLOTS = BazaarLayout.PRODUCT_SLOTS;

    private final BazaarManager manager;
    private final BazaarMessages messages;
    private final ConcurrentMap<UUID, Boolean> advancedModeByPlayer = new ConcurrentHashMap<>();

    public BazaarGui(BazaarManager manager, BazaarMessages messages) {
        this.manager = manager;
        this.messages = messages;
    }

    public void openPlayer(Player player) {
        openProducts(player, "FARMING", 0, "");
    }

    public void openHome(Player player) {
        openProducts(player, "FARMING", 0, "");
    }

    public void openProducts(Player player, String category, int page, String query) {
        String selectedCategory = normalizeCategory(category);
        List<BazaarItem> products = filtered(selectedCategory, query);
        int pages = Math.max(1, (products.size() + PRODUCT_SLOTS.size() - 1) / PRODUCT_SLOTS.size());
        int safePage = Math.min(Math.max(0, page), pages - 1);
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.PRODUCTS, selectedCategory, null,
                safePage, query, null, 0, 0L, 0L);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                "§8Bazaar ➜ " + categoryName(selectedCategory));
        fill(inventory, paneFor(selectedCategory));

        for (Map.Entry<Integer, String> entry : BazaarLayout.CATEGORY_BY_SLOT.entrySet()) {
            inventory.setItem(entry.getKey(), categoryIcon(entry.getValue(), selectedCategory.equals(entry.getValue())));
        }

        int start = safePage * PRODUCT_SLOTS.size();
        for (int index = 0; index < PRODUCT_SLOTS.size() && start + index < products.size(); index++) {
            inventory.setItem(PRODUCT_SLOTS.get(index), productDisplay(products.get(start + index), isAdvancedMode(player)));
        }

        inventory.setItem(BazaarLayout.SEARCH, named(Material.OAK_SIGN, "§aSearch", List.of(
                query == null || query.isBlank() ? "§7Search Bazaar products." : "§7Current: §f" + query,
                "", "§eClick to search!")));
        inventory.setItem(BazaarLayout.SELL_INVENTORY, named(Material.HOPPER, "§6Sell Inventory Now", List.of(
                "§7Instantly sells all eligible Bazaar", "§7items in your inventory.", "", "§eClick to sell!")));
        inventory.setItem(BazaarLayout.SELL_SACKS, named(Material.BUNDLE, "§6Sell Sacks Now", List.of(
                "§7Sack integration is not installed.", "§8This button is reserved for it.")));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        inventory.setItem(BazaarLayout.MANAGE_ORDERS, named(Material.BOOK, "§aManage Orders", List.of(
                "§7Open orders: §e" + manager.playerOrders(player.getUniqueId()).size(),
                "§7Claimable coins: §6" + format(manager.claimableCoins(player.getUniqueId())),
                "", "§eClick to manage!")));
        boolean advancedMode = isAdvancedMode(player);
        inventory.setItem(BazaarLayout.VIEW_MODE, named(
                advancedMode ? Material.COMPARATOR : Material.REDSTONE_TORCH,
                advancedMode ? "§aAdvanced Mode" : "§eDirect Mode",
                List.of(
                        advancedMode
                                ? "§7Prices and live order volume are shown."
                                : "§7Only essential buy and sell prices are shown.",
                        "",
                        "§eClick to switch mode!"
                )
        ));

        if (safePage > 0) inventory.setItem(46, named(Material.ARROW, "§aPrevious Page", List.of()));
        if (safePage + 1 < pages) inventory.setItem(53, named(Material.ARROW, "§aNext Page", List.of()));
        player.openInventory(inventory);
    }

    public void openDetail(Player player, BazaarItem item, String category, int page, String query) {
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.DETAIL, category, item.id(), page, query,
                null, 0, 0L, 0L);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§8" + displayName(item.template()));
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);

        inventory.setItem(BazaarLayout.INSTANT_BUY, marketAction(
                Material.EMERALD_BLOCK, "§aBuy Instantly", manager.bestSellPrice(item.id()).orElse(0L),
                manager.availableVolume(item.id(), BazaarOrderSide.SELL)));
        inventory.setItem(BazaarLayout.PRODUCT_CENTER, productDisplay(item));
        inventory.setItem(BazaarLayout.INSTANT_SELL, marketAction(
                Material.GOLD_BLOCK, "§6Sell Instantly", manager.bestBuyPrice(item.id()).orElse(0L),
                manager.availableVolume(item.id(), BazaarOrderSide.BUY)));
        inventory.setItem(BazaarLayout.CREATE_BUY_ORDER, named(Material.LIME_DYE, "§aCreate Buy Order", List.of(
                "§7Choose an amount and price.", "§7Your coins are held until filled", "§7or the order is cancelled.", "", "§eClick to create!")));
        inventory.setItem(BazaarLayout.CREATE_SELL_OFFER, named(Material.ORANGE_DYE, "§6Create Sell Offer", List.of(
                "§7Owned: §e" + manager.countMatching(player, item.template()),
                "§7Choose an amount and price.", "", "§eClick to create!")));
        inventory.setItem(BazaarLayout.BACK_LEFT, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        inventory.setItem(BazaarLayout.MANAGE_ORDERS, named(Material.BOOK, "§aManage Orders", List.of("§eClick to manage!")));
        inventory.setItem(BazaarLayout.VIEW_GRAPHS, named(Material.PAINTING, "§eView Graphs", List.of(
                "§7Displays the live order-book summary.", "", "§eClick to view!")));
        inventory.setItem(BazaarLayout.INSTA_SELL_IGNORE, named(Material.REDSTONE_TORCH, "§cInstasell Ignore", List.of(
                "§7Ignore-list persistence is reserved", "§7for the settings update.")));
        player.openInventory(inventory);
    }

    public void openQuantity(Player player, BazaarItem item, BazaarHolder.Action action,
                             String category, int page, String query) {
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.QUANTITY, category, item.id(), page, query,
                action, 0, 0L, 0L);
        String title = action == BazaarHolder.Action.INSTANT_BUY ? "§8Instant Buy" : "§8Instant Sell";
        Inventory inventory = Bukkit.createInventory(holder, 54, title + " ➜ " + displayName(item.template()));
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(BazaarLayout.QUANTITY_ONE, quantityButton(action, 1, "§a" + actionVerb(action) + " only one!"));
        inventory.setItem(BazaarLayout.QUANTITY_STACK, quantityButton(action, 64, "§a" + actionVerb(action) + " a stack!"));
        int maximum = action == BazaarHolder.Action.INSTANT_BUY
                ? manager.maxInstantBuy(player, item)
                : Math.min(manager.countMatching(player, item.template()),
                manager.availableVolume(item.id(), BazaarOrderSide.BUY));
        inventory.setItem(BazaarLayout.QUANTITY_FILL, quantityButton(action, Math.max(1, maximum),
                action == BazaarHolder.Action.INSTANT_BUY ? "§aFill my inventory!" : "§6Sell all available!"));
        inventory.setItem(BazaarLayout.QUANTITY_CUSTOM, named(Material.OAK_SIGN, "§eCustom Amount", List.of(
                "§7Enter an exact amount in chat.", "", "§eClick to enter!")));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        player.openInventory(inventory);
    }

    public void openOrderAmount(Player player, BazaarItem item, BazaarHolder.Action action,
                                String category, int page, String query) {
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.ORDER_AMOUNT, category, item.id(), page, query,
                action, 0, 0L, 0L);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§8How many do you want?");
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(BazaarLayout.ORDER_AMOUNT_4, orderAmountButton(4, "§aOrder 4x"));
        inventory.setItem(BazaarLayout.ORDER_AMOUNT_10, orderAmountButton(10, "§aOrder 10x"));
        inventory.setItem(BazaarLayout.ORDER_AMOUNT_64, orderAmountButton(64, "§aOrder 64x"));
        inventory.setItem(BazaarLayout.ORDER_AMOUNT_CUSTOM, named(Material.OAK_SIGN, "§eCustom Amount", List.of(
                "§7Enter the amount in chat.", "", "§eClick to enter!")));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        player.openInventory(inventory);
    }

    public void openPriceSelection(Player player, BazaarItem item, BazaarHolder.Action action, int amount,
                                   String category, int page, String query) {
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.PRICE, category, item.id(), page, query,
                action, amount, 0L, 0L);
        boolean buy = action == BazaarHolder.Action.CREATE_BUY;
        Inventory inventory = Bukkit.createInventory(holder, 54,
                buy ? "§8How much do you want to pay?" : "§8At what price are you selling?");
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);

        long top = buy ? manager.bestBuyPrice(item.id()).orElse(manager.referenceSellPrice(item))
                : manager.bestSellPrice(item.id()).orElse(manager.referenceBuyPrice(item));
        long improved = buy ? safeAdd(top, 1L) : Math.max(1L, top - 1L);
        long buyTop = manager.bestBuyPrice(item.id()).orElse(manager.referenceSellPrice(item));
        long sellTop = manager.bestSellPrice(item.id()).orElse(manager.referenceBuyPrice(item));
        long spread = Math.max(1L, buyTop + Math.max(0L, sellTop - buyTop) / 2L);

        inventory.setItem(BazaarLayout.PRICE_TOP, priceButton(top,
                buy ? "§aSame as Top Order" : "§6Same as Best Offer"));
        inventory.setItem(BazaarLayout.PRICE_IMPROVE, priceButton(improved,
                buy ? "§aTop Order +1" : "§6Best Offer -1"));
        inventory.setItem(BazaarLayout.PRICE_SPREAD, priceButton(spread,
                buy ? "§e5% of Spread" : "§e10% of Spread"));
        inventory.setItem(BazaarLayout.PRICE_CUSTOM, named(Material.OAK_SIGN, "§eCustom Price", List.of(
                "§7Enter a price per item in chat.", "", "§eClick to enter!")));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cCancel Order", List.of()));
        player.openInventory(inventory);
    }

    public void openConfirm(Player player, BazaarItem item, BazaarHolder.Action action, int amount, long price,
                            String category, int page, String query) {
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.CONFIRM, category, item.id(), page, query,
                action, amount, price, 0L);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                action == BazaarHolder.Action.CREATE_BUY ? "§8Confirm Buy Order" :
                        action == BazaarHolder.Action.CREATE_SELL ? "§8Confirm Sell Offer" : "§8Are you sure?");
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);
        ItemStack center = item.template();
        center.setAmount(Math.min(center.getMaxStackSize(), Math.max(1, amount)));
        inventory.setItem(13, center);
        long total = safeMultiply(price, amount);
        inventory.setItem(29, named(Material.LIME_TERRACOTTA, "§aConfirm", List.of(
                "§7Amount: §e" + format(amount),
                "§7Price each: §6" + format(price),
                "§7Total: §6" + format(total) + " Coins",
                "", "§eClick to confirm!")));
        inventory.setItem(33, named(Material.RED_TERRACOTTA, "§cCancel", List.of()));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        player.openInventory(inventory);
    }

    public void openMyOrders(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.MY_ORDERS), 54, "§8Manage Orders");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);
        List<BazaarOrder> orders = manager.playerOrders(player.getUniqueId());
        for (int index = 0; index < Math.min(PRODUCT_SLOTS.size(), orders.size()); index++) {
            BazaarOrder order = orders.get(index);
            BazaarItem item = manager.find(order.itemId()).orElse(null);
            if (item == null) continue;
            ItemStack stack = item.template();
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName((order.side() == BazaarOrderSide.BUY ? "§aBuy Order: " : "§6Sell Offer: ") + displayName(stack));
            int completed = order.originalAmount() - order.remainingAmount();
            double percent = order.originalAmount() <= 0 ? 0.0D : completed * 100.0D / order.originalAmount();
            meta.setLore(List.of(
                    "§7Order ID: §f" + order.id(),
                    "§7Price: §6" + format(order.price()) + " each",
                    "§7Remaining: §e" + format(order.remainingAmount()),
                    "§7Filled: §b" + String.format(Locale.ROOT, "%.1f%%", percent),
                    "", "§cRight-click to cancel"
            ));
            stack.setItemMeta(meta);
            inventory.setItem(PRODUCT_SLOTS.get(index), stack);
        }
        if (orders.isEmpty()) inventory.setItem(22, named(Material.PAPER, "§7No active orders", List.of()));
        inventory.setItem(47, named(Material.ENDER_CHEST, "§eClaim Orders", List.of(
                "§7Coins: §6" + format(manager.claimableCoins(player.getUniqueId())),
                "§7Item types: §e" + manager.itemClaims(player.getUniqueId()).size(),
                "", "§eClick to claim!")));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        player.openInventory(inventory);
    }

    public void openClaims(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.CLAIMS), 54, "§8Bazaar Claims");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);
        long coins = manager.claimableCoins(player.getUniqueId());
        inventory.setItem(4, named(Material.GOLD_INGOT, "§6Claim Coins", List.of(
                "§7Available: §6" + format(coins) + " Coins", "", "§eClick to claim!")));
        List<BazaarItemClaim> claims = manager.itemClaims(player.getUniqueId());
        for (int index = 0; index < Math.min(PRODUCT_SLOTS.size(), claims.size()); index++) {
            BazaarItemClaim claim = claims.get(index);
            BazaarItem item = manager.find(claim.itemId()).orElse(null);
            if (item == null) continue;
            ItemStack stack = item.template();
            ItemMeta meta = stack.getItemMeta();
            List<String> lore = new ArrayList<>(meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : List.of());
            lore.add("");
            lore.add("§7Available: §e" + format(claim.amount()));
            lore.add("§eClick to claim!");
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inventory.setItem(PRODUCT_SLOTS.get(index), stack);
        }
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        player.openInventory(inventory);
    }

    public void openHistory(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.HISTORY), 54, "§8Bazaar History");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);
        List<BazaarItem> products = manager.enabledItems();
        for (int index = 0; index < Math.min(PRODUCT_SLOTS.size(), products.size()); index++) {
            BazaarItem product = products.get(index);
            inventory.setItem(PRODUCT_SLOTS.get(index), productDisplay(product));
        }
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        inventory.setItem(BazaarLayout.CLOSE, named(Material.BARRIER, "§cClose", List.of()));
        player.openInventory(inventory);
    }

    public void openSettings(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.SETTINGS), 27, "§8Bazaar Settings");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);
        inventory.setItem(11, named(Material.REDSTONE_TORCH, "§cInstasell Ignore List", List.of(
                "§7The button locations are reserved.", "§7Persistent ignore rules will be added", "§7with the sack system.")));
        inventory.setItem(15, named(Material.COMPARATOR, "§aAdvanced Mode", List.of(
                "§7This Bazaar always displays best", "§7prices and live order volume.")));
        inventory.setItem(22, named(Material.ARROW, "§aGo Back", List.of()));
        player.openInventory(inventory);
    }

    public void openGraphs(Player player, BazaarItem item, String category, int page, String query) {
        BazaarHolder holder = new BazaarHolder(BazaarHolder.Screen.GRAPHS, category, item.id(), page, query,
                null, 0, 0L, 0L);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§8Graphs ➜ " + displayName(item.template()));
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);
        inventory.setItem(11, graph(Material.EMERALD, "§aBuy Price", manager.bestBuyPrice(item.id()).orElse(0L)));
        inventory.setItem(13, graph(Material.CHEST, "§aBuy Orders Volume", manager.availableVolume(item.id(), BazaarOrderSide.BUY)));
        inventory.setItem(15, graph(Material.GOLD_INGOT, "§6Sell Price", manager.bestSellPrice(item.id()).orElse(0L)));
        inventory.setItem(31, graph(Material.HOPPER, "§6Sell Offers Volume", manager.availableVolume(item.id(), BazaarOrderSide.SELL)));
        inventory.setItem(BazaarLayout.BACK, named(Material.ARROW, "§aGo Back", List.of()));
        player.openInventory(inventory);
    }

    public void openAdmin(Player player) {
        Inventory inventory = Bukkit.createInventory(new BazaarHolder(BazaarHolder.Screen.ADMIN), 54, "§4Bazaar Admin");
        int slot = 0;
        for (BazaarItem item : manager.allItems()) {
            if (slot >= 45) break;
            inventory.setItem(slot++, adminDisplay(item));
        }
        inventory.setItem(49, named(Material.LIME_DYE, "§aRegister held item", List.of("§7Registers the item in your main hand.")));
        inventory.setItem(50, named(Material.CLOCK, "§eReload", List.of("§7Reload products from MySQL.")));
        player.openInventory(inventory);
    }

    public boolean isAdvancedMode(Player player) {
        Objects.requireNonNull(player, "player");
        return advancedModeByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public void toggleViewMode(Player player) {
        Objects.requireNonNull(player, "player");
        advancedModeByPlayer.compute(player.getUniqueId(), (uuid, current) -> current == null || !current);
    }

    public List<BazaarItem> filtered(String category, String query) {
        String selected = normalizeCategory(category);
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        return manager.enabledItems().stream()
                .filter(item -> q.isBlank() ? selected.equals(normalizeCategory(item.category())) : true)
                .filter(item -> q.isBlank()
                        || item.id().toLowerCase(Locale.ROOT).contains(q)
                        || displayName(item.template()).toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public long suggestedPrice(BazaarItem item, BazaarHolder.Action action, int priceSlot) {
        boolean buy = action == BazaarHolder.Action.CREATE_BUY;
        long top = buy ? manager.bestBuyPrice(item.id()).orElse(manager.referenceSellPrice(item))
                : manager.bestSellPrice(item.id()).orElse(manager.referenceBuyPrice(item));
        if (priceSlot == BazaarLayout.PRICE_TOP) return Math.max(1L, top);
        if (priceSlot == BazaarLayout.PRICE_IMPROVE) return buy ? safeAdd(top, 1L) : Math.max(1L, top - 1L);
        long bestBuy = manager.bestBuyPrice(item.id()).orElse(manager.referenceSellPrice(item));
        long bestSell = manager.bestSellPrice(item.id()).orElse(manager.referenceBuyPrice(item));
        return Math.max(1L, bestBuy + Math.max(0L, bestSell - bestBuy) / 2L);
    }

    private ItemStack categoryIcon(String category, boolean selected) {
        Material material = switch (category) {
            case "FARMING" -> Material.WHEAT;
            case "MINING" -> Material.DIAMOND_PICKAXE;
            case "COMBAT" -> Material.IRON_SWORD;
            case "FORAGING" -> Material.OAK_LOG;
            default -> Material.NETHER_STAR;
        };
        return named(material, (selected ? "§e" : "§a") + categoryName(category), List.of(
                "§7Products: §e" + manager.enabledItems().stream()
                        .filter(item -> normalizeCategory(item.category()).equals(category)).count(),
                selected ? "§aCurrently selected" : "§eClick to view!"));
    }

    private ItemStack productDisplay(BazaarItem item) {
        return productDisplay(item, true);
    }

    private ItemStack productDisplay(BazaarItem item, boolean advancedMode) {
        ItemStack stack = item.template();
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = new ArrayList<>(meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : List.of());
        if (!lore.isEmpty()) lore.add("");
        lore.add("§7Buy price: §6" + format(manager.bestSellPrice(item.id()).orElse(item.buyPrice())) + " coins");
        lore.add("§7Sell price: §6" + format(manager.bestBuyPrice(item.id()).orElse(item.sellPrice())) + " coins");
        if (advancedMode) {
            lore.add("§7Buy volume: §e" + format(manager.availableVolume(item.id(), BazaarOrderSide.BUY)));
            lore.add("§7Sell volume: §e" + format(manager.availableVolume(item.id(), BazaarOrderSide.SELL)));
        }
        lore.add("");
        lore.add("§eClick to view product!");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack adminDisplay(BazaarItem item) {
        ItemStack stack = productDisplay(item);
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = new ArrayList<>(Objects.requireNonNullElse(meta.getLore(), List.of()));
        lore.add("§7ID: §f" + item.id());
        lore.add("§7Category: §f" + item.category());
        lore.add("§aLeft: reference buy +1");
        lore.add("§cRight: reference buy -1");
        lore.add("§bShift-left: reference sell +1");
        lore.add("§4Shift-right: delete");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack marketAction(Material material, String name, long price, int volume) {
        return named(material, name, List.of(
                price > 0L ? "§7Best price: §6" + format(price) + " coins" : "§cNo matching orders",
                "§7Available: §e" + format(volume), "", "§eClick to continue!"));
    }

    private ItemStack quantityButton(BazaarHolder.Action action, int amount, String name) {
        return named(action == BazaarHolder.Action.INSTANT_BUY ? Material.EMERALD : Material.GOLD_INGOT,
                name, List.of("§7Amount: §e" + format(amount), "", "§eClick to select!"));
    }

    private ItemStack orderAmountButton(int amount, String name) {
        return named(Material.CHEST, name, List.of("§7Amount: §e" + format(amount), "", "§eClick to select!"));
    }

    private ItemStack priceButton(long price, String name) {
        return named(Material.GOLD_NUGGET, name, List.of("§7Price per item: §6" + format(price), "", "§eClick to select!"));
    }

    private ItemStack graph(Material material, String name, long value) {
        return named(material, name, List.of("§7Current value: §e" + format(value), "§8Historical storage is not enabled yet."));
    }

    private Material paneFor(String category) {
        return switch (category) {
            case "FARMING" -> Material.YELLOW_STAINED_GLASS_PANE;
            case "MINING" -> Material.GRAY_STAINED_GLASS_PANE;
            case "COMBAT" -> Material.RED_STAINED_GLASS_PANE;
            case "FORAGING" -> Material.LIME_STAINED_GLASS_PANE;
            default -> Material.PINK_STAINED_GLASS_PANE;
        };
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack pane = named(material, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private String normalizeCategory(String category) {
        if (category == null) return "FARMING";
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "FISHING" -> "FORAGING";
            case "NETHER" -> "COMBAT";
            case "ODDITIES" -> "OTHER";
            default -> category.toUpperCase(Locale.ROOT);
        };
    }

    private String categoryName(String category) {
        return switch (normalizeCategory(category)) {
            case "FARMING" -> "Farming";
            case "MINING" -> "Mining";
            case "COMBAT" -> "Combat";
            case "FORAGING" -> "Woods & Fishes";
            default -> "Oddities";
        };
    }

    private String displayName(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        return meta.hasDisplayName() ? meta.getDisplayName()
                : titleCase(stack.getType().getKey().getKey().replace('_', ' '));
    }

    private String titleCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean upper = true;
        for (char character : input.toCharArray()) {
            if (upper && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                upper = false;
            } else {
                result.append(character);
                if (character == ' ') upper = true;
            }
        }
        return result.toString();
    }

    private String actionVerb(BazaarHolder.Action action) {
        return action == BazaarHolder.Action.INSTANT_BUY ? "Buy" : "Sell";
    }

    private long safeMultiply(long price, int amount) {
        try {
            return Math.multiplyExact(price, (long) amount);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private long safeAdd(long value, long increment) {
        try {
            return Math.addExact(value, increment);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
