package me.lrg.skyblock.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * player_stats周辺のDB構造を安全に最新版へ更新するクラス。
 *
 * SQLはこのDB専用クラス内だけで実行する。
 */
public final class StatsSchemaMigrator {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    private static final String CREATE_SCHEMA_VERSION_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS schema_versions (
                schema_name VARCHAR(64) NOT NULL PRIMARY KEY,
                version INT NOT NULL,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_PLAYER_STATS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                health DOUBLE NOT NULL DEFAULT 100,
                mana DOUBLE NOT NULL DEFAULT 100,
                strength DOUBLE NOT NULL DEFAULT 0,
                defense DOUBLE NOT NULL DEFAULT 0,
                speed DOUBLE NOT NULL DEFAULT 100,
                critical_chance DOUBLE NOT NULL DEFAULT 30,
                magic_find DOUBLE NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_PLAYER_EXTRA_STATS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS player_extra_stats (
                uuid VARCHAR(36) NOT NULL,
                stat_key VARCHAR(64) NOT NULL,
                value DOUBLE NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, stat_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String COLUMN_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """;

    private static final String UPDATE_SCHEMA_VERSION_SQL = """
            INSERT INTO schema_versions (schema_name, version)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE version = VALUES(version)
            """;

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public StatsSchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void migrate() {
        try (Connection connection = databaseManager.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                createRequiredTables(connection);
                addMissingPlayerStatsColumns(connection);
                normalizePlayerStats(connection);
                normalizeExtraStats(connection);
                updateSchemaVersion(connection);
                connection.commit();

                logger.info("Stats DBスキーマをv" + CURRENT_SCHEMA_VERSION + "へ更新しました。");
            } catch (SQLException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Stats DBスキーマの更新に失敗しました。", exception);
            throw new IllegalStateException("Stats DBスキーマの更新に失敗しました。", exception);
        }
    }

    private void createRequiredTables(Connection connection) throws SQLException {
        executeStatement(connection, CREATE_SCHEMA_VERSION_TABLE_SQL);
        executeStatement(connection, CREATE_PLAYER_STATS_TABLE_SQL);
        executeStatement(connection, CREATE_PLAYER_EXTRA_STATS_TABLE_SQL);
    }

    private void addMissingPlayerStatsColumns(Connection connection) throws SQLException {
        Map<String, String> requiredColumns = new LinkedHashMap<>();
        requiredColumns.put("health", "DOUBLE NOT NULL DEFAULT 100");
        requiredColumns.put("mana", "DOUBLE NOT NULL DEFAULT 100");
        requiredColumns.put("strength", "DOUBLE NOT NULL DEFAULT 0");
        requiredColumns.put("defense", "DOUBLE NOT NULL DEFAULT 0");
        requiredColumns.put("speed", "DOUBLE NOT NULL DEFAULT 100");
        requiredColumns.put("critical_chance", "DOUBLE NOT NULL DEFAULT 30");
        requiredColumns.put("magic_find", "DOUBLE NOT NULL DEFAULT 0");

        for (Map.Entry<String, String> entry : requiredColumns.entrySet()) {
            addColumnIfMissing(connection, "player_stats", entry.getKey(), entry.getValue());
        }
    }

    private void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String definition
    ) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition;
        executeStatement(connection, sql);
        logger.info(tableName + "." + columnName + " を追加しました。");
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(COLUMN_EXISTS_SQL)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void normalizePlayerStats(Connection connection) throws SQLException {
        executeStatement(connection, "UPDATE player_stats SET health = 100 WHERE health IS NULL OR health < 1");
        executeStatement(connection, "UPDATE player_stats SET mana = 100 WHERE mana IS NULL OR mana < 0");
        executeStatement(connection, "UPDATE player_stats SET strength = 0 WHERE strength IS NULL OR strength < 0");
        executeStatement(connection, "UPDATE player_stats SET defense = 0 WHERE defense IS NULL OR defense < 0");
        executeStatement(connection, "UPDATE player_stats SET speed = 100 WHERE speed IS NULL OR speed < 0");
        executeStatement(connection, "UPDATE player_stats SET critical_chance = 30 WHERE critical_chance IS NULL");
        executeStatement(connection, "UPDATE player_stats SET critical_chance = 0 WHERE critical_chance < 0");
        executeStatement(connection, "UPDATE player_stats SET critical_chance = 100 WHERE critical_chance > 100");
        executeStatement(connection, "UPDATE player_stats SET magic_find = 0 WHERE magic_find IS NULL OR magic_find < 0");
    }

    private void normalizeExtraStats(Connection connection) throws SQLException {
        executeStatement(connection, "UPDATE player_extra_stats SET value = 0 WHERE value IS NULL OR value < 0");
        executeStatement(connection, "DELETE FROM player_extra_stats WHERE stat_key = 'intelligence'");
    }

    private void updateSchemaVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SCHEMA_VERSION_SQL)) {
            statement.setString(1, "stats");
            statement.setInt(2, CURRENT_SCHEMA_VERSION);
            statement.executeUpdate();
        }
    }

    private void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            logger.log(Level.SEVERE, "Stats DBスキーマ更新のロールバックに失敗しました。", rollbackException);
        }
    }
}
