package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.PlayerData;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * プレイヤーのCoinsを操作するManager。
 *
 * このクラスの役割:
 * - Coinsを取得する
 * - Coinsを増やす
 * - Coinsを減らす
 * - Coinsを指定値にする
 * - Coinsが足りるか確認する
 *
 * 注意:
 * - SQLは書かない
 * - DB保存はPlayerRepository.savePlayer()に任せる
 * - PlayerDataのcoinsだけを操作する
 */
public class CoinManager {

    private final PlayerManager playerManager;

    public CoinManager(PlayerManager playerManager) {
        this.playerManager = Objects.requireNonNull(playerManager, "playerManager");
    }

    /**
     * プレイヤーのCoinsを取得する。
     *
     * @param uuid プレイヤーUUID
     * @return Coins。オンラインデータがなければOptional.empty()
     */
    public Optional<Long> getCoins(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        return playerManager.getPlayerData(uuid)
                .map(PlayerData::getCoins);
    }

    /**
     * プレイヤーのCoinsを指定値にする。
     *
     * @param uuid プレイヤーUUID
     * @param amount 設定するCoins
     * @return 成功したらtrue
     */
    public boolean setCoins(UUID uuid, long amount) {
        Objects.requireNonNull(uuid, "uuid");

        if (amount < 0L) {
            return false;
        }

        Optional<PlayerData> playerDataOptional = playerManager.getPlayerData(uuid);

        if (playerDataOptional.isEmpty()) {
            return false;
        }

        PlayerData playerData = playerDataOptional.get();
        playerData.setCoins(amount);
        return true;
    }

    /**
     * プレイヤーのCoinsを増やす。
     *
     * @param uuid プレイヤーUUID
     * @param amount 増やすCoins
     * @return 成功したらtrue
     */
    public boolean addCoins(UUID uuid, long amount) {
        Objects.requireNonNull(uuid, "uuid");

        if (amount <= 0L) {
            return false;
        }

        Optional<PlayerData> playerDataOptional = playerManager.getPlayerData(uuid);

        if (playerDataOptional.isEmpty()) {
            return false;
        }

        PlayerData playerData = playerDataOptional.get();
        long currentCoins = playerData.getCoins();

        if (Long.MAX_VALUE - currentCoins < amount) {
            return false;
        }

        playerData.setCoins(currentCoins + amount);
        return true;
    }

    /**
     * プレイヤーのCoinsを減らす。
     *
     * @param uuid プレイヤーUUID
     * @param amount 減らすCoins
     * @return 成功したらtrue
     */
    public boolean removeCoins(UUID uuid, long amount) {
        Objects.requireNonNull(uuid, "uuid");

        if (amount <= 0L) {
            return false;
        }

        Optional<PlayerData> playerDataOptional = playerManager.getPlayerData(uuid);

        if (playerDataOptional.isEmpty()) {
            return false;
        }

        PlayerData playerData = playerDataOptional.get();

        if (playerData.getCoins() < amount) {
            return false;
        }

        playerData.setCoins(playerData.getCoins() - amount);
        return true;
    }

    /**
     * 指定Coins以上持っているか確認する。
     *
     * @param uuid プレイヤーUUID
     * @param amount 必要Coins
     * @return 足りていればtrue
     */
    public boolean hasEnoughCoins(UUID uuid, long amount) {
        Objects.requireNonNull(uuid, "uuid");

        if (amount < 0L) {
            return false;
        }

        return playerManager.getPlayerData(uuid)
                .map(playerData -> playerData.getCoins() >= amount)
                .orElse(false);
    }
}