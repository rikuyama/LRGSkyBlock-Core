package me.lrg.skyblock.core.economy.repository;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.economy.model.BankAccount;
import me.lrg.skyblock.core.economy.model.EconomyFailure;
import me.lrg.skyblock.core.economy.model.EconomyResult;
import me.lrg.skyblock.core.economy.model.EconomyTransaction;
import me.lrg.skyblock.core.economy.model.InterestRunResult;
import me.lrg.skyblock.core.economy.model.PlayerIdentity;
import me.lrg.skyblock.core.economy.service.InterestCalculator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EconomyRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public EconomyRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public OptionalLongValue walletBalance(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT coins FROM player_data WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? OptionalLongValue.of(resultSet.getLong(1)) : OptionalLongValue.empty();
            }
        } catch (SQLException exception) {
            throw repositoryFailure("ウォレット残高の取得に失敗しました。uuid=" + uuid, exception);
        }
    }

    public Optional<PlayerIdentity> findPlayerByName(String name) {
        Objects.requireNonNull(name, "name");
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT uuid,name FROM player_data WHERE LOWER(name)=LOWER(?) LIMIT 1")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return Optional.empty();
                return Optional.of(new PlayerIdentity(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name")));
            }
        } catch (SQLException exception) {
            throw repositoryFailure("プレイヤー検索に失敗しました。name=" + name, exception);
        }
    }

    public BankAccount bankAccount(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureBankAccount(connection, uuid);
                BankAccount account = lockBankAccount(connection, uuid);
                connection.commit();
                return account;
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw repositoryFailure("銀行口座の取得に失敗しました。uuid=" + uuid, exception);
        }
    }

    public EconomyResult adjustWallet(UUID uuid, long delta, String type, String reason, String operationId) {
        Objects.requireNonNull(uuid, "uuid");
        return inTransaction(connection -> {
            if (!claimOperation(connection, operationId)) return EconomyResult.failure(EconomyFailure.DUPLICATE_OPERATION);
            Long wallet = lockWallet(connection, uuid);
            if (wallet == null) return EconomyResult.failure(EconomyFailure.PLAYER_NOT_FOUND);
            long updated;
            try {
                updated = Math.addExact(wallet, delta);
            } catch (ArithmeticException exception) {
                return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
            }
            if (updated < 0L) return EconomyResult.failure(EconomyFailure.INSUFFICIENT_WALLET);
            updateWallet(connection, uuid, updated);
            completeOperation(connection, operationId, delta < 0 ? uuid : null, delta > 0 ? uuid : null,
                    Math.abs(delta), 0L, type, reason);
            long bank = lockOrCreateBank(connection, uuid).balance();
            return EconomyResult.success(Math.abs(delta), 0L, updated, bank);
        });
    }

    public EconomyResult depositToBank(UUID uuid, long amount, long capacity, String operationId) {
        Objects.requireNonNull(uuid, "uuid");
        return inTransaction(connection -> {
            if (!claimOperation(connection, operationId)) return EconomyResult.failure(EconomyFailure.DUPLICATE_OPERATION);
            Long wallet = lockWallet(connection, uuid);
            if (wallet == null) return EconomyResult.failure(EconomyFailure.PLAYER_NOT_FOUND);
            BankAccount account = lockOrCreateBank(connection, uuid);
            if (wallet < amount) return EconomyResult.failure(EconomyFailure.INSUFFICIENT_WALLET);
            if (amount > capacity - account.balance()) return EconomyResult.failure(EconomyFailure.BANK_CAPACITY);
            long newWallet = wallet - amount;
            long newBank = Math.addExact(account.balance(), amount);
            updateWallet(connection, uuid, newWallet);
            updateBankBalance(connection, uuid, newBank);
            completeOperation(connection, operationId, uuid, uuid, amount, 0L, "BANK_DEPOSIT", "bank_deposit");
            return EconomyResult.success(amount, 0L, newWallet, newBank);
        });
    }

    public EconomyResult withdrawFromBank(UUID uuid, long amount, String operationId) {
        Objects.requireNonNull(uuid, "uuid");
        return inTransaction(connection -> {
            if (!claimOperation(connection, operationId)) return EconomyResult.failure(EconomyFailure.DUPLICATE_OPERATION);
            Long wallet = lockWallet(connection, uuid);
            if (wallet == null) return EconomyResult.failure(EconomyFailure.PLAYER_NOT_FOUND);
            BankAccount account = lockOrCreateBank(connection, uuid);
            if (account.balance() < amount) return EconomyResult.failure(EconomyFailure.INSUFFICIENT_BANK);
            long newWallet;
            try {
                newWallet = Math.addExact(wallet, amount);
            } catch (ArithmeticException exception) {
                return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
            }
            long newBank = account.balance() - amount;
            updateWallet(connection, uuid, newWallet);
            updateBankBalance(connection, uuid, newBank);
            completeOperation(connection, operationId, uuid, uuid, amount, 0L, "BANK_WITHDRAW", "bank_withdraw");
            return EconomyResult.success(amount, 0L, newWallet, newBank);
        });
    }

    public EconomyResult transfer(
            UUID source,
            UUID target,
            long amount,
            long fee,
            long dailyLimit,
            LocalDate usageDate,
            String operationId
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        return inTransaction(connection -> {
            if (!claimOperation(connection, operationId)) return EconomyResult.failure(EconomyFailure.DUPLICATE_OPERATION);
            UUID first = source.toString().compareTo(target.toString()) <= 0 ? source : target;
            UUID second = first.equals(source) ? target : source;
            Long firstWallet = lockWallet(connection, first);
            Long secondWallet = lockWallet(connection, second);
            if (firstWallet == null || secondWallet == null) return EconomyResult.failure(EconomyFailure.PLAYER_NOT_FOUND);
            long sourceWallet = first.equals(source) ? firstWallet : secondWallet;
            long targetWallet = first.equals(target) ? firstWallet : secondWallet;
            long totalDebit;
            long newTarget;
            try {
                totalDebit = Math.addExact(amount, fee);
                newTarget = Math.addExact(targetWallet, amount);
            } catch (ArithmeticException exception) {
                return EconomyResult.failure(EconomyFailure.INVALID_AMOUNT);
            }
            if (sourceWallet < totalDebit) return EconomyResult.failure(EconomyFailure.INSUFFICIENT_WALLET);
            long used = lockTransferUsage(connection, source, usageDate);
            if (dailyLimit > 0L && (amount > dailyLimit || used > dailyLimit - amount)) {
                return EconomyResult.failure(EconomyFailure.DAILY_LIMIT);
            }
            updateWallet(connection, source, sourceWallet - totalDebit);
            updateWallet(connection, target, newTarget);
            updateTransferUsage(connection, source, usageDate, used + amount);
            completeOperation(connection, operationId, source, target, amount, fee, "PLAYER_TRANSFER", "player_pay");
            BankAccount sourceBank = lockOrCreateBank(connection, source);
            return EconomyResult.success(amount, fee, sourceWallet - totalDebit, sourceBank.balance());
        });
    }

    public EconomyResult upgradeBank(UUID uuid, int currentLevel, int nextLevel, long cost, String operationId) {
        Objects.requireNonNull(uuid, "uuid");
        return inTransaction(connection -> {
            if (!claimOperation(connection, operationId)) return EconomyResult.failure(EconomyFailure.DUPLICATE_OPERATION);
            Long wallet = lockWallet(connection, uuid);
            if (wallet == null) return EconomyResult.failure(EconomyFailure.PLAYER_NOT_FOUND);
            BankAccount account = lockOrCreateBank(connection, uuid);
            if (account.level() != currentLevel || nextLevel <= currentLevel) {
                return EconomyResult.failure(EconomyFailure.MAX_BANK_LEVEL);
            }
            if (wallet < cost) return EconomyResult.failure(EconomyFailure.INSUFFICIENT_WALLET);
            long newWallet = wallet - cost;
            updateWallet(connection, uuid, newWallet);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE bank_accounts SET bank_level=? WHERE owner_uuid=? AND bank_level=?")) {
                statement.setInt(1, nextLevel);
                statement.setString(2, uuid.toString());
                statement.setInt(3, currentLevel);
                if (statement.executeUpdate() != 1) return EconomyResult.failure(EconomyFailure.DATABASE_ERROR);
            }
            completeOperation(connection, operationId, uuid, null, cost, 0L, "BANK_UPGRADE", "bank_upgrade");
            return EconomyResult.success(cost, 0L, newWallet, account.balance());
        });
    }

    public List<EconomyTransaction> history(UUID uuid, int limit) {
        Objects.requireNonNull(uuid, "uuid");
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = """
                SELECT transaction_id,operation_id,source_uuid,target_uuid,amount,fee,transaction_type,reason,created_at
                FROM economy_transactions
                WHERE source_uuid=? OR target_uuid=?
                ORDER BY transaction_id DESC
                LIMIT ?
                """;
        List<EconomyTransaction> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, uuid.toString());
            statement.setInt(3, safeLimit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new EconomyTransaction(
                            rs.getLong("transaction_id"),
                            rs.getString("operation_id"),
                            nullableUuid(rs.getString("source_uuid")),
                            nullableUuid(rs.getString("target_uuid")),
                            rs.getLong("amount"),
                            rs.getLong("fee"),
                            rs.getString("transaction_type"),
                            rs.getString("reason"),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                }
            }
            return result;
        } catch (SQLException exception) {
            throw repositoryFailure("取引履歴の取得に失敗しました。uuid=" + uuid, exception);
        }
    }

    public InterestRunResult applyDailyInterest(
            LocalDate date,
            double rate,
            IntToLongFunction capacityForLevel
    ) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(capacityForLevel, "capacityForLevel");
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement claim = connection.prepareStatement(
                        "INSERT INTO interest_runs(run_date) VALUES(?)")) {
                    claim.setDate(1, Date.valueOf(date));
                    claim.executeUpdate();
                } catch (SQLIntegrityConstraintViolationException duplicate) {
                    rollbackQuietly(connection);
                    return new InterestRunResult(false, date, 0, 0L);
                }

                int affected = 0;
                long total = 0L;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT owner_uuid,balance,bank_level FROM bank_accounts ORDER BY owner_uuid FOR UPDATE");
                     ResultSet rs = select.executeQuery();
                     PreparedStatement update = connection.prepareStatement(
                             "UPDATE bank_accounts SET balance=? WHERE owner_uuid=?");
                     PreparedStatement transaction = connection.prepareStatement("""
                             INSERT INTO economy_transactions(operation_id,source_uuid,target_uuid,amount,fee,transaction_type,reason)
                             VALUES(?,NULL,?,?,0,'BANK_INTEREST','daily_interest')
                             """)) {
                    while (rs.next()) {
                        UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                        long balance = rs.getLong("balance");
                        int level = rs.getInt("bank_level");
                        long interest = InterestCalculator.calculateInterest(balance, rate, capacityForLevel.applyAsLong(level));
                        if (interest <= 0L) continue;
                        long updated = Math.addExact(balance, interest);
                        update.setLong(1, updated);
                        update.setString(2, owner.toString());
                        update.addBatch();
                        transaction.setString(1, "interest:" + date + ":" + owner);
                        transaction.setString(2, owner.toString());
                        transaction.setLong(3, interest);
                        transaction.addBatch();
                        affected++;
                        total = Math.addExact(total, interest);
                    }
                    update.executeBatch();
                    transaction.executeBatch();
                }
                try (PreparedStatement complete = connection.prepareStatement(
                        "UPDATE interest_runs SET completed_at=CURRENT_TIMESTAMP,affected_accounts=?,total_interest=? WHERE run_date=?")) {
                    complete.setInt(1, affected);
                    complete.setLong(2, total);
                    complete.setDate(3, Date.valueOf(date));
                    complete.executeUpdate();
                }
                connection.commit();
                return new InterestRunResult(true, date, affected, total);
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw repositoryFailure("銀行利息処理に失敗しました。date=" + date, exception);
        }
    }

    public Optional<LocalDate> latestInterestRunDate() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT MAX(run_date) FROM interest_runs WHERE completed_at IS NOT NULL");
             ResultSet rs = statement.executeQuery()) {
            if (!rs.next()) return Optional.empty();
            Date value = rs.getDate(1);
            return value == null ? Optional.empty() : Optional.of(value.toLocalDate());
        } catch (SQLException exception) {
            throw repositoryFailure("最終利息実行日の取得に失敗しました。", exception);
        }
    }

    private EconomyResult inTransaction(TransactionWork work) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                EconomyResult result = work.execute(connection);
                if (!result.success()) {
                    rollbackQuietly(connection);
                    return result;
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                logger.log(Level.SEVERE, "Economyトランザクションに失敗しました。", exception);
                return EconomyResult.failure(EconomyFailure.DATABASE_ERROR);
            }
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Economy DB接続に失敗しました。", exception);
            return EconomyResult.failure(EconomyFailure.DATABASE_ERROR);
        }
    }

    /** Reserve an operation id inside the transaction. It is removed on rollback. */
    private boolean claimOperation(Connection connection, String operationId) throws SQLException {
        if (operationId == null || operationId.isBlank() || operationId.length() > 96) return false;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions(operation_id,amount,fee,transaction_type,reason)
                VALUES(?,0,0,'PENDING','pending')
                """)) {
            statement.setString(1, operationId);
            statement.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            return false;
        }
    }

    private void completeOperation(
            Connection connection,
            String operationId,
            UUID source,
            UUID target,
            long amount,
            long fee,
            String type,
            String reason
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE economy_transactions
                SET source_uuid=?,target_uuid=?,amount=?,fee=?,transaction_type=?,reason=?
                WHERE operation_id=? AND transaction_type='PENDING'
                """)) {
            setNullableUuid(statement, 1, source);
            setNullableUuid(statement, 2, target);
            statement.setLong(3, amount);
            statement.setLong(4, fee);
            statement.setString(5, type);
            statement.setString(6, reason == null ? "unspecified" : reason.substring(0, Math.min(128, reason.length())));
            statement.setString(7, operationId);
            if (statement.executeUpdate() != 1) throw new SQLException("operation completion failed: " + operationId);
        }
    }

    private Long lockWallet(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT coins FROM player_data WHERE uuid=? FOR UPDATE")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private void updateWallet(Connection connection, UUID uuid, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE player_data SET coins=? WHERE uuid=?")) {
            statement.setLong(1, balance);
            statement.setString(2, uuid.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("wallet update failed: " + uuid);
        }
    }

    private BankAccount lockOrCreateBank(Connection connection, UUID uuid) throws SQLException {
        ensureBankAccount(connection, uuid);
        return lockBankAccount(connection, uuid);
    }

    private void ensureBankAccount(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO bank_accounts(owner_uuid,balance,bank_level) VALUES(?,0,1)")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    private BankAccount lockBankAccount(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance,bank_level FROM bank_accounts WHERE owner_uuid=? FOR UPDATE")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("bank account missing: " + uuid);
                return new BankAccount(uuid, rs.getLong("balance"), rs.getInt("bank_level"));
            }
        }
    }

    private void updateBankBalance(Connection connection, UUID uuid, long balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bank_accounts SET balance=? WHERE owner_uuid=?")) {
            statement.setLong(1, balance);
            statement.setString(2, uuid.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("bank update failed: " + uuid);
        }
    }

    private long lockTransferUsage(Connection connection, UUID uuid, LocalDate date) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT IGNORE INTO daily_transfer_usage(owner_uuid,usage_date,amount) VALUES(?,?,0)")) {
            insert.setString(1, uuid.toString());
            insert.setDate(2, Date.valueOf(date));
            insert.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT amount FROM daily_transfer_usage WHERE owner_uuid=? AND usage_date=? FOR UPDATE")) {
            select.setString(1, uuid.toString());
            select.setDate(2, Date.valueOf(date));
            try (ResultSet rs = select.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private void updateTransferUsage(Connection connection, UUID uuid, LocalDate date, long amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE daily_transfer_usage SET amount=? WHERE owner_uuid=? AND usage_date=?")) {
            statement.setLong(1, amount);
            statement.setString(2, uuid.toString());
            statement.setDate(3, Date.valueOf(date));
            statement.executeUpdate();
        }
    }

    private RuntimeException repositoryFailure(String message, SQLException exception) {
        logger.log(Level.SEVERE, message, exception);
        return new IllegalStateException(message, exception);
    }

    private static UUID nullableUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private static void setNullableUuid(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) statement.setNull(index, java.sql.Types.VARCHAR);
        else statement.setString(index, uuid.toString());
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    @FunctionalInterface
    private interface TransactionWork {
        EconomyResult execute(Connection connection) throws SQLException;
    }

    public record OptionalLongValue(boolean present, long value) {
        public static OptionalLongValue of(long value) { return new OptionalLongValue(true, value); }
        public static OptionalLongValue empty() { return new OptionalLongValue(false, 0L); }
    }
}
