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
import org.bukkit.scheduler.BukkitTask;

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
 * - StatsDataをメモリに保存する
 * - Health / Speed をMinecraft本体へ反映する
 * - Health / Mana をActionBarへ常時表示する
 * - Strengthを攻撃ダメージ計算に使う
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
     * Minecraft GUI上のハート表示上限。
     *
     * 40HP = 20ハート = 最大2段くらい。
     * Stats上のHealthが200や500でも、GUIのハートは40HPまでに制限する。
     */
    private static final double MAX_VISUAL_HEALTH = 40.0;

    /**
     * Minecraftの通常移動速度はだいたい0.1。
     * LRG SkyBlock側のspeed=100を0.1に変換する。
     */
    private static final double SPEED_TO_MOVEMENT_SPEED_RATE = 0.001;

    private static final double MIN_MOVEMENT_SPEED = 0.0;
    private static final double MAX_MOVEMENT_SPEED = 1.0;

    /**
     * ActionBar更新間隔。
     * 20 ticks = 1秒。
     */
    private static final long ACTION_BAR_INTERVAL_TICKS = 20L;

    private final JavaPlugin plugin;
    private final StatsRepository statsRepository;
    private final Logger logger;

    private final ConcurrentMap<UUID, StatsData> statsDataMap = new ConcurrentHashMap<>();

    private BukkitTask actionBarTask;

    public StatsManager(JavaPlugin plugin, StatsRepository statsRepository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.statsRepository = Objects.requireNonNull(statsRepository, "statsRepository");
        this.logger = plugin.getLogger();
    }

    /**
     * プレイヤー参加時に呼ぶ。
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
     * ActionBar常時表示タスクを開始する。
     */
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

    /**
     * ActionBar常時表示タスクを停止する。
     */
    public void stopActionBarTask() {
        if (actionBarTask == null) {
            return;
        }

        actionBarTask.cancel();
        actionBarTask = null;

        logger.info("[LRG] Stats ActionBar task stopped.");
    }

    /**
     * StatsDataをMinecraft本体へ反映する。
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
        sendHealthManaActionBar(player, statsData);

        return true;
    }

    /**
     * Strengthを使って攻撃ダメージを計算する。
     *
     * 仮仕様:
     * finalDamage = baseDamage * (1 + strength / 100)
     *
     * @param attacker 攻撃者
     * @param baseDamage 元ダメージ
     * @return Strength反映後のダメージ
     */
    public double calculateStrengthDamage(Player attacker, double baseDamage) {
        Objects.requireNonNull(attacker, "attacker");

        Optional<StatsData> statsDataOptional = getStatsData(attacker.getUniqueId());

        if (statsDataOptional.isEmpty()) {
            return baseDamage;
        }

        StatsData statsData = statsDataOptional.get();

        double strength = Math.max(0.0, statsData.getStrength());
        double multiplier = 1.0 + (strength / 100.0);

        return baseDamage * multiplier;
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

    /**
     * HealthをMinecraft本体へ反映する。
     *
     * Stats上のHealthは100, 200, 500など大きくできる。
     * ただしGUIのハートが増えすぎないよう、Minecraft上の最大体力は最大40HPまでに制限する。
     */
    private void applyHealth(Player player, StatsData statsData) {
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

    private void sendActionBarToOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<StatsData> statsDataOptional = getStatsData(player.getUniqueId());

            if (statsDataOptional.isEmpty()) {
                continue;
            }

            sendHealthManaActionBar(player, statsDataOptional.get());
        }
    }

    /**
     * Health / Mana をActionBarに表示する。
     *
     * 表示は小数点なし。
     * Minecraft上の実HPは最大40までだが、表示上はStatsのHealthにスケールする。
     *
     * 例:
     * Stats Health = 200
     * Minecraft実HP = 40 / 40
     * 表示 = 200 / 200
     */
    private void sendHealthManaActionBar(Player player, StatsData statsData) {
        double statsMaxHealth = clamp(statsData.getHealth(), MIN_HEALTH, MAX_HEALTH);
        double visualMaxHealth = Math.min(statsMaxHealth, MAX_VISUAL_HEALTH);

        double healthRate = player.getHealth() / visualMaxHealth;
        double displayCurrentHealth = statsMaxHealth * healthRate;

        String text = "§c❤ " + formatInteger(displayCurrentHealth) + " / " + formatInteger(statsMaxHealth)
                + "   §b✎ Mana " + formatInteger(statsData.getMana());

        player.sendActionBar(Component.text(text));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private String formatInteger(double value) {
        return String.valueOf(Math.round(value));
    }
}