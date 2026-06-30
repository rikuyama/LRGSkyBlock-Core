package me.lrg.skyblock.core.repository;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.model.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * player_data テーブルを扱うRepository。
 *
 * このクラスの役割:
 * - player_data にプレイヤーが存在するか確認する
 * - player_data からプレイヤーデータを読み込む
 * - player_data に新規プレイヤーデータを作成する
 * - player_data にプレイヤーデータを保存する
 *
 * 注意:
 * - SQLはこのクラスだけに書く
 * - ゲームロジックは書かない
 * - BukkitのPlayer操作は書かない
 */
public class PlayerRepository {

    private static final String EXISTS_BY_UUID_SQL = """
            SELECT 1
            FROM player_data
            WHERE uuid = ?
            LIMIT 1
            """;

    private static final String LOAD_PLAYER_SQL = """
            SELECT uuid, name, coins
            FROM player_data
            WHERE uuid = ?
            LIMIT 1
            """;

    private static final String CREATE_PLAYER_SQL = """
            INSERT INTO player_data (uuid, name, coins)
            VALUES (?, ?, ?)
            """;

    private static final String SAVE_PLAYER_SQL = """
            UPDATE player_data
            SET name = ?, coins = ?
            WHERE uuid = ?
            """;

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public PlayerRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * 指定したUUIDのプレイヤーデータがDBに存在するか確認する。
     *
     * @param uuid プレイヤーUUID
     * @return 存在するならtrue
     */
    public boolean existsByUuid(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(EXISTS_BY_UUID_SQL)
        ) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_data の存在確認に失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to check player existence. uuid=" + uuid, exception);
        }
    }

    /**
     * DBからプレイヤーデータを読み込む。
     *
     * nullは返さず、存在しない場合は Optional.empty() を返す。
     *
     * @param uuid プレイヤーUUID
     * @param currentName 現在のプレイヤー名
     * @return プレイヤーデータ
     */
    public Optional<PlayerData> loadPlayer(UUID uuid, String currentName) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(currentName, "currentName");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(LOAD_PLAYER_SQL)
        ) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                long coins = resultSet.getLong("coins");

                PlayerData playerData = new PlayerData(
                        uuid,
                        currentName,
                        coins
                );

                return Optional.of(playerData);
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_data の読み込みに失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to load player data. uuid=" + uuid, exception);
        }
    }

    /**
     * 新規プレイヤーデータをDBに作成する。
     *
     * @param playerData 作成するプレイヤーデータ
     */
    public void createPlayer(PlayerData playerData) {
        Objects.requireNonNull(playerData, "playerData");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_PLAYER_SQL)
        ) {
            statement.setString(1, playerData.getUuid().toString());
            statement.setString(2, playerData.getName());
            statement.setLong(3, playerData.getCoins());

            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_data の作成に失敗しました。uuid=" + playerData.getUuid(), exception);
            throw new RepositoryException("Failed to create player data. uuid=" + playerData.getUuid(), exception);
        }
    }

    /**
     * プレイヤーデータをDBに保存する。
     *
     * @param playerData 保存するプレイヤーデータ
     */
    public void savePlayer(PlayerData playerData) {
        Objects.requireNonNull(playerData, "playerData");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_PLAYER_SQL)
        ) {
            statement.setString(1, playerData.getName());
            statement.setLong(2, playerData.getCoins());
            statement.setString(3, playerData.getUuid().toString());

            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_data の保存に失敗しました。uuid=" + playerData.getUuid(), exception);
            throw new RepositoryException("Failed to save player data. uuid=" + playerData.getUuid(), exception);
        }
    }
}