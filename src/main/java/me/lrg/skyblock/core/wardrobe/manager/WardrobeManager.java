package me.lrg.skyblock.core.wardrobe.manager;

import me.lrg.skyblock.core.wardrobe.model.WardrobeSet;
import me.lrg.skyblock.core.wardrobe.repository.WardrobeRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WardrobeManager {
    public static final int SLOT_COUNT = 9;

    private final JavaPlugin plugin;
    private final WardrobeRepository repository;
    private final ConcurrentMap<UUID, Map<Integer, WardrobeSet>> cache = new ConcurrentHashMap<>();

    public WardrobeManager(JavaPlugin plugin, WardrobeRepository repository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, WardrobeSet> loaded = new ConcurrentHashMap<>(repository.load(uuid));
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    cache.put(uuid, loaded);
                }
            });
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public Optional<WardrobeSet> getSet(UUID uuid, int slot) {
        validateSlot(slot);
        Map<Integer, WardrobeSet> sets = cache.get(uuid);
        return sets == null ? Optional.empty() : Optional.ofNullable(sets.get(slot));
    }

    public boolean storeEquippedArmor(Player player, int slot) {
        validateSlot(slot);
        Map<Integer, WardrobeSet> sets = cache.get(player.getUniqueId());
        if (sets == null || sets.containsKey(slot)) {
            return false;
        }
        WardrobeSet set = new WardrobeSet(player.getInventory().getArmorContents());
        if (set.isEmpty()) {
            return false;
        }
        sets.put(slot, set);
        player.getInventory().setArmorContents(new ItemStack[4]);
        saveAsync(player.getUniqueId(), slot, set);
        return true;
    }

    public SwapResult equipSet(Player player, int slot) {
        validateSlot(slot);
        Map<Integer, WardrobeSet> sets = cache.get(player.getUniqueId());
        if (sets == null) {
            return SwapResult.NOT_LOADED;
        }
        WardrobeSet target = sets.get(slot);
        if (target == null || target.isEmpty()) {
            return SwapResult.EMPTY_SLOT;
        }

        WardrobeSet current = new WardrobeSet(player.getInventory().getArmorContents());
        player.getInventory().setArmorContents(target.getArmor());
        if (current.isEmpty()) {
            sets.remove(slot);
            saveAsync(player.getUniqueId(), slot, WardrobeSet.empty());
        } else {
            sets.put(slot, current);
            saveAsync(player.getUniqueId(), slot, current);
        }
        return SwapResult.SUCCESS;
    }

    private void saveAsync(UUID uuid, int slot, WardrobeSet set) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.save(uuid, slot, set));
    }

    private static void validateSlot(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) {
            throw new IllegalArgumentException("slot must be between 1 and " + SLOT_COUNT);
        }
    }

    public enum SwapResult {
        SUCCESS,
        NOT_LOADED,
        EMPTY_SLOT
    }
}
