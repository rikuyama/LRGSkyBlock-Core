package me.lrg.skyblock.core.bazaar.database;

import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

public final class BazaarSchemaMigrator {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public BazaarSchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public void migrate() {
        List<String> statements = List.of(
                """
                CREATE TABLE IF NOT EXISTS bazaar_items (
                    item_id VARCHAR(96) PRIMARY KEY,
                    item_data LONGTEXT NOT NULL,
                    buy_price BIGINT NOT NULL,
                    sell_price BIGINT NOT NULL,
                    category VARCHAR(32) NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS bazaar_orders (
                    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    owner_uuid CHAR(36) NOT NULL,
                    item_id VARCHAR(96) NOT NULL,
                    side VARCHAR(8) NOT NULL,
                    price BIGINT NOT NULL,
                    original_amount INT NOT NULL,
                    remaining_amount INT NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_bazaar_match(item_id, side, status, price, created_at),
                    INDEX idx_bazaar_owner(owner_uuid, status),
                    CONSTRAINT fk_bazaar_order_item FOREIGN KEY(item_id) REFERENCES bazaar_items(item_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS bazaar_coin_claims (
                    owner_uuid CHAR(36) PRIMARY KEY,
                    amount BIGINT NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS bazaar_item_claims (
                    owner_uuid CHAR(36) NOT NULL,
                    item_id VARCHAR(96) NOT NULL,
                    amount BIGINT NOT NULL DEFAULT 0,
                    PRIMARY KEY(owner_uuid,item_id),
                    CONSTRAINT fk_bazaar_claim_item FOREIGN KEY(item_id) REFERENCES bazaar_items(item_id) ON DELETE CASCADE
                )
                """
        );
        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) statement.executeUpdate(sql);
            logger.info("Bazaarテーブルを確認しました。");
        } catch (Exception exception) {
            throw new IllegalStateException("Bazaarスキーマ更新に失敗しました。", exception);
        }
    }
}
