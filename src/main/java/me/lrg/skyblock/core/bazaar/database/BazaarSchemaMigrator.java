package me.lrg.skyblock.core.bazaar.database;

import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

public final class BazaarSchemaMigrator {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public BazaarSchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public void migrate() {
        String sql = """
                CREATE TABLE IF NOT EXISTS bazaar_items (
                    item_id VARCHAR(96) PRIMARY KEY,
                    item_data LONGTEXT NOT NULL,
                    buy_price BIGINT NOT NULL,
                    sell_price BIGINT NOT NULL,
                    category VARCHAR(32) NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("bazaar_itemsテーブルを確認しました。");
        } catch (Exception exception) {
            throw new IllegalStateException("Bazaarスキーマ更新に失敗しました。", exception);
        }
    }
}
