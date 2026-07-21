package me.lrg.skyblock.core.bazaar.listener;

import me.lrg.skyblock.core.bazaar.config.BazaarMessages;
import me.lrg.skyblock.core.bazaar.gui.BazaarGui;
import me.lrg.skyblock.core.bazaar.gui.BazaarHolder;
import me.lrg.skyblock.core.bazaar.gui.BazaarLayout;
import me.lrg.skyblock.core.bazaar.manager.BazaarManager;
import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import me.lrg.skyblock.core.bazaar.order.model.BazaarItemClaim;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrder;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderSide;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BazaarListener implements Listener {
    private enum InputType { SEARCH, AMOUNT, PRICE }

    private record PendingInput(
            InputType type,
            String itemId,
            BazaarHolder.Action action,
            int amount,
            String category,
            int page,
            String query
    ) {
    }

    private final JavaPlugin plugin;
    private final BazaarManager manager;
    private final BazaarGui gui;
    private final BazaarMessages messages;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

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
            case PRODUCTS -> clickProducts(player, holder, slot);
            case DETAIL -> clickDetail(player, holder, slot);
            case QUANTITY -> clickQuantity(player, holder, slot);
            case ORDER_AMOUNT -> clickOrderAmount(player, holder, slot);
            case PRICE -> clickPrice(player, holder, slot);
            case CONFIRM -> clickConfirm(player, holder, slot);
            case MY_ORDERS -> clickOrders(player, event, slot);
            case CLAIMS -> clickClaims(player, slot);
            case HISTORY, SETTINGS -> clickSimpleBack(player, slot);
            case GRAPHS -> clickGraphs(player, holder, slot);
            case ADMIN -> clickAdmin(player, event, slot);
        }
    }

    private void clickProducts(Player player, BazaarHolder holder, int slot) {
        String category = BazaarLayout.CATEGORY_BY_SLOT.get(slot);
        if (category != null) {
            gui.openProducts(player, category, 0, "");
            return;
        }
        if (slot == BazaarLayout.SEARCH) {
            beginInput(player, new PendingInput(InputType.SEARCH, null, null, 0,
                    holder.category(), holder.page(), holder.query()),
                    "§e検索する商品名をチャットへ入力してください。§7（キャンセルで中止）");
            return;
        }
        if (slot == BazaarLayout.SELL_INVENTORY) {
            BazaarManager.TradeResult result = manager.sellInventoryNow(player);
            player.sendMessage(result.success()
                    ? "§aインベントリから §e" + result.amount() + "個 §aを §6" + result.totalCoins() + " Coins §aで売却しました。"
                    : "§c今すぐ売却できるアイテムまたは買い注文がありません。");
            gui.openProducts(player, holder.category(), holder.page(), holder.query());
            return;
        }
        if (slot == BazaarLayout.SELL_SACKS) {
            player.sendMessage("§cSackシステムが未導入のため使用できません。");
            return;
        }
        if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == BazaarLayout.MANAGE_ORDERS) {
            gui.openMyOrders(player);
            return;
        }
        if (slot == BazaarLayout.VIEW_MODE) {
            gui.toggleViewMode(player);
            gui.openProducts(player, holder.category(), holder.page(), holder.query());
            return;
        }
        if (slot == 46 && holder.page() > 0) {
            gui.openProducts(player, holder.category(), holder.page() - 1, holder.query());
            return;
        }
        if (slot == 53) {
            int nextStart = (holder.page() + 1) * BazaarGui.PRODUCT_SLOTS.size();
            if (nextStart < gui.filtered(holder.category(), holder.query()).size()) {
                gui.openProducts(player, holder.category(), holder.page() + 1, holder.query());
            }
            return;
        }

        int index = BazaarGui.PRODUCT_SLOTS.indexOf(slot);
        if (index < 0) return;
        List<BazaarItem> products = gui.filtered(holder.category(), holder.query());
        int absolute = holder.page() * BazaarGui.PRODUCT_SLOTS.size() + index;
        if (absolute < products.size()) {
            gui.openDetail(player, products.get(absolute), holder.category(), holder.page(), holder.query());
        }
    }

    private void clickDetail(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) {
            gui.openPlayer(player);
            return;
        }
        if (slot == BazaarLayout.INSTANT_BUY) {
            if (manager.availableVolume(item.id(), BazaarOrderSide.SELL) <= 0) {
                player.sendMessage("§c現在、売り注文がありません。");
                return;
            }
            gui.openQuantity(player, item, BazaarHolder.Action.INSTANT_BUY,
                    holder.category(), holder.page(), holder.query());
        } else if (slot == BazaarLayout.INSTANT_SELL) {
            if (manager.availableVolume(item.id(), BazaarOrderSide.BUY) <= 0) {
                player.sendMessage("§c現在、買い注文がありません。");
                return;
            }
            gui.openQuantity(player, item, BazaarHolder.Action.INSTANT_SELL,
                    holder.category(), holder.page(), holder.query());
        } else if (slot == BazaarLayout.CREATE_BUY_ORDER) {
            gui.openOrderAmount(player, item, BazaarHolder.Action.CREATE_BUY,
                    holder.category(), holder.page(), holder.query());
        } else if (slot == BazaarLayout.CREATE_SELL_OFFER) {
            gui.openOrderAmount(player, item, BazaarHolder.Action.CREATE_SELL,
                    holder.category(), holder.page(), holder.query());
        } else if (slot == BazaarLayout.BACK_LEFT || slot == BazaarLayout.BACK) {
            gui.openProducts(player, holder.category(), holder.page(), holder.query());
        } else if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
        } else if (slot == BazaarLayout.MANAGE_ORDERS) {
            gui.openMyOrders(player);
        } else if (slot == BazaarLayout.VIEW_GRAPHS) {
            gui.openGraphs(player, item, holder.category(), holder.page(), holder.query());
        }
    }

    private void clickQuantity(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) {
            gui.openPlayer(player);
            return;
        }
        if (slot == BazaarLayout.BACK) {
            gui.openDetail(player, item, holder.category(), holder.page(), holder.query());
            return;
        }
        if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == BazaarLayout.QUANTITY_CUSTOM) {
            beginInput(player, new PendingInput(InputType.AMOUNT, item.id(), holder.action(), 0,
                    holder.category(), holder.page(), holder.query()),
                    "§e数量をチャットへ入力してください。§7（キャンセルで中止）");
            return;
        }
        int amount = switch (slot) {
            case BazaarLayout.QUANTITY_ONE -> 1;
            case BazaarLayout.QUANTITY_STACK -> 64;
            case BazaarLayout.QUANTITY_FILL -> holder.action() == BazaarHolder.Action.INSTANT_BUY
                    ? manager.maxInstantBuy(player, item)
                    : Math.min(manager.countMatching(player, item.template()),
                    manager.availableVolume(item.id(), BazaarOrderSide.BUY));
            default -> 0;
        };
        if (amount <= 0) {
            player.sendMessage("§c取引可能な数量がありません。");
            return;
        }
        long displayPrice = holder.action() == BazaarHolder.Action.INSTANT_BUY
                ? manager.referenceBuyPrice(item) : manager.referenceSellPrice(item);
        gui.openConfirm(player, item, holder.action(), amount, displayPrice,
                holder.category(), holder.page(), holder.query());
    }

    private void clickOrderAmount(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) {
            gui.openPlayer(player);
            return;
        }
        if (slot == BazaarLayout.BACK) {
            gui.openDetail(player, item, holder.category(), holder.page(), holder.query());
            return;
        }
        if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == BazaarLayout.ORDER_AMOUNT_CUSTOM) {
            beginInput(player, new PendingInput(InputType.AMOUNT, item.id(), holder.action(), 0,
                    holder.category(), holder.page(), holder.query()),
                    "§e注文数量をチャットへ入力してください。§7（キャンセルで中止）");
            return;
        }
        int amount = switch (slot) {
            case BazaarLayout.ORDER_AMOUNT_4 -> 4;
            case BazaarLayout.ORDER_AMOUNT_10 -> 10;
            case BazaarLayout.ORDER_AMOUNT_64 -> 64;
            default -> 0;
        };
        if (amount > 0) {
            gui.openPriceSelection(player, item, holder.action(), amount,
                    holder.category(), holder.page(), holder.query());
        }
    }

    private void clickPrice(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) {
            gui.openPlayer(player);
            return;
        }
        if (slot == BazaarLayout.BACK) {
            gui.openOrderAmount(player, item, holder.action(), holder.category(), holder.page(), holder.query());
            return;
        }
        if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == BazaarLayout.PRICE_CUSTOM) {
            beginInput(player, new PendingInput(InputType.PRICE, item.id(), holder.action(), holder.amount(),
                    holder.category(), holder.page(), holder.query()),
                    "§e1個あたりの価格をチャットへ入力してください。§7（キャンセルで中止）");
            return;
        }
        if (slot == BazaarLayout.PRICE_TOP || slot == BazaarLayout.PRICE_IMPROVE || slot == BazaarLayout.PRICE_SPREAD) {
            long price = gui.suggestedPrice(item, holder.action(), slot);
            gui.openConfirm(player, item, holder.action(), holder.amount(), price,
                    holder.category(), holder.page(), holder.query());
        }
    }

    private void clickConfirm(Player player, BazaarHolder holder, int slot) {
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) {
            gui.openPlayer(player);
            return;
        }
        if (slot == 33) {
            gui.openDetail(player, item, holder.category(), holder.page(), holder.query());
            return;
        }
        if (slot == BazaarLayout.BACK) {
            if (holder.action() == BazaarHolder.Action.CREATE_BUY || holder.action() == BazaarHolder.Action.CREATE_SELL) {
                gui.openPriceSelection(player, item, holder.action(), holder.amount(),
                        holder.category(), holder.page(), holder.query());
            } else {
                gui.openQuantity(player, item, holder.action(), holder.category(), holder.page(), holder.query());
            }
            return;
        }
        if (slot != 29) return;

        boolean success;
        if (holder.action() == BazaarHolder.Action.INSTANT_BUY) {
            BazaarManager.TradeResult result = manager.instantBuy(player, item, holder.amount());
            success = result.success();
            player.sendMessage(success
                    ? "§a" + item.id() + "を" + result.amount() + "個、合計" + result.totalCoins() + " Coinsで購入しました。"
                    : "§c購入できませんでした。");
        } else if (holder.action() == BazaarHolder.Action.INSTANT_SELL) {
            BazaarManager.TradeResult result = manager.instantSell(player, item, holder.amount());
            success = result.success();
            player.sendMessage(success
                    ? "§a" + item.id() + "を" + result.amount() + "個、合計" + result.totalCoins() + " Coinsで売却しました。"
                    : "§c売却できませんでした。");
        } else if (holder.action() == BazaarHolder.Action.CREATE_BUY) {
            success = manager.createBuyOrder(player, item, holder.price(), holder.amount());
            player.sendMessage(success ? "§a買い注文を作成しました。" : "§c買い注文を作成できませんでした。");
        } else {
            success = manager.createSellOrder(player, item, holder.price(), holder.amount());
            player.sendMessage(success ? "§a売り注文を作成しました。" : "§c売り注文を作成できませんでした。");
        }
        gui.openDetail(player, item, holder.category(), holder.page(), holder.query());
    }

    private void clickOrders(Player player, InventoryClickEvent event, int slot) {
        if (slot == BazaarLayout.BACK) {
            gui.openPlayer(player);
            return;
        }
        if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == 47) {
            gui.openClaims(player);
            return;
        }
        int index = BazaarGui.PRODUCT_SLOTS.indexOf(slot);
        if (index < 0) return;
        List<BazaarOrder> orders = manager.playerOrders(player.getUniqueId());
        if (index >= orders.size()) return;
        BazaarOrder order = orders.get(index);
        if (!event.isRightClick()) {
            player.sendMessage("§e右クリックで注文をキャンセルできます。注文ID: " + order.id());
            return;
        }
        boolean cancelled = manager.cancelOrder(player, order.id());
        player.sendMessage(cancelled ? "§a注文をキャンセルしました。返却物は受取所へ移動しました。"
                : "§c注文をキャンセルできませんでした。");
        gui.openMyOrders(player);
    }

    private void clickClaims(Player player, int slot) {
        if (slot == BazaarLayout.BACK) {
            gui.openMyOrders(player);
            return;
        }
        if (slot == BazaarLayout.CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == 4) {
            long amount = manager.claimCoins(player);
            player.sendMessage(amount > 0L ? "§a" + amount + " Coinsを受け取りました。" : "§7受け取れるコインはありません。");
            gui.openClaims(player);
            return;
        }
        int index = BazaarGui.PRODUCT_SLOTS.indexOf(slot);
        if (index < 0) return;
        List<BazaarItemClaim> claims = manager.itemClaims(player.getUniqueId());
        if (index >= claims.size()) return;
        BazaarItemClaim claim = claims.get(index);
        int amount = manager.claimItems(player, claim.itemId());
        player.sendMessage(amount > 0 ? "§a" + claim.itemId() + "を" + amount + "個受け取りました。"
                : "§cインベントリに空きがありません。");
        gui.openClaims(player);
    }

    private void clickSimpleBack(Player player, int slot) {
        if (slot == BazaarLayout.BACK || slot == 22) gui.openPlayer(player);
        else if (slot == BazaarLayout.CLOSE) player.closeInventory();
    }

    private void clickGraphs(Player player, BazaarHolder holder, int slot) {
        if (slot != BazaarLayout.BACK) return;
        BazaarItem item = manager.find(holder.itemId()).orElse(null);
        if (item == null) gui.openPlayer(player);
        else gui.openDetail(player, item, holder.category(), holder.page(), holder.query());
    }

    private void clickAdmin(Player player, InventoryClickEvent event, int slot) {
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
        List<BazaarItem> items = new ArrayList<>(manager.allItems());
        if (slot < 0 || slot >= Math.min(45, items.size())) return;
        BazaarItem item = items.get(slot);
        if (event.isShiftClick() && event.isRightClick()) {
            manager.delete(item);
            player.sendMessage(messages.text("bazaar.messages.deleted", Map.of("id", item.id())));
        } else if (event.isShiftClick() && event.isLeftClick()) {
            item.setSellPrice(item.sellPrice() + 1L);
            manager.save(item);
        } else if (event.isLeftClick()) {
            item.setBuyPrice(item.buyPrice() + 1L);
            manager.save(item);
        } else if (event.isRightClick()) {
            item.setBuyPrice(Math.max(1L, item.buyPrice() - 1L));
            manager.save(item);
        }
        gui.openAdmin(player);
    }

    private void beginInput(Player player, PendingInput input, String prompt) {
        pendingInputs.put(player.getUniqueId(), input);
        player.closeInventory();
        player.sendMessage(prompt);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = pendingInputs.remove(player.getUniqueId());
        if (pending == null) return;
        event.setCancelled(true);
        String value = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> handleInput(player, pending, value));
    }

    private void handleInput(Player player, PendingInput pending, String value) {
        if (isCancel(value)) {
            if (pending.itemId() == null) gui.openProducts(player, pending.category(), pending.page(), pending.query());
            else manager.find(pending.itemId()).ifPresentOrElse(
                    item -> gui.openDetail(player, item, pending.category(), pending.page(), pending.query()),
                    () -> gui.openPlayer(player));
            return;
        }
        if (pending.type() == InputType.SEARCH) {
            gui.openProducts(player, pending.category(), 0, value);
            return;
        }
        BazaarItem item = manager.find(pending.itemId()).orElse(null);
        if (item == null) {
            gui.openPlayer(player);
            return;
        }
        try {
            if (pending.type() == InputType.AMOUNT) {
                int amount = Integer.parseInt(value.replace(",", ""));
                if (amount <= 0) throw new NumberFormatException();
                if (pending.action() == BazaarHolder.Action.INSTANT_BUY || pending.action() == BazaarHolder.Action.INSTANT_SELL) {
                    long price = pending.action() == BazaarHolder.Action.INSTANT_BUY
                            ? manager.referenceBuyPrice(item) : manager.referenceSellPrice(item);
                    gui.openConfirm(player, item, pending.action(), amount, price,
                            pending.category(), pending.page(), pending.query());
                } else {
                    gui.openPriceSelection(player, item, pending.action(), amount,
                            pending.category(), pending.page(), pending.query());
                }
            } else {
                long price = Long.parseLong(value.replace(",", ""));
                if (price <= 0L) throw new NumberFormatException();
                gui.openConfirm(player, item, pending.action(), pending.amount(), price,
                        pending.category(), pending.page(), pending.query());
            }
        } catch (NumberFormatException exception) {
            player.sendMessage("§c正の整数を入力してください。");
            gui.openDetail(player, item, pending.category(), pending.page(), pending.query());
        }
    }

    private boolean isCancel(String input) {
        return input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("キャンセル");
    }
}
