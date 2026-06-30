package me.lrg.skyblock.core.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * プレイヤー初期設定をconfig.ymlから読み込むクラス。
 *
 * このクラスの役割:
 * - 初期Coinsを読み込む
 * - データ読み込み失敗時のキックメッセージを読み込む
 *
 * 注意:
 * - SQLは書かない
 * - イベント処理は書かない
 * - プレイヤーデータは保持しない
 */
public record PlayerDefaultSettings(
        long defaultCoins,
        String dataLoadFailedKickMessage
) {

    private static final String PATH_DEFAULT_COINS = "player.default-coins";
    private static final String PATH_DATA_LOAD_FAILED_KICK_MESSAGE = "messages.data-load-failed-kick";

    private static final long FALLBACK_DEFAULT_COINS = 0L;
    private static final String FALLBACK_DATA_LOAD_FAILED_KICK_MESSAGE =
            "&cプレイヤーデータの読み込みに失敗しました。&7時間をおいてもう一度参加してください。";

    public PlayerDefaultSettings {
        if (defaultCoins < 0L) {
            throw new IllegalArgumentException("defaultCoins must be 0 or greater.");
        }

        Objects.requireNonNull(dataLoadFailedKickMessage, "dataLoadFailedKickMessage");

        if (dataLoadFailedKickMessage.isBlank()) {
            throw new IllegalArgumentException("dataLoadFailedKickMessage must not be blank.");
        }
    }

    public static PlayerDefaultSettings from(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        long defaultCoins = plugin.getConfig().getLong(
                PATH_DEFAULT_COINS,
                FALLBACK_DEFAULT_COINS
        );

        String dataLoadFailedKickMessage = plugin.getConfig().getString(
                PATH_DATA_LOAD_FAILED_KICK_MESSAGE,
                FALLBACK_DATA_LOAD_FAILED_KICK_MESSAGE
        );

        return new PlayerDefaultSettings(defaultCoins, dataLoadFailedKickMessage);
    }
}