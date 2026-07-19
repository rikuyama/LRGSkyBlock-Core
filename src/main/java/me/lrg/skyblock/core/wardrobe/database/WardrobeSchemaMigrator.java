package me.lrg.skyblock.core.wardrobe.database;

import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WardrobeSchemaMigrator {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public WardrobeSchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void migrate() {
        String sql = """
                CREATE TABLE IF NOT EXISTS wardrobe_sets (
                    uuid VARCHAR(36) NOT NULL,
                    slot_index TINYINT NOT NULL,
                    helmet LONGTEXT NULL,
                    chestplate LONGTEXT NULL,
                    leggings LONGTEXT NULL,
                    boots LONGTEXT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid, slot_index)
                )
                """;
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Wardrobeテーブルを確認しました。");
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Wardrobeテーブルの作成に失敗しました。", exception);
            throw new IllegalStateException("Failed to migrate wardrobe schema", exception);
        }
    }
}
