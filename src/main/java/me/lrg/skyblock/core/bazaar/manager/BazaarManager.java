package me.lrg.skyblock.core.bazaar.manager;

import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import me.lrg.skyblock.core.bazaar.order.model.BazaarItemClaim;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrder;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderSide;
import me.lrg.skyblock.core.bazaar.order.repository.BazaarOrderRepository;
import me.lrg.skyblock.core.bazaar.repository.BazaarRepository;
import me.lrg.skyblock.core.manager.CoinManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class BazaarManager {
    public record TradeResult(int amount, long totalCoins) {
        public boolean success() { return amount > 0; }
    }

    private final BazaarRepository repository;
    private final BazaarOrderRepository orderRepository;
    private final CoinManager coinManager;
    private final Map<String, BazaarItem> items = new LinkedHashMap<>();

    public BazaarManager(BazaarRepository repository, BazaarOrderRepository orderRepository, CoinManager coinManager) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.coinManager = coinManager;
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
        return orderRepository.findBest(itemId, BazaarOrderSide.BUY).map(order -> OptionalLong.of(order.price())).orElseGet(OptionalLong::empty);
    }

    public OptionalLong bestSellPrice(String itemId) {
        return orderRepository.findBest(itemId, BazaarOrderSide.SELL).map(order -> OptionalLong.of(order.price())).orElseGet(OptionalLong::empty);
    }

    public int availableVolume(String itemId, BazaarOrderSide side) {
        long total = orderRepository.findOpenByItem(itemId, side).stream().mapToLong(BazaarOrder::remainingAmount).sum();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public synchronized boolean createBuyOrder(Player player, BazaarItem item, long price, int amount) {
        if (!validOrder(price, amount)) return false;
        long escrow;
        try { escrow = Math.multiplyExact(price, (long) amount); } catch (ArithmeticException exception) { return false; }
        if (!coinManager.removeCoins(player.getUniqueId(), escrow)) return false;
        try {
            orderRepository.create(player.getUniqueId(), item.id(), BazaarOrderSide.BUY, price, amount);
            match(item.id());
            return true;
        } catch (RuntimeException exception) {
            coinManager.addCoins(player.getUniqueId(), escrow);
            throw exception;
        }
    }

    public synchronized boolean createSellOrder(Player player, BazaarItem item, long price, int amount) {
        if (!validOrder(price, amount) || countMatching(player, item.template()) < amount) return false;
        removeMatching(player, item.template(), amount);
        try {
            orderRepository.create(player.getUniqueId(), item.id(), BazaarOrderSide.SELL, price, amount);
            match(item.id());
            return true;
        } catch (RuntimeException exception) {
            giveOrDrop(player, item.template(), amount);
            throw exception;
        }
    }

    public synchronized TradeResult instantBuy(Player player, BazaarItem item, int requested) {
        if (requested <= 0) return new TradeResult(0, 0);
        int bought = 0;
        long spent = 0;
        for (BazaarOrder sell : orderRepository.findOpenByItem(item.id(), BazaarOrderSide.SELL)) {
            if (bought >= requested) break;
            int quantity = Math.min(requested - bought, sell.remainingAmount());
            long cost;
            try { cost = Math.multiplyExact(sell.price(), (long) quantity); } catch (ArithmeticException exception) { break; }
            if (!coinManager.removeCoins(player.getUniqueId(), cost)) break;
            if (!canFit(player, item.template(), quantity)) {
                coinManager.addCoins(player.getUniqueId(), cost);
                break;
            }
            giveOrDrop(player, item.template(), quantity);
            orderRepository.addCoinClaim(sell.owner(), cost);
            orderRepository.updateRemaining(sell.id(), sell.remainingAmount() - quantity);
            bought += quantity;
            spent += cost;
        }
        return new TradeResult(bought, spent);
    }

    public synchronized TradeResult instantSell(Player player, BazaarItem item, int requested) {
        if (requested <= 0) return new TradeResult(0, 0);
        int owned = countMatching(player, item.template());
        int target = Math.min(requested, owned);
        int sold = 0;
        long earned = 0;
        for (BazaarOrder buy : orderRepository.findOpenByItem(item.id(), BazaarOrderSide.BUY)) {
            if (sold >= target) break;
            int quantity = Math.min(target - sold, buy.remainingAmount());
            long value;
            try { value = Math.multiplyExact(buy.price(), (long) quantity); } catch (ArithmeticException exception) { break; }
            removeMatching(player, item.template(), quantity);
            orderRepository.addItemClaim(buy.owner(), item.id(), quantity);
            orderRepository.updateRemaining(buy.id(), buy.remainingAmount() - quantity);
            sold += quantity;
            earned += value;
        }
        if (earned > 0) coinManager.addCoins(player.getUniqueId(), earned);
        return new TradeResult(sold, earned);
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
        if (amount <= 0 || !coinManager.addCoins(player.getUniqueId(), amount)) return 0;
        if (!orderRepository.clearCoinClaim(player.getUniqueId(), amount)) {
            coinManager.removeCoins(player.getUniqueId(), amount);
            return 0;
        }
        return amount;
    }

    public synchronized int claimItems(Player player, String itemId) {
        BazaarItem item = find(itemId).orElse(null);
        if (item == null) return 0;
        long available = itemClaims(player.getUniqueId()).stream().filter(c -> c.itemId().equals(itemId)).mapToLong(BazaarItemClaim::amount).findFirst().orElse(0);
        if (available <= 0) return 0;
        int deliver = (int) Math.min(Integer.MAX_VALUE, available);
        int fit = maxFit(player, item.template(), deliver);
        if (fit <= 0 || !orderRepository.decreaseItemClaim(player.getUniqueId(), itemId, fit)) return 0;
        giveOrDrop(player, item.template(), fit);
        return fit;
    }

    public int maxInstantBuy(Player player, BazaarItem item) {
        long coins = coinManager.getCoins(player.getUniqueId()).orElse(0L);
        int amount = 0;
        for (BazaarOrder order : orderRepository.findOpenByItem(item.id(), BazaarOrderSide.SELL)) {
            if (order.price() <= 0) continue;
            int affordable = (int) Math.min(order.remainingAmount(), coins / order.price());
            amount += affordable;
            coins -= affordable * order.price();
            if (affordable < order.remainingAmount()) break;
        }
        return amount;
    }

    public int countMatching(Player player, ItemStack template) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) if (stack != null && stack.isSimilar(template)) total += stack.getAmount();
        return total;
    }

    private boolean validOrder(long price, int amount) { return price > 0 && amount > 0; }

    private void removeMatching(Player player, ItemStack template, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !stack.isSimilar(template)) continue;
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) contents[i] = null;
            left -= take;
        }
        player.getInventory().setStorageContents(contents);
    }

    private boolean canFit(Player player, ItemStack template, int amount) { return maxFit(player, template, amount) >= amount; }

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
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
            left -= stack.getAmount();
        }
    }

    private String guessCategory(Material material) {
        String name = material.name();
        if (name.contains("ORE") || name.contains("INGOT") || name.contains("COAL") || name.contains("STONE")) return "MINING";
        if (name.contains("LOG") || name.contains("WOOD") || name.contains("STEM") || name.contains("HYPHAE")) return "FORAGING";
        if (name.contains("WHEAT") || name.contains("SEEDS") || name.contains("CARROT") || name.contains("POTATO") || name.contains("MELON") || name.contains("PUMPKIN")) return "FARMING";
        if (name.contains("NETHER") || name.contains("BLAZE") || name.contains("MAGMA")) return "NETHER";
        return "OTHER";
    }
}
