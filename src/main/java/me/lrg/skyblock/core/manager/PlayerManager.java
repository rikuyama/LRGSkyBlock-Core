package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.config.PlayerDefaultSettings;
import me.lrg.skyblock.core.model.PlayerData;
import me.lrg.skyblock.core.repository.PlayerRepository;
import me.lrg.skyblock.core.repository.RepositoryException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

/**
 * オンライン中プレイヤーのPlayerDataを管理するクラス。
 *
 * このクラスの役割:
 * - 参加時にPlayerDataを読み込む
 * - 読み込んだPlayerDataをメモリに保存する
 * - 退出時にPlayerDataをDBへ保存する
 *
 * 注意:
 * - SQLは書かない
 * - SQLはPlayerRepositoryに任せる
 * - Listenerから呼ばれる処理をまとめる
 */
public class PlayerManager {

    private final JavaPlugin plugin;
    private final PlayerRepository playerRepository;
    private final PlayerDefaultSettings playerDefaultSettings;
    private final Logger logger;

    private final ConcurrentMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public PlayerManager(
            JavaPlugin plugin,
            PlayerRepository playerRepository,
            PlayerDefaultSettings playerDefaultSettings
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.playerDefaultSettings = Objects.requireNonNull(playerDefaultSettings, "playerDefaultSettings");
        this.logger = plugin.getLogger();
    }

    /**
     * プレイヤー参加時に呼ぶ。
     *
     * DB処理は重い可能性があるため、非同期で実行する。
     */
    public void loadPlayer(Player player) {
        Objects.requireNonNull(player, "player");

        UUID uuid = player.getUniqueId();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerAsync(uuid, name));
    }

    /**
     * プレイヤー退出時に呼ぶ。
     *
     * メモリから削除してから、DBへ非同期保存する。
     */
    public void saveAndRemovePlayer(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        Optional<PlayerData> playerDataOptional = removePlayerData(uuid);

        playerDataOptional.ifPresent(playerData ->
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayer(playerData))
        );
    }

    /**
     * PlayerDataをメモリに保存する。
     */
    public void cachePlayer(PlayerData playerData) {
        Objects.requireNonNull(playerData, "playerData");
        playerDataMap.put(playerData.getUuid(), playerData);
    }

    /**
     * メモリからPlayerDataを取得する。
     */
    public Optional<PlayerData> getPlayerData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(playerDataMap.get(uuid));
    }

    /**
     * メモリにPlayerDataがあるか確認する。
     */
    public boolean hasPlayerData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return playerDataMap.containsKey(uuid);
    }

    /**
     * メモリからPlayerDataを削除する。
     */
    public Optional<PlayerData> removePlayerData(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(playerDataMap.remove(uuid));
    }

    /**
     * サーバー停止時に使う。
     * 現在メモリにある全PlayerDataを取得する。
     */
    public Collection<PlayerData> getCachedPlayers() {
        return List.copyOf(playerDataMap.values());
    }

    /**
     * サーバー停止時に、全プレイヤーデータを保存する。
     */
    public void saveAllSynchronously() {
        for (PlayerData playerData : getCachedPlayers()) {
            savePlayer(playerData);
        }
    }

    /**
     * キャッシュを全削除する。
     */
    public void clear() {
        playerDataMap.clear();
    }

    private void loadPlayerAsync(UUID uuid, String name) {
        logger.info("[LRG] PlayerData load start: " + uuid + " / " + name);

        try {
            boolean exists = playerRepository.existsByUuid(uuid);
            logger.info("[LRG] PlayerData exists: " + exists);

            if (!exists) {
                logger.info("[LRG] PlayerData not found. Creating new data: " + uuid);

                PlayerData newPlayerData = new PlayerData(
                        uuid,
                        name,
                        playerDefaultSettings.defaultCoins()
                );

                playerRepository.createPlayer(newPlayerData);
                logger.info("[LRG] PlayerData created: " + uuid);
            }

            PlayerData playerData = playerRepository.loadPlayer(uuid, name)
                    .orElseGet(() -> {
                        logger.warning("[LRG] loadPlayer returned empty. Creating fallback data: " + uuid);
                        return createAndReturnPlayerData(uuid, name);
                    });

            logger.info("[LRG] PlayerData loaded. coins=" + playerData.getCoins());

            Bukkit.getScheduler().runTask(plugin, () -> cachePlayerIfOnline(uuid, playerData));
        } catch (RepositoryException exception) {
            logger.log(Level.SEVERE, "[LRG] プレイヤーデータの読み込みに失敗しました。uuid=" + uuid, exception);
            Bukkit.getScheduler().runTask(plugin, () -> kickPlayerIfOnline(uuid));
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[LRG] 想定外のエラーが発生しました。uuid=" + uuid, exception);
            Bukkit.getScheduler().runTask(plugin, () -> kickPlayerIfOnline(uuid));
        }
    }

    private PlayerData createAndReturnPlayerData(UUID uuid, String name) {
        PlayerData playerData = new PlayerData(
                uuid,
                name,
                playerDefaultSettings.defaultCoins()
        );

        playerRepository.createPlayer(playerData);
        return playerData;
    }

    private void savePlayer(PlayerData playerData) {
        try {
            playerRepository.savePlayer(playerData);
        } catch (RepositoryException exception) {
            logger.log(
                    Level.SEVERE,
                    "プレイヤーデータの保存に失敗しました。uuid=" + playerData.getUuid(),
                    exception
            );
        }
    }

    private void cachePlayerIfOnline(UUID uuid, PlayerData playerData) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);

        if (onlinePlayer == null || !onlinePlayer.isOnline()) {
            return;
        }

        playerData.setName(onlinePlayer.getName());
        cachePlayer(playerData);
    }

    private void kickPlayerIfOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);

        if (player == null || !player.isOnline()) {
            return;
        }

        player.kick(
                LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(playerDefaultSettings.dataLoadFailedKickMessage())
        );
    }
}