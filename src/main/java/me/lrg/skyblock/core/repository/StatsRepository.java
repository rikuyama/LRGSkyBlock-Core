package me.lrg.skyblock.core.repository;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.model.StatsData;
import me.lrg.skyblock.core.model.StatsType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * player_stats / player_extra_stats テーブルを扱うRepository。
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
            SELECT uuid, health, mana, strength, defense, speed, critical_chance, magic_find
            FROM player_stats
            WHERE uuid = ?
            LIMIT 1
            """;

    private static final String CREATE_STATS_SQL = """
            INSERT INTO player_stats (
                uuid,
                health,
                mana,
                strength,
                defense,
                speed,
                critical_chance,
                magic_find
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SAVE_STATS_SQL = """
            UPDATE player_stats
            SET health = ?,
                mana = ?,
                strength = ?,
                defense = ?,
                speed = ?,
                critical_chance = ?,
                magic_find = ?
            WHERE uuid = ?
            """;

    private static final String LOAD_EXTRA_STATS_SQL = """
            SELECT stat_key, value
            FROM player_extra_stats
            WHERE uuid = ?
            """;

    private static final String DELETE_EXTRA_STATS_SQL = """
            DELETE FROM player_extra_stats
            WHERE uuid = ?
            """;

    private static final String INSERT_EXTRA_STATS_SQL = """
            INSERT INTO player_extra_stats (uuid, stat_key, value)
            VALUES (?, ?, ?)
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

        try (Connection connection = databaseManager.getConnection()) {
            StatsData statsData = loadBaseStats(connection, uuid);

            if (statsData == null) {
                return Optional.empty();
            }

            loadExtraStats(connection, statsData);

            return Optional.of(statsData);
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_stats の読み込みに失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to load stats data. uuid=" + uuid, exception);
        }
    }

    public void createStats(StatsData statsData) {
        Objects.requireNonNull(statsData, "statsData");

        try (Connection connection = databaseManager.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                createBaseStats(connection, statsData);
                saveExtraStats(connection, statsData);

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_stats の作成に失敗しました。uuid=" + statsData.getUuid(), exception);
            throw new RepositoryException("Failed to create stats data. uuid=" + statsData.getUuid(), exception);
        }
    }

    public void saveStats(StatsData statsData) {
        Objects.requireNonNull(statsData, "statsData");

        try (Connection connection = databaseManager.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                saveBaseStats(connection, statsData);
                deleteExtraStats(connection, statsData.getUuid());
                saveExtraStats(connection, statsData);

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_stats の保存に失敗しました。uuid=" + statsData.getUuid(), exception);
            throw new RepositoryException("Failed to save stats data. uuid=" + statsData.getUuid(), exception);
        }
    }

    private StatsData loadBaseStats(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LOAD_STATS_SQL)) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new StatsData(
                        uuid,
                        resultSet.getDouble("health"),
                        resultSet.getDouble("mana"),
                        resultSet.getDouble("strength"),
                        resultSet.getDouble("defense"),
                        resultSet.getDouble("speed"),
                        resultSet.getDouble("critical_chance"),
                        resultSet.getDouble("magic_find")
                );
            }
        }
    }

    private void loadExtraStats(Connection connection, StatsData statsData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LOAD_EXTRA_STATS_SQL)) {
            statement.setString(1, statsData.getUuid().toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String statKey = resultSet.getString("stat_key");
                    double value = resultSet.getDouble("value");

                    StatsType.fromKey(statKey)
                            .ifPresent(statsType -> statsData.setExtraStat(statsType, value));
                }
            }
        }
    }

    private void createBaseStats(Connection connection, StatsData statsData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_STATS_SQL)) {
            statement.setString(1, statsData.getUuid().toString());
            statement.setDouble(2, statsData.getHealth());
            statement.setDouble(3, statsData.getMana());
            statement.setDouble(4, statsData.getStrength());
            statement.setDouble(5, statsData.getDefense());
            statement.setDouble(6, statsData.getSpeed());
            statement.setDouble(7, statsData.getCriticalChance());
            statement.setDouble(8, statsData.getMagicFind());

            statement.executeUpdate();
        }
    }

    private void saveBaseStats(Connection connection, StatsData statsData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SAVE_STATS_SQL)) {
            statement.setDouble(1, statsData.getHealth());
            statement.setDouble(2, statsData.getMana());
            statement.setDouble(3, statsData.getStrength());
            statement.setDouble(4, statsData.getDefense());
            statement.setDouble(5, statsData.getSpeed());
            statement.setDouble(6, statsData.getCriticalChance());
            statement.setDouble(7, statsData.getMagicFind());
            statement.setString(8, statsData.getUuid().toString());

            statement.executeUpdate();
        }
    }

    private void deleteExtraStats(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_EXTRA_STATS_SQL)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    private void saveExtraStats(Connection connection, StatsData statsData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_EXTRA_STATS_SQL)) {
            for (Map.Entry<StatsType, Double> entry : statsData.getExtraStats().entrySet()) {
                statement.setString(1, statsData.getUuid().toString());
                statement.setString(2, entry.getKey().getKey());
                statement.setDouble(3, entry.getValue());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }
}