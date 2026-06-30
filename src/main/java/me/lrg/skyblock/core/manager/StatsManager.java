package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.repository.RepositoryException;
import me.lrg.skyblock.core.repository.StatsRepository;
import org.bukkit.Bukkit;
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

/**
 * オンライン中プレイヤーのStatsDataを管理するManager。
 *
 * このクラスの役割:
 * - 参加時にStatsDataを読み込む
 * - 初期StatsDataを作成する
 * - StatsDataをメモリに保存する
 * - 退出時や停止時にStatsDataを保存する
 *
 * 注意:
 * - SQLは書かない
 * - SQLはStatsRepositoryに任せる
 */
public class StatsManager {

    private static final double DEFAULT_HEALTH = 100.0;
    private static final double DEFAULT_MANA = 100.0;
    private static final double DEFAULT_STRENGTH = 0.0;
    private static final double DEFAULT_DEFENSE = 0.0;
    private static final double DEFAULT_SPEED = 100.0;

    private final JavaPlugin plugin;
    private final StatsRepository statsRepository;
    private final Logger logger;

    private final ConcurrentMap<UUID, StatsData> statsDataMap = new ConcurrentHashMap<>();

    public StatsManager(JavaPlugin plugin, StatsRepository statsRepository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.statsRepository = Objects.requireNonNull(statsRepository, "statsRepository");
        this.logger = plugin.getLogger();
    }

    /**
     * プレイヤー参加時に呼ぶ。
     * DB処理は非同期で実行する。
     *
     * @param uuid プレイヤーUUID
     */
    public void loadStats(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadStatsAsync(uuid));
    }

    /**
     * プレイヤー退出時に呼ぶ。
     * キャッシュから削除してから非同期保存する。
     *
     * @param uuid プレイヤーUUID
     */
    public void saveAndRemoveStats(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        Optional<StatsData> statsDataOptional = removeStatsData(uuid);

        statsDataOptional.ifPresent(statsData ->
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveStats(statsData))
        );
    }

    public Optional<StatsData> getStatsData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(statsDataMap.get(uuid));
    }

    public void cacheStats(StatsData statsData) {
        Objects.requireNonNull(statsData, "statsData");
        statsDataMap.put(statsData.getUuid(), statsData);
    }

    public Optional<StatsData> removeStatsData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(statsDataMap.remove(uuid));
    }

    public Collection<StatsData> getCachedStats() {
        return List.copyOf(statsDataMap.values());
    }

    public void saveAllSynchronously() {
        for (StatsData statsData : getCachedStats()) {
            saveStats(statsData);
        }
    }

    public void clear() {
        statsDataMap.clear();
    }

    private void loadStatsAsync(UUID uuid) {
        try {
            if (!statsRepository.existsByUuid(uuid)) {
                StatsData newStatsData = createDefaultStatsData(uuid);
                statsRepository.createStats(newStatsData);
            }

            StatsData statsData = statsRepository.loadStats(uuid)
                    .orElseGet(() -> {
                        StatsData fallbackStatsData = createDefaultStatsData(uuid);
                        statsRepository.createStats(fallbackStatsData);
                        return fallbackStatsData;
                    });

            cacheStats(statsData);
            logger.info("[LRG] StatsData loaded. uuid=" + uuid);
        } catch (RepositoryException exception) {
            logger.log(Level.SEVERE, "ステータスデータの読み込みに失敗しました。uuid=" + uuid, exception);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "ステータスデータの読み込み中に想定外のエラーが発生しました。uuid=" + uuid, exception);
        }
    }

    private void saveStats(StatsData statsData) {
        try {
            statsRepository.saveStats(statsData);
        } catch (RepositoryException exception) {
            logger.log(Level.SEVERE, "ステータスデータの保存に失敗しました。uuid=" + statsData.getUuid(), exception);
        }
    }

    private StatsData createDefaultStatsData(UUID uuid) {
        return new StatsData(
                uuid,
                DEFAULT_HEALTH,
                DEFAULT_MANA,
                DEFAULT_STRENGTH,
                DEFAULT_DEFENSE,
                DEFAULT_SPEED
        );
    }
}