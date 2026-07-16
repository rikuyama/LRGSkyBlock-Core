package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.BaseStatsType;
import me.lrg.skyblock.core.model.CalculatedStats;
import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.model.StatsLayer;
import me.lrg.skyblock.core.model.StatsModifierData;
import me.lrg.skyblock.core.model.StatsLoadState;
import me.lrg.skyblock.core.model.StatsType;
import me.lrg.skyblock.core.repository.RepositoryException;
import me.lrg.skyblock.core.repository.StatsRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

public class StatsManager {

    private static final double DEFAULT_HEALTH = 100.0;
    private static final double DEFAULT_MANA = 100.0;
    private static final double DEFAULT_STRENGTH = 0.0;
    private static final double DEFAULT_DEFENSE = 0.0;
    private static final double DEFAULT_SPEED = 100.0;
    private static final double DEFAULT_CRITICAL_CHANCE = 30.0;
    private static final double DEFAULT_MAGIC_FIND = 0.0;

    private static final double MIN_HEALTH = 1.0;
    private static final double MAX_HEALTH = 10000.0;
    private static final double MAX_VISUAL_HEALTH = 40.0;
    private static final double SPEED_TO_MOVEMENT_SPEED_RATE = 0.001;
    private static final double MIN_MOVEMENT_SPEED = 0.0;
    private static final double MAX_MOVEMENT_SPEED = 1.0;

    private static final long ACTION_BAR_INTERVAL_TICKS = 20L;
    private static final int SAVE_RETRY_COUNT = 2;
    private static final long AUTO_SAVE_INTERVAL_TICKS = 20L * 60L * 5L;

    private final JavaPlugin plugin;
    private final StatsRepository statsRepository;
    private final Logger logger;

    private final ConcurrentMap<UUID, StatsData> statsDataMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, StatsLoadState> loadStateMap = new ConcurrentHashMap<>();

    private final Set<UUID> savingUuids = ConcurrentHashMap.newKeySet();
    private final StatsCalculationManager statsCalculationManager = new StatsCalculationManager();

    private BukkitTask actionBarTask;
    private BukkitTask autoSaveTask;

    public StatsManager(JavaPlugin plugin, StatsRepository statsRepository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.statsRepository = Objects.requireNonNull(statsRepository, "statsRepository");
        this.logger = plugin.getLogger();
    }

    public void loadStats(Player player) {
        Objects.requireNonNull(player, "player");

        UUID uuid = player.getUniqueId();
        StatsLoadState currentState = loadStateMap.getOrDefault(uuid, StatsLoadState.NOT_LOADED);

        if (currentState == StatsLoadState.LOADING || currentState == StatsLoadState.LOADED) {
            return;
        }

        loadStateMap.put(uuid, StatsLoadState.LOADING);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadStatsAsync(uuid));
    }

    public void saveAndRemoveStats(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        StatsData statsData = statsDataMap.get(uuid);
        loadStateMap.remove(uuid);
        statsCalculationManager.clearPlayer(uuid);

        if (statsData == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (saveStatsWithRetry(statsData)) {
                statsData.markClean();
                statsDataMap.remove(uuid, statsData);
                return;
            }

            logger.severe("ステータスデータを保存できなかったため、キャッシュを保持します。uuid=" + uuid);
        });
    }

    public Optional<StatsData> getStatsData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(statsDataMap.get(uuid));
    }

    public StatsLoadState getLoadState(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return loadStateMap.getOrDefault(uuid, StatsLoadState.NOT_LOADED);
    }

    public boolean isStatsLoaded(UUID uuid) {
        return getLoadState(uuid) == StatsLoadState.LOADED && statsDataMap.containsKey(uuid);
    }

    public boolean isStatsLoading(UUID uuid) {
        return getLoadState(uuid) == StatsLoadState.LOADING;
    }

    public boolean hasStatsLoadFailed(UUID uuid) {
        return getLoadState(uuid) == StatsLoadState.FAILED;
    }

    public void cacheStats(StatsData statsData) {
        Objects.requireNonNull(statsData, "statsData");
        statsData.markClean();
        statsDataMap.put(statsData.getUuid(), statsData);
        loadStateMap.put(statsData.getUuid(), StatsLoadState.LOADED);
    }

    public Optional<StatsData> removeStatsData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        loadStateMap.remove(uuid);
        statsCalculationManager.clearPlayer(uuid);
        return Optional.ofNullable(statsDataMap.remove(uuid));
    }

    public Collection<StatsData> getCachedStats() {
        return List.copyOf(statsDataMap.values());
    }

    public void saveAllSynchronously() {
        for (StatsData statsData : getCachedStats()) {
            if (!statsData.isDirty()) {
                continue;
            }
            if (saveStatsWithRetry(statsData)) {
                statsData.markClean();
            }
        }
    }

    public void saveDirtyStatsAsynchronously() {
        for (StatsData statsData : getCachedStats()) {
            if (!statsData.isDirty()) {
                continue;
            }

            UUID uuid = statsData.getUuid();
            if (!savingUuids.add(uuid)) {
                continue;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (saveStatsWithRetry(statsData)) {
                        statsData.markClean();
                    }
                } finally {
                    savingUuids.remove(uuid);
                }
            });
        }
    }

    public int getCachedPlayerCount() {
        return statsDataMap.size();
    }

    public int getDirtyPlayerCount() {
        return (int) statsDataMap.values().stream().filter(StatsData::isDirty).count();
    }

    public int getLoadingPlayerCount() {
        return (int) loadStateMap.values().stream().filter(state -> state == StatsLoadState.LOADING).count();
    }

    public int getFailedPlayerCount() {
        return (int) loadStateMap.values().stream().filter(state -> state == StatsLoadState.FAILED).count();
    }

    public int getSavingPlayerCount() {
        return savingUuids.size();
    }

    public void clear() {
        statsDataMap.clear();
        loadStateMap.clear();
        savingUuids.clear();
        statsCalculationManager.clear();
    }

    public void startActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            return;
        }

        this.actionBarTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::sendActionBarToOnlinePlayers,
                ACTION_BAR_INTERVAL_TICKS,
                ACTION_BAR_INTERVAL_TICKS
        );

        logger.info("[LRG] Stats ActionBar task started.");
    }

    public void startAutoSaveTask() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            return;
        }

        this.autoSaveTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::saveDirtyStatsAsynchronously,
                AUTO_SAVE_INTERVAL_TICKS,
                AUTO_SAVE_INTERVAL_TICKS
        );

        logger.info("[LRG] Stats auto-save task started.");
    }

    public void stopAutoSaveTask() {
        if (autoSaveTask == null) {
            return;
        }

        autoSaveTask.cancel();
        autoSaveTask = null;
        logger.info("[LRG] Stats auto-save task stopped.");
    }

    public void stopActionBarTask() {
        if (actionBarTask == null) {
            return;
        }

        actionBarTask.cancel();
        actionBarTask = null;
        logger.info("[LRG] Stats ActionBar task stopped.");
    }

    public boolean applyStatsToPlayer(Player player) {
        Objects.requireNonNull(player, "player");

        Optional<StatsData> statsDataOptional = getStatsData(player.getUniqueId());
        if (statsDataOptional.isEmpty()) {
            return false;
        }

        CalculatedStats calculatedStats = statsCalculationManager.calculate(statsDataOptional.get());
        applyHealth(player, calculatedStats);
        applySpeed(player, calculatedStats);
        sendHealthManaActionBar(player, calculatedStats);
        return true;
    }

    public double calculateAttackDamage(Player attacker, double baseDamage) {
        Objects.requireNonNull(attacker, "attacker");

        Optional<StatsData> statsDataOptional = getStatsData(attacker.getUniqueId());
        if (statsDataOptional.isEmpty()) {
            return baseDamage;
        }

        CalculatedStats calculatedStats = statsCalculationManager.calculate(statsDataOptional.get());
        double strength = Math.max(0.0, calculatedStats.getStrength());
        double damage = baseDamage * (1.0 + (strength / 100.0));

        if (rollCritical(calculatedStats.getCriticalChance())) {
            double critDamage = Math.max(0.0, calculatedStats.getExtraStat(StatsType.CRIT_DAMAGE));
            damage *= 1.0 + (critDamage / 100.0);
            attacker.sendMessage("§6✧ クリティカル！");
        }

        return damage;
    }

    public double calculateDefenseDamage(Player defender, double incomingDamage) {
        Objects.requireNonNull(defender, "defender");

        return getStatsData(defender.getUniqueId())
                .map(statsCalculationManager::calculate)
                .map(calculatedStats -> {
                    double defense = Math.max(0.0, calculatedStats.getDefense());
                    return incomingDamage * (100.0 / (100.0 + defense));
                })
                .orElse(incomingDamage);
    }

    public double getMagicFind(Player player) {
        Objects.requireNonNull(player, "player");
        return getStatsData(player.getUniqueId())
                .map(statsCalculationManager::calculate)
                .map(CalculatedStats::getMagicFind)
                .orElse(0.0);
    }

    public double getExtraStat(Player player, StatsType statsType) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(statsType, "statsType");

        return getStatsData(player.getUniqueId())
                .map(statsCalculationManager::calculate)
                .map(calculatedStats -> calculatedStats.getExtraStat(statsType))
                .orElse(statsType.getDefaultValue());
    }


    public Optional<CalculatedStats> getCalculatedStats(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return getStatsData(uuid).map(statsCalculationManager::calculate);
    }

    public void setLayerBaseStat(
            UUID uuid,
            StatsLayer layer,
            BaseStatsType statsType,
            double value
    ) {
        statsCalculationManager.setBaseStatModifier(uuid, layer, statsType, value);
        applyStatsIfOnline(uuid);
    }

    public void setLayerExtraStat(
            UUID uuid,
            StatsLayer layer,
            StatsType statsType,
            double value
    ) {
        statsCalculationManager.setExtraStatModifier(uuid, layer, statsType, value);
        applyStatsIfOnline(uuid);
    }

    public void replaceStatsLayer(UUID uuid, StatsLayer layer, StatsModifierData modifierData) {
        statsCalculationManager.replaceLayer(uuid, layer, modifierData);
        applyStatsIfOnline(uuid);
    }

    public void clearStatsLayer(UUID uuid, StatsLayer layer) {
        statsCalculationManager.clearLayer(uuid, layer);
        applyStatsIfOnline(uuid);
    }

    public StatsCalculationManager getStatsCalculationManager() {
        return statsCalculationManager;
    }

    public Map<StatsLayer, StatsModifierData> getStatsLayerSnapshot(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return statsCalculationManager.getLayerSnapshot(uuid);
    }

    public boolean recalculateStats(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return false;
        }
        return applyStatsToPlayer(player);
    }

    public void loadStatsForDebug(UUID uuid, Consumer<Optional<StatsData>> callback) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(callback, "callback");

        StatsData cached = statsDataMap.get(uuid);
        if (cached != null) {
            callback.accept(Optional.of(cached));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<StatsData> loaded;
            try {
                loaded = statsRepository.loadStats(uuid);
            } catch (RepositoryException exception) {
                logger.log(Level.SEVERE, "デバッグ用Stats読み込みに失敗しました。uuid=" + uuid, exception);
                loaded = Optional.empty();
            }

            Optional<StatsData> result = loaded;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void saveStatsForDebug(StatsData statsData, Consumer<Boolean> callback) {
        Objects.requireNonNull(statsData, "statsData");
        Objects.requireNonNull(callback, "callback");

        StatsData cached = statsDataMap.get(statsData.getUuid());
        if (cached == statsData) {
            statsData.markDirty();
            applyStatsIfOnline(statsData.getUuid());
            callback.accept(true);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = saveStatsWithRetry(statsData);
            if (success) {
                statsData.markClean();
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
        });
    }

    public StatsData createDefaultStatsForDebug(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return createDefaultStatsData(uuid);
    }

    private void applyStatsIfOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyStatsToPlayer(player);
        }
    }

    private void loadStatsAsync(UUID uuid) {
        try {
            StatsData statsData = loadOrCreateStats(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> cacheAndApplyIfOnline(uuid, statsData));
        } catch (RepositoryException exception) {
            markLoadFailed(uuid, "ステータスデータの読み込みに失敗しました。", exception);
        } catch (Exception exception) {
            markLoadFailed(uuid, "ステータスデータの読み込み中に想定外のエラーが発生しました。", exception);
        }
    }

    private StatsData loadOrCreateStats(UUID uuid) {
        Optional<StatsData> loaded = statsRepository.loadStats(uuid);
        if (loaded.isPresent()) {
            return loaded.get();
        }

        StatsData newStatsData = createDefaultStatsData(uuid);
        statsRepository.createStats(newStatsData);
        return newStatsData;
    }

    private void cacheAndApplyIfOnline(UUID uuid, StatsData statsData) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            loadStateMap.remove(uuid);
            return;
        }

        cacheStats(statsData);
        applyStatsToPlayer(player);
        logger.info("[LRG] StatsData loaded and applied. uuid=" + uuid);
    }

    private void markLoadFailed(UUID uuid, String message, Exception exception) {
        loadStateMap.put(uuid, StatsLoadState.FAILED);
        logger.log(Level.SEVERE, message + " uuid=" + uuid, exception);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage("§cステータスデータの読み込みに失敗しました。再ログインしてください。");
            }
        });
    }

    private boolean saveStatsWithRetry(StatsData statsData) {
        RepositoryException lastException = null;

        for (int attempt = 1; attempt <= SAVE_RETRY_COUNT + 1; attempt++) {
            try {
                statsRepository.saveStats(statsData);
                return true;
            } catch (RepositoryException exception) {
                lastException = exception;
                logger.log(
                        Level.WARNING,
                        "ステータスデータの保存に失敗しました。再試行します。uuid="
                                + statsData.getUuid() + ", attempt=" + attempt,
                        exception
                );
            }
        }

        logger.log(
                Level.SEVERE,
                "ステータスデータの保存に最終的に失敗しました。uuid=" + statsData.getUuid(),
                lastException
        );
        return false;
    }

    private StatsData createDefaultStatsData(UUID uuid) {
        return new StatsData(
                uuid,
                DEFAULT_HEALTH,
                DEFAULT_MANA,
                DEFAULT_STRENGTH,
                DEFAULT_DEFENSE,
                DEFAULT_SPEED,
                DEFAULT_CRITICAL_CHANCE,
                DEFAULT_MAGIC_FIND
        );
    }

    private void applyHealth(Player player, CalculatedStats statsData) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute == null) {
            logger.warning("GENERIC_MAX_HEALTH が取得できませんでした。player=" + player.getName());
            return;
        }

        double statsMaxHealth = clamp(statsData.getHealth(), MIN_HEALTH, MAX_HEALTH);
        double visualMaxHealth = Math.min(statsMaxHealth, MAX_VISUAL_HEALTH);
        maxHealthAttribute.setBaseValue(visualMaxHealth);

        if (player.getHealth() > visualMaxHealth) {
            player.setHealth(visualMaxHealth);
        }
    }

    private void applySpeed(Player player, CalculatedStats statsData) {
        AttributeInstance movementSpeedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeedAttribute == null) {
            logger.warning("GENERIC_MOVEMENT_SPEED が取得できませんでした。player=" + player.getName());
            return;
        }

        double movementSpeed = clamp(
                statsData.getSpeed() * SPEED_TO_MOVEMENT_SPEED_RATE,
                MIN_MOVEMENT_SPEED,
                MAX_MOVEMENT_SPEED
        );
        movementSpeedAttribute.setBaseValue(movementSpeed);
    }

    private void sendActionBarToOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            getStatsData(player.getUniqueId())
                    .map(statsCalculationManager::calculate)
                    .ifPresent(calculatedStats -> sendHealthManaActionBar(player, calculatedStats));
        }
    }

    private void sendHealthManaActionBar(Player player, CalculatedStats statsData) {
        int displayedHealth = (int) Math.round(statsData.getHealth());
        int displayedMana = (int) Math.round(statsData.getMana());

        player.sendActionBar(Component.text(
                "§c❤ " + displayedHealth + " §8| §b✎ " + displayedMana
        ));
    }

    private boolean rollCritical(double criticalChance) {
        double clampedChance = clamp(criticalChance, 0.0, 100.0);
        return ThreadLocalRandom.current().nextDouble(100.0) < clampedChance;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
