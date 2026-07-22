package me.lrg.skyblock.core.bazaar.order.repository;

import me.lrg.skyblock.core.bazaar.order.model.BazaarItemClaim;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrder;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderSide;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderStatus;
import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BazaarOrderRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public BazaarOrderRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public long create(UUID owner, String itemId, BazaarOrderSide side, long price, int amount) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(side, "side");
        if (itemId.isBlank() || itemId.length() > 64 || price <= 0L || amount <= 0) {
            throw new IllegalArgumentException("Invalid Bazaar order");
        }

        String sql = "INSERT INTO bazaar_orders(owner_uuid,item_id,side,price,original_amount,remaining_amount,status) VALUES(?,?,?,?,?,?,?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, owner.toString());
            statement.setString(2, itemId);
            statement.setString(3, side.name());
            statement.setLong(4, price);
            statement.setInt(5, amount);
            statement.setInt(6, amount);
            statement.setString(7, BazaarOrderStatus.OPEN.name());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Bazaar order insert affected unexpected rows");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new IllegalStateException("注文IDを取得できませんでした。");
        } catch (SQLException exception) {
            throw failure("Bazaar注文の作成に失敗しました。", exception);
        }
    }

    public Optional<BazaarOrder> find(long id) {
        if (id <= 0L) return Optional.empty();
        String sql = "SELECT * FROM bazaar_orders WHERE order_id=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Bazaar注文の取得に失敗しました。", exception);
        }
    }

    public List<BazaarOrder> findOpenByOwner(UUID owner) {
        Objects.requireNonNull(owner, "owner");
        return query("SELECT * FROM bazaar_orders WHERE owner_uuid=? AND status='OPEN' AND remaining_amount>0 ORDER BY created_at ASC,order_id ASC",
                statement -> statement.setString(1, owner.toString()));
    }

    public List<BazaarOrder> findOpenByItem(String itemId, BazaarOrderSide side) {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(side, "side");
        String order = side == BazaarOrderSide.BUY ? "price DESC" : "price ASC";
        return query("SELECT * FROM bazaar_orders WHERE item_id=? AND side=? AND status='OPEN' AND remaining_amount>0 ORDER BY "
                        + order + ",created_at ASC,order_id ASC",
                statement -> {
                    statement.setString(1, itemId);
                    statement.setString(2, side.name());
                });
    }

    public Optional<BazaarOrder> findBest(String itemId, BazaarOrderSide side) {
        List<BazaarOrder> orders = findOpenByItem(itemId, side);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.getFirst());
    }

    public void updateRemaining(long id, int remaining) {
        if (id <= 0L || remaining < 0) {
            throw new IllegalArgumentException("Invalid remaining update");
        }
        String status = remaining == 0 ? BazaarOrderStatus.FILLED.name() : BazaarOrderStatus.OPEN.name();
        int changed = execute("UPDATE bazaar_orders SET remaining_amount=?,status=? WHERE order_id=? AND status='OPEN'",
                statement -> {
                    statement.setInt(1, remaining);
                    statement.setString(2, status);
                    statement.setLong(3, id);
                });
        if (changed != 1) {
            throw new IllegalStateException("注文が既に更新またはキャンセルされています。orderId=" + id);
        }
    }

    public boolean cancel(long id, UUID owner) {
        Objects.requireNonNull(owner, "owner");
        if (id <= 0L) return false;
        return execute("UPDATE bazaar_orders SET status='CANCELLED' WHERE order_id=? AND owner_uuid=? AND status='OPEN' AND remaining_amount>0",
                statement -> {
                    statement.setLong(1, id);
                    statement.setString(2, owner.toString());
                }) == 1;
    }

    public long coinClaim(UUID owner) {
        Objects.requireNonNull(owner, "owner");
        String sql = "SELECT amount FROM bazaar_coin_claims WHERE owner_uuid=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Math.max(0L, rs.getLong(1)) : 0L;
            }
        } catch (SQLException exception) {
            throw failure("コイン受取額の取得に失敗しました。", exception);
        }
    }

    public void addCoinClaim(UUID owner, long amount) {
        Objects.requireNonNull(owner, "owner");
        if (amount <= 0L) return;
        addClaimSafely("bazaar_coin_claims", owner, null, amount);
    }

    public boolean clearCoinClaim(UUID owner, long expectedAmount) {
        Objects.requireNonNull(owner, "owner");
        if (expectedAmount <= 0L) return false;
        return execute("DELETE FROM bazaar_coin_claims WHERE owner_uuid=? AND amount=?",
                statement -> {
                    statement.setString(1, owner.toString());
                    statement.setLong(2, expectedAmount);
                }) == 1;
    }

    public List<BazaarItemClaim> itemClaims(UUID owner) {
        Objects.requireNonNull(owner, "owner");
        List<BazaarItemClaim> claims = new ArrayList<>();
        String sql = "SELECT item_id,amount FROM bazaar_item_claims WHERE owner_uuid=? AND amount>0 ORDER BY item_id";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) claims.add(new BazaarItemClaim(rs.getString(1), rs.getLong(2)));
            }
        } catch (SQLException exception) {
            throw failure("アイテム受取一覧の取得に失敗しました。", exception);
        }
        return claims;
    }

    public void addItemClaim(UUID owner, String itemId, long amount) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(itemId, "itemId");
        if (amount <= 0L) return;
        addClaimSafely("bazaar_item_claims", owner, itemId, amount);
    }

    public boolean decreaseItemClaim(UUID owner, String itemId, long amount) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(itemId, "itemId");
        if (amount <= 0L) return false;
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE bazaar_item_claims SET amount=amount-? WHERE owner_uuid=? AND item_id=? AND amount>=?")) {
                    update.setLong(1, amount);
                    update.setString(2, owner.toString());
                    update.setString(3, itemId);
                    update.setLong(4, amount);
                    if (update.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM bazaar_item_claims WHERE owner_uuid=? AND item_id=? AND amount=0")) {
                    delete.setString(1, owner.toString());
                    delete.setString(2, itemId);
                    delete.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("アイテム受取額の更新に失敗しました。", exception);
        }
    }

    private void addClaimSafely(String table, UUID owner, String itemId, long amount) {
        boolean itemClaim = itemId != null;
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String selectSql = itemClaim
                        ? "SELECT amount FROM bazaar_item_claims WHERE owner_uuid=? AND item_id=? FOR UPDATE"
                        : "SELECT amount FROM bazaar_coin_claims WHERE owner_uuid=? FOR UPDATE";
                Long current = null;
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setString(1, owner.toString());
                    if (itemClaim) select.setString(2, itemId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) current = rs.getLong(1);
                    }
                }

                if (current == null) {
                    String insertSql = itemClaim
                            ? "INSERT INTO bazaar_item_claims(owner_uuid,item_id,amount) VALUES(?,?,?)"
                            : "INSERT INTO bazaar_coin_claims(owner_uuid,amount) VALUES(?,?)";
                    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                        insert.setString(1, owner.toString());
                        if (itemClaim) {
                            insert.setString(2, itemId);
                            insert.setLong(3, amount);
                        } else {
                            insert.setLong(2, amount);
                        }
                        insert.executeUpdate();
                    }
                } else {
                    long updated = Math.addExact(current, amount);
                    String updateSql = itemClaim
                            ? "UPDATE bazaar_item_claims SET amount=? WHERE owner_uuid=? AND item_id=?"
                            : "UPDATE bazaar_coin_claims SET amount=? WHERE owner_uuid=?";
                    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                        update.setLong(1, updated);
                        update.setString(2, owner.toString());
                        if (itemClaim) update.setString(3, itemId);
                        if (update.executeUpdate() != 1) throw new SQLException("Claim update failed");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            }
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("Bazaar受取残高が上限を超えました。", exception);
        } catch (SQLException exception) {
            throw failure("Bazaar受取残高の更新に失敗しました。", exception);
        }
    }

    private List<BazaarOrder> query(String sql, SqlBinder binder) {
        List<BazaarOrder> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException exception) {
            throw failure("Bazaar注文一覧の取得に失敗しました。", exception);
        }
    }

    private int execute(String sql, SqlBinder binder) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Bazaar注文データの更新に失敗しました。", exception);
        }
    }

    private BazaarOrder map(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("created_at");
        Instant created = timestamp == null ? Instant.EPOCH : timestamp.toInstant();
        return new BazaarOrder(
                rs.getLong("order_id"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("item_id"),
                BazaarOrderSide.valueOf(rs.getString("side")),
                rs.getLong("price"),
                rs.getInt("original_amount"),
                rs.getInt("remaining_amount"),
                BazaarOrderStatus.valueOf(rs.getString("status")),
                created
        );
    }

    private IllegalStateException failure(String message, SQLException exception) {
        logger.log(Level.SEVERE, message, exception);
        return new IllegalStateException(message, exception);
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
