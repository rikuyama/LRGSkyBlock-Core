package me.lrg.skyblock.core.bazaar.manager;

import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import me.lrg.skyblock.core.bazaar.order.model.BazaarItemClaim;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrder;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderSide;
import me.lrg.skyblock.core.bazaar.order.repository.BazaarOrderRepository;
import me.lrg.skyblock.core.bazaar.repository.BazaarRepository;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.service.EconomyOperationIds;
import me.lrg.skyblock.core.economy.service.EconomyService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class BazaarManager {
    public record TradeResult(int amount, long totalCoins) {
        public boolean success() { return amount > 0; }
    }

    private final BazaarRepository repository;
    private final BazaarOrderRepository orderRepository;
    private final EconomyService economyService;
    private final Map<String, BazaarItem> items = new LinkedHashMap<>();

    public BazaarManager(
            BazaarRepository repository,
            BazaarOrderRepository orderRepository,
            EconomyService economyService
    ) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.economyService = economyService;
        reload();
    }

    public void reload() {
        items.clear();
        repository.loadAll().forEach(item -> items.put(item.id(), item));
    }

    public Collection<BazaarItem> allItems() { return List.copyOf(items.values()); }
    public List<BazaarItem> enabledItems() { return items.values().stream().filter(BazaarItem::enabled).toList(); }
    public Optional<BazaarItem> find(String id) { return Optional.ofNullable(items.get(id)); }

    public BazaarItem addTemplate(ItemStack held) {
        ItemStack template = held.clone();
        template.setAmount(1);
        String base = template.getType().getKey().getKey().toUpperCase(Locale.ROOT);
        String id = base;
        int suffix = 2;
        while (items.containsKey(id)) id = base + "_" + suffix++;
        BazaarItem item = new BazaarItem(id, template, 10L, 5L, guessCategory(template.getType()), true);
        items.put(id, item);
        repository.save(item);
        return item;
    }

    public void save(BazaarItem item) { repository.save(item); }
    public void delete(BazaarItem item) { items.remove(item.id()); repository.delete(item.id()); }

    public OptionalLong bestBuyPrice(String itemId) {
        return orderRepository.findBest(itemId, BazaarOrderSide.BUY)
                .map(order -> OptionalLong.of(order.price()))
                .orElseGet(OptionalLong::empty);
    }

    public OptionalLong bestSellPrice(String itemId) {
        return orderRepository.findBest(itemId, BazaarOrderSide.SELL)
                .map(order -> OptionalLong.of(order.price()))
                .orElseGet(OptionalLong::empty);
    }

    public long referenceBuyPrice(BazaarItem item) {
        return bestSellPrice(item.id()).orElse(Math.max(1L, item.buyPrice()));
    }

    public long referenceSellPrice(BazaarItem item) {
        return bestBuyPrice(item.id()).orElse(Math.max(1L, item.sellPrice()));
    }

    public int availableVolume(String itemId, BazaarOrderSide side) {
        long total = orderRepository.findOpenByItem(itemId, side).stream()
                .mapToLong(BazaarOrder::remainingAmount)
                .sum();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public synchronized boolean createBuyOrder(Player player, BazaarItem item, long price, int amount) {
        if (!validOrder(price, amount)) return false;
        long escrow;
        try {
            escrow = Math.multiplyExact(price, (long) amount);
        } catch (ArithmeticException exception) {
            return false;
        }
        String operation = EconomyOperationIds.create("bazaar-buy-order", player.getUniqueId());
        EconomyResult withdrawn = economyService.withdrawWallet(
                player.getUniqueId(), escrow, "bazaar_buy_order:" + item.id(), operation);
        if (!withdrawn.success()) return false;
        boolean persisted = false;
        try {
            orderRepository.create(player.getUniqueId(), item.id(), BazaarOrderSide.BUY, price, amount);
            persisted = true;
            match(item.id());
            return true;
        } catch (RuntimeException exception) {
            // Once persisted, the escrow belongs to the open order. Refunding here would duplicate coins.
            if (!persisted) {
                economyService.depositWallet(player.getUniqueId(), escrow, "bazaar_buy_order_refund:" + item.id(),
                        EconomyOperationIds.create("bazaar-buy-order-refund", player.getUniqueId()));
            }
            throw exception;
        }
    }

    public synchronized boolean createSellOrder(Player player, BazaarItem item, long price, int amount) {
        if (!validOrder(price, amount) || countMatching(player, item.template()) < amount) return false;
        removeMatching(player, item.template(), amount);
        boolean persisted = false;
        try {
            orderRepository.create(player.getUniqueId(), item.id(), BazaarOrderSide.SELL, price, amount);
            persisted = true;
            match(item.id());
            return true;
        } catch (RuntimeException exception) {
            // Once persisted, the items are escrow for the open order. Returning them would duplicate items.
            if (!persisted) {
                giveOrDrop(player, item.template(), amount);
            }
            throw exception;
        }
    }

    public synchronized TradeResult instantBuy(Player player, BazaarItem item, int requested) {
        if (requested <= 0) return new TradeResult(0, 0L);
        int bought = 0;
        long spent = 0L;
        for (BazaarOrder sell : orderRepository.findOpenByItem(item.id(), BazaarOrderSide.SELL)) {
            if (bought >= requested) break;
            int quantity = Math.min(requested - bought, sell.remainingAmount());
            if (!canFit(player, item.template(), quantity)) break;
            long cost;
            try {
                cost = Math.multiplyExact(sell.price(), (long) quantity);
            } catch (ArithmeticException exception) {
                break;
            }
            EconomyResult payment = economyService.withdrawWallet(
                    player.getUniqueId(), cost, "bazaar_instant_buy:" + item.id(),
                    EconomyOperationIds.create("bazaar-instant-buy", player.getUniqueId()));
            if (!payment.success()) break;
            try {
                orderRepository.addCoinClaim(sell.owner(), cost);
                orderRepository.updateRemaining(sell.id(), sell.remainingAmount() - quantity);
                giveOrDrop(player, item.template(), quantity);
                bought += quantity;
                spent = Math.addExact(spent, cost);
            } catch (RuntimeException exception) {
                economyService.depositWallet(player.getUniqueId(), cost, "bazaar_instant_buy_refund:" + item.id(),
                        EconomyOperationIds.create("bazaar-instant-buy-refund", player.getUniqueId()));
                throw exception;
            }
        }
        return new TradeResult(bought, spent);
    }

    public synchronized TradeResult instantSell(Player player, BazaarItem item, int requested) {
        if (requested <= 0) return new TradeResult(0, 0L);
        int target = Math.min(requested, countMatching(player, item.template()));
        int sold = 0;
        long earned = 0L;
        for (BazaarOrder buy : orderRepository.findOpenByItem(item.id(), BazaarOrderSide.BUY)) {
            if (sold >= target) break;
            int quantity = Math.min(target - sold, buy.remainingAmount());
            long value;
            try {
                value = Math.multiplyExact(buy.price(), (long) quantity);
            } catch (ArithmeticException exception) {
                break;
            }
            removeMatching(player, item.template(), quantity);
            try {
                orderRepository.addItemClaim(buy.owner(), item.id(), quantity);
                orderRepository.updateRemaining(buy.id(), buy.remainingAmount() - quantity);
                sold += quantity;
                earned = Math.addExact(earned, value);
            } catch (RuntimeException exception) {
                giveOrDrop(player, item.template(), quantity);
                throw exception;
            }
        }
        if (earned > 0L) {
            EconomyResult credit = economyService.depositWallet(
                    player.getUniqueId(), earned, "bazaar_instant_sell:" + item.id(),
                    EconomyOperationIds.create("bazaar-instant-sell", player.getUniqueId()));
            if (!credit.success()) orderRepository.addCoinClaim(player.getUniqueId(), earned);
        }
        return new TradeResult(sold, earned);
    }

    public synchronized TradeResult sellInventoryNow(Player player) {
        int totalItems = 0;
        long totalCoins = 0L;
        for (BazaarItem item : enabledItems()) {
            int owned = countMatching(player, item.template());
            if (owned <= 0) continue;
            TradeResult result = instantSell(player, item, owned);
            totalItems += result.amount();
            totalCoins += result.totalCoins();
        }
        return new TradeResult(totalItems, totalCoins);
    }

    private void match(String itemId) {
        while (true) {
            BazaarOrder buy = orderRepository.findBest(itemId, BazaarOrderSide.BUY).orElse(null);
            BazaarOrder sell = orderRepository.findBest(itemId, BazaarOrderSide.SELL).orElse(null);
            if (buy == null || sell == null || buy.price() < sell.price()) return;
            int quantity = Math.min(buy.remainingAmount(), sell.remainingAmount());
            long tradePrice = sell.id() < buy.id() ? sell.price() : buy.price();
            long sellerCoins = Math.multiplyExact(tradePrice, (long) quantity);
            long reserved = Math.multiplyExact(buy.price(), (long) quantity);
            orderRepository.addCoinClaim(sell.owner(), sellerCoins);
            if (reserved > sellerCoins) orderRepository.addCoinClaim(buy.owner(), reserved - sellerCoins);
            orderRepository.addItemClaim(buy.owner(), itemId, quantity);
            orderRepository.updateRemaining(buy.id(), buy.remainingAmount() - quantity);
            orderRepository.updateRemaining(sell.id(), sell.remainingAmount() - quantity);
        }
    }

    public synchronized boolean cancelOrder(Player player, long orderId) {
        BazaarOrder order = orderRepository.find(orderId).orElse(null);
        if (order == null || !order.owner().equals(player.getUniqueId()) || order.remainingAmount() <= 0) return false;
        if (!orderRepository.cancel(orderId, player.getUniqueId())) return false;
        if (order.side() == BazaarOrderSide.BUY) {
            orderRepository.addCoinClaim(order.owner(), Math.multiplyExact(order.price(), (long) order.remainingAmount()));
        } else {
            orderRepository.addItemClaim(order.owner(), order.itemId(), order.remainingAmount());
        }
        return true;
    }

    public List<BazaarOrder> playerOrders(UUID owner) { return orderRepository.findOpenByOwner(owner); }
    public long claimableCoins(UUID owner) { return orderRepository.coinClaim(owner); }
    public List<BazaarItemClaim> itemClaims(UUID owner) { return orderRepository.itemClaims(owner); }

    public synchronized long claimCoins(Player player) {
        long amount = orderRepository.coinClaim(player.getUniqueId());
        if (amount <= 0L) return 0L;
        EconomyResult credit = economyService.depositWallet(
                player.getUniqueId(), amount, "bazaar_claim", EconomyOperationIds.create("bazaar-claim", player.getUniqueId()));
        if (!credit.success()) return 0L;
        if (!orderRepository.clearCoinClaim(player.getUniqueId(), amount)) {
            economyService.withdrawWallet(player.getUniqueId(), amount, "bazaar_claim_rollback",
                    EconomyOperationIds.create("bazaar-claim-rollback", player.getUniqueId()));
            return 0L;
        }
        return amount;
    }

    public synchronized int claimItems(Player player, String itemId) {
        BazaarItem item = find(itemId).orElse(null);
        if (item == null) return 0;
        long available = itemClaims(player.getUniqueId()).stream()
                .filter(claim -> claim.itemId().equals(itemId))
                .mapToLong(BazaarItemClaim::amount)
                .findFirst()
                .orElse(0L);
        if (available <= 0L) return 0;
        int deliver = (int) Math.min(Integer.MAX_VALUE, available);
        int fit = maxFit(player, item.template(), deliver);
        if (fit <= 0 || !orderRepository.decreaseItemClaim(player.getUniqueId(), itemId, fit)) return 0;
        giveOrDrop(player, item.template(), fit);
        return fit;
    }

    public int maxInstantBuy(Player player, BazaarItem item) {
        long coins = economyService.getWalletBalance(player.getUniqueId());
        int amount = 0;
        for (BazaarOrder order : orderRepository.findOpenByItem(item.id(), BazaarOrderSide.SELL)) {
            if (order.price() <= 0L) continue;
            int affordable = (int) Math.min(order.remainingAmount(), coins / order.price());
            amount += affordable;
            coins -= affordable * order.price();
            if (affordable < order.remainingAmount()) break;
        }
        return amount;
    }

    public int countMatching(Player player, ItemStack template) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.isSimilar(template)) total += stack.getAmount();
        }
        return total;
    }

    private boolean validOrder(long price, int amount) { return price > 0L && amount > 0; }

    private void removeMatching(Player player, ItemStack template, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int index = 0; index < contents.length && left > 0; index++) {
            ItemStack stack = contents[index];
            if (stack == null || !stack.isSimilar(template)) continue;
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) contents[index] = null;
            left -= take;
        }
        player.getInventory().setStorageContents(contents);
    }

    private boolean canFit(Player player, ItemStack template, int amount) {
        return maxFit(player, template, amount) >= amount;
    }

    private int maxFit(Player player, ItemStack template, int limit) {
        int capacity = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) capacity += template.getMaxStackSize();
            else if (stack.isSimilar(template)) capacity += Math.max(0, stack.getMaxStackSize() - stack.getAmount());
            if (capacity >= limit) return limit;
        }
        return capacity;
    }

    private void giveOrDrop(Player player, ItemStack template, int amount) {
        int left = amount;
        while (left > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(stack.getMaxStackSize(), left));
            int delivered = stack.getAmount();
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
            left -= delivered;
        }
    }

    private String guessCategory(Material material) {
        String name = material.name();
        if (name.contains("ORE") || name.contains("INGOT") || name.contains("COAL") || name.contains("STONE")) return "MINING";
        if (name.contains("LOG") || name.contains("WOOD") || name.contains("STEM") || name.contains("HYPHAE")
                || name.contains("FISH") || name.contains("COD") || name.contains("SALMON")) return "FORAGING";
        if (name.contains("WHEAT") || name.contains("SEEDS") || name.contains("CARROT") || name.contains("POTATO")
                || name.contains("MELON") || name.contains("PUMPKIN")) return "FARMING";
        if (name.contains("ROTTEN") || name.contains("BONE") || name.contains("BLAZE") || name.contains("MAGMA")
                || name.contains("ENDER") || name.contains("SPIDER")) return "COMBAT";
        return "OTHER";
    }
}
