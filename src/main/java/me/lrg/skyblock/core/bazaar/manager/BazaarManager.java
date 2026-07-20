package me.lrg.skyblock.core.bazaar.manager;

import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import me.lrg.skyblock.core.bazaar.repository.BazaarRepository;
import me.lrg.skyblock.core.manager.CoinManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class BazaarManager {
    private final BazaarRepository repository;
    private final CoinManager coinManager;
    private final Map<String, BazaarItem> items = new LinkedHashMap<>();

    public BazaarManager(BazaarRepository repository, CoinManager coinManager) {
        this.repository = repository;
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

    public boolean buy(Player player, BazaarItem item, int amount) {
        long cost;
        try { cost = Math.multiplyExact(item.buyPrice(), amount); } catch (ArithmeticException ex) { return false; }
        if (!coinManager.removeCoins(player.getUniqueId(), cost)) return false;
        ItemStack stack = item.template();
        stack.setAmount(amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            coinManager.addCoins(player.getUniqueId(), cost);
            removeMatching(player, item.template(), amount - overflow.values().stream().mapToInt(ItemStack::getAmount).sum());
            return false;
        }
        return true;
    }

    public int sell(Player player, BazaarItem item, int requested) {
        int owned = countMatching(player, item.template());
        int amount = Math.min(owned, requested);
        if (amount <= 0) return 0;
        removeMatching(player, item.template(), amount);
        coinManager.addCoins(player.getUniqueId(), item.sellPrice() * amount);
        return amount;
    }

    public int countMatching(Player player, ItemStack template) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) if (stack != null && stack.isSimilar(template)) total += stack.getAmount();
        return total;
    }

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

    private String guessCategory(Material material) {
        String name = material.name();
        if (name.contains("ORE") || name.contains("INGOT") || name.contains("COAL") || name.contains("STONE")) return "MINING";
        if (name.contains("LOG") || name.contains("WOOD") || name.contains("STEM") || name.contains("HYPHAE")) return "FORAGING";
        if (name.contains("WHEAT") || name.contains("SEEDS") || name.contains("CARROT") || name.contains("POTATO") || name.contains("MELON") || name.contains("PUMPKIN")) return "FARMING";
        if (name.contains("NETHER") || name.contains("BLAZE") || name.contains("MAGMA")) return "NETHER";
        return "OTHER";
    }
}
