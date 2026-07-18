package me.lrg.skyblock.core.playerlevel.database;

import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerLevelSchemaMigrator {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS player_levels (
                uuid VARCHAR(36) PRIMARY KEY,
                level INT NOT NULL DEFAULT 1,
                current_xp BIGINT NOT NULL DEFAULT 0,
                total_xp BIGINT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP
            )
            """;

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public PlayerLevelSchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void migrate() {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
            logger.info("player_levels テーブルを確認しました。");
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "player_levels テーブルの作成に失敗しました。", exception);
            throw new IllegalStateException("Failed to migrate player level schema.", exception);
        }
    }
}
