package me.lrg.skyblock.core.repository;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.model.StatsData;

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
 * player_stats テーブルを扱うRepository。
 *
 * このクラスの役割:
 * - ステータスデータの存在確認
 * - ステータスデータの読み込み
 * - ステータスデータの新規作成
 * - ステータスデータの保存
 *
 * 注意:
 * - SQLはこのクラスだけに書く
 * - ゲームロジックは書かない
 * - Bukkit APIは使わない
 */
public class StatsRepository {

    private static final String EXISTS_BY_UUID_SQL = """
            SELECT 1
            FROM player_stats
            WHERE uuid = ?
            LIMIT 1
            """;

    private static final String LOAD_STATS_SQL = """
            SELECT uuid, health, mana, strength, defense, speed
            FROM player_stats
            WHERE uuid = ?
            LIMIT 1
            """;

    private static final String CREATE_STATS_SQL = """
            INSERT INTO player_stats (uuid, health, mana, strength, defense, speed)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SAVE_STATS_SQL = """
            UPDATE player_stats
            SET health = ?, mana = ?, strength = ?, defense = ?, speed = ?
            WHERE uuid = ?
            """;

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public StatsRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

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
            logger.log(Level.SEVERE, "player_stats の存在確認に失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to check stats existence. uuid=" + uuid, exception);
        }
    }

    public Optional<StatsData> loadStats(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(LOAD_STATS_SQL)
        ) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                StatsData statsData = new StatsData(
                        uuid,
                        resultSet.getDouble("health"),
                        resultSet.getDouble("mana"),
                        resultSet.getDouble("strength"),
                        resultSet.getDouble("defense"),
                        resultSet.getDouble("speed")
                );

                return Optional.of(statsData);
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_stats の読み込みに失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to load stats data. uuid=" + uuid, exception);
        }
    }

    public void createStats(StatsData statsData) {
        Objects.requireNonNull(statsData, "statsData");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_STATS_SQL)
        ) {
            statement.setString(1, statsData.getUuid().toString());
            statement.setDouble(2, statsData.getHealth());
            statement.setDouble(3, statsData.getMana());
            statement.setDouble(4, statsData.getStrength());
            statement.setDouble(5, statsData.getDefense());
            statement.setDouble(6, statsData.getSpeed());

            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_stats の作成に失敗しました。uuid=" + statsData.getUuid(), exception);
            throw new RepositoryException("Failed to create stats data. uuid=" + statsData.getUuid(), exception);
        }
    }

    public void saveStats(StatsData statsData) {
        Objects.requireNonNull(statsData, "statsData");

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_STATS_SQL)
        ) {
            statement.setDouble(1, statsData.getHealth());
            statement.setDouble(2, statsData.getMana());
            statement.setDouble(3, statsData.getStrength());
            statement.setDouble(4, statsData.getDefense());
            statement.setDouble(5, statsData.getSpeed());
            statement.setString(6, statsData.getUuid().toString());

            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_stats の保存に失敗しました。uuid=" + statsData.getUuid(), exception);
            throw new RepositoryException("Failed to save stats data. uuid=" + statsData.getUuid(), exception);
        }
    }
}