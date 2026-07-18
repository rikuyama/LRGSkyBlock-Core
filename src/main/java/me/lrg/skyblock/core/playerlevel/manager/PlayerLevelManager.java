package me.lrg.skyblock.core.playerlevel.manager;

import me.lrg.skyblock.core.playerlevel.api.PlayerLevelXpReason;
import me.lrg.skyblock.core.playerlevel.api.PlayerLevelXpResult;
import me.lrg.skyblock.core.playerlevel.event.PlayerLevelLoadedEvent;
import me.lrg.skyblock.core.playerlevel.event.PlayerLevelUpEvent;
import me.lrg.skyblock.core.playerlevel.formula.PlayerLevelFormula;
import me.lrg.skyblock.core.playerlevel.model.PlayerLevelData;
import me.lrg.skyblock.core.playerlevel.repository.PlayerLevelRepository;
import me.lrg.skyblock.core.repository.RepositoryException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerLevelManager {

    private final JavaPlugin plugin;
    private final PlayerLevelRepository repository;
    private final Logger logger;
    private final ConcurrentMap<UUID, PlayerLevelData> cache = new ConcurrentHashMap<>();

    public PlayerLevelManager(JavaPlugin plugin, PlayerLevelRepository repository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.logger = plugin.getLogger();
    }

    public void load(Player player) {
        Objects.requireNonNull(player, "player");
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadAsync(uuid));
    }

    public void saveAndRemove(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        PlayerLevelData data = cache.remove(uuid);
        if (data == null || !data.isDirty()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> save(data));
    }

    public Optional<PlayerLevelData> getData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(cache.get(uuid));
    }

    public int getLevel(UUID uuid) {
        return getData(uuid).map(PlayerLevelData::getLevel).orElse(1);
    }

    public long getCurrentXp(UUID uuid) {
        return getData(uuid).map(PlayerLevelData::getCurrentXp).orElse(0L);
    }

    public long getTotalXp(UUID uuid) {
        return getData(uuid).map(PlayerLevelData::getTotalXp).orElse(0L);
    }

    public long getRequiredXp(int level) {
        return PlayerLevelFormula.getRequiredXp(level);
    }

    public boolean addXp(UUID uuid, long amount) {
        return addXp(uuid, amount, PlayerLevelXpReason.OTHER).success();
    }

    public PlayerLevelXpResult addXp(UUID uuid, long amount, PlayerLevelXpReason reason) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(reason, "reason");
        if (amount < 0L) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        PlayerLevelData data = cache.get(uuid);
        if (data == null) {
            return new PlayerLevelXpResult(false, 1, 1, 0L, 0L, getRequiredXp(1), reason);
        }
        int oldLevel = data.getLevel();
        data.addXp(amount);
        int newLevel = data.getLevel();
        if (newLevel > oldLevel) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, oldLevel, newLevel, reason));
            }
        }
        return new PlayerLevelXpResult(true, oldLevel, newLevel, amount,
                data.getCurrentXp(), data.getRequiredXp(), reason);
    }

    public long getXpUntilNextLevel(UUID uuid) {
        PlayerLevelData data = cache.get(uuid);
        if (data == null) return getRequiredXp(1);
        return Math.max(0L, data.getRequiredXp() - data.getCurrentXp());
    }

    public boolean removeXp(UUID uuid, long amount) {
        return mutate(uuid, data -> data.removeXp(amount));
    }

    public boolean setXp(UUID uuid, long xp) {
        return mutate(uuid, data -> data.setCurrentXp(xp));
    }

    public boolean setLevel(UUID uuid, int level) {
        return mutate(uuid, data -> data.setLevel(level));
    }

    public Collection<PlayerLevelData> getCachedData() {
        return List.copyOf(cache.values());
    }

    public void saveAllSynchronously() {
        for (PlayerLevelData data : getCachedData()) {
            if (data.isDirty()) {
                save(data);
            }
        }
    }

    public void clear() {
        cache.clear();
    }

    private void loadAsync(UUID uuid) {
        try {
            PlayerLevelData data = repository.load(uuid).orElseGet(() -> createDefault(uuid));
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    data.markClean();
                    cache.put(uuid, data);
                    Bukkit.getPluginManager().callEvent(new PlayerLevelLoadedEvent(player));
                }
            });
        } catch (RepositoryException exception) {
            logger.log(Level.SEVERE, "Player Levelの読み込みに失敗しました。uuid=" + uuid, exception);
        }
    }

    private PlayerLevelData createDefault(UUID uuid) {
        PlayerLevelData data = PlayerLevelData.createDefault(uuid);
        repository.create(data);
        return data;
    }

    private void save(PlayerLevelData data) {
        try {
            repository.save(data);
            data.markClean();
        } catch (RepositoryException exception) {
            logger.log(Level.SEVERE, "Player Levelの保存に失敗しました。uuid=" + data.getUuid(), exception);
        }
    }

    private boolean mutate(UUID uuid, DataMutation mutation) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(mutation, "mutation");
        PlayerLevelData data = cache.get(uuid);
        if (data == null) {
            return false;
        }
        mutation.apply(data);
        return true;
    }

    @FunctionalInterface
    private interface DataMutation {
        void apply(PlayerLevelData data);
    }
}
