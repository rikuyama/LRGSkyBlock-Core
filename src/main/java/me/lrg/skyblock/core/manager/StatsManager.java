package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.repository.RepositoryException;
import me.lrg.skyblock.core.repository.StatsRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

/**
 * オンライン中プレイヤーのStatsDataを管理するManager。
 *
 * このクラスの役割:
 * - 参加時にStatsDataを読み込む
 * - 初期StatsDataを作成する
 * - StatsDataをメモリに保存する
 * - StatsをMinecraft本体へ反映する
 * - 退出時や停止時にStatsDataを保存する
 *
 * 注意:
 * - SQLは書かない
 * - SQLはStatsRepositoryに任せる
 * - Bukkit API操作はメインスレッドで実行する
 */
public class StatsManager {

    private static final double DEFAULT_HEALTH = 100.0;
    private static final double DEFAULT_MANA = 100.0;
    private static final double DEFAULT_STRENGTH = 0.0;
    private static final double DEFAULT_DEFENSE = 0.0;
    private static final double DEFAULT_SPEED = 100.0;

    private static final double MIN_HEALTH = 1.0;
    private static final double MAX_HEALTH = 10000.0;

    /**
     * Minecraftの通常移動速度はだいたい0.1。
     * LRG SkyBlock側のspeed=100を0.1に変換する。
     */
    private static final double SPEED_TO_MOVEMENT_SPEED_RATE = 0.001;

    private static final double MIN_MOVEMENT_SPEED = 0.0;
    private static final double MAX_MOVEMENT_SPEED = 1.0;

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
     * DB処理は非同期で実行し、Minecraft本体への反映はメインスレッドで行う。
     *
     * @param player 参加したプレイヤー
     */
    public void loadStats(Player player) {
        Objects.requireNonNull(player, "player");

        UUID uuid = player.getUniqueId();

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

    /**
     * キャッシュ済みStatsDataをMinecraft本体へ反映する。
     *
     * @param player 反映対象プレイヤー
     * @return 反映成功ならtrue
     */
    public boolean applyStatsToPlayer(Player player) {
        Objects.requireNonNull(player, "player");

        Optional<StatsData> statsDataOptional = getStatsData(player.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            return false;
        }

        StatsData statsData = statsDataOptional.get();

        applyHealth(player, statsData);
        applySpeed(player, statsData);
        sendManaActionBar(player, statsData);

        return true;
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

            Bukkit.getScheduler().runTask(plugin, () -> cacheAndApplyIfOnline(uuid, statsData));
        } catch (RepositoryException exception) {
            logger.log(Level.SEVERE, "ステータスデータの読み込みに失敗しました。uuid=" + uuid, exception);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "ステータスデータの読み込み中に想定外のエラーが発生しました。uuid=" + uuid, exception);
        }
    }

    private void cacheAndApplyIfOnline(UUID uuid, StatsData statsData) {
        Player player = Bukkit.getPlayer(uuid);

        if (player == null || !player.isOnline()) {
            return;
        }

        cacheStats(statsData);
        applyStatsToPlayer(player);

        logger.info("[LRG] StatsData loaded and applied. uuid=" + uuid);
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

    private void applyHealth(Player player, StatsData statsData) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealthAttribute == null) {
            logger.warning("GENERIC_MAX_HEALTH が取得できませんでした。player=" + player.getName());
            return;
        }

        double maxHealth = clamp(statsData.getHealth(), MIN_HEALTH, MAX_HEALTH);

        maxHealthAttribute.setBaseValue(maxHealth);

        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    private void applySpeed(Player player, StatsData statsData) {
        AttributeInstance movementSpeedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

        if (movementSpeedAttribute == null) {
            logger.warning("GENERIC_MOVEMENT_SPEED が取得できませんでした。player=" + player.getName());
            return;
        }

        double movementSpeed = statsData.getSpeed() * SPEED_TO_MOVEMENT_SPEED_RATE;
        double clampedMovementSpeed = clamp(movementSpeed, MIN_MOVEMENT_SPEED, MAX_MOVEMENT_SPEED);

        movementSpeedAttribute.setBaseValue(clampedMovementSpeed);
    }

    private void sendManaActionBar(Player player, StatsData statsData) {
        String text = "§c❤ " + format(statsData.getHealth())
                + " §b✎ Mana " + format(statsData.getMana())
                + " §f✦ Speed " + format(statsData.getSpeed());

        player.sendActionBar(Component.text(text));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private String format(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }

        return String.format("%.2f", value);
    }
}