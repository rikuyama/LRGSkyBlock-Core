package me.lrg.skyblock.core.economy.database;

import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EconomySchemaMigrator {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public EconomySchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void migrate() {
        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE player_data MODIFY COLUMN coins BIGINT NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bank_accounts (
                        owner_uuid VARCHAR(36) PRIMARY KEY,
                        balance BIGINT NOT NULL DEFAULT 0,
                        bank_level INT NOT NULL DEFAULT 1,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT chk_bank_balance_nonnegative CHECK (balance >= 0),
                        CONSTRAINT chk_bank_level_positive CHECK (bank_level >= 1)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS economy_transactions (
                        transaction_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        operation_id VARCHAR(96) NOT NULL UNIQUE,
                        source_uuid VARCHAR(36) NULL,
                        target_uuid VARCHAR(36) NULL,
                        amount BIGINT NOT NULL,
                        fee BIGINT NOT NULL DEFAULT 0,
                        transaction_type VARCHAR(32) NOT NULL,
                        reason VARCHAR(128) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_economy_source_created (source_uuid, created_at),
                        INDEX idx_economy_target_created (target_uuid, created_at)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS daily_transfer_usage (
                        owner_uuid VARCHAR(36) NOT NULL,
                        usage_date DATE NOT NULL,
                        amount BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (owner_uuid, usage_date)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS interest_runs (
                        run_date DATE PRIMARY KEY,
                        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        completed_at TIMESTAMP NULL,
                        affected_accounts INT NOT NULL DEFAULT 0,
                        total_interest BIGINT NOT NULL DEFAULT 0
                    )
                    """);
            logger.info("Economy/Bankテーブルのマイグレーションが完了しました。");
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Economy/Bankテーブルのマイグレーションに失敗しました。", exception);
            throw new IllegalStateException("Economy schema migration failed", exception);
        }
    }
}
