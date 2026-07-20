package me.lrg.skyblock.core.bazaar.order.repository;

import me.lrg.skyblock.core.bazaar.order.model.BazaarItemClaim;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrder;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderSide;
import me.lrg.skyblock.core.bazaar.order.model.BazaarOrderStatus;
import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class BazaarOrderRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public BazaarOrderRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public long create(UUID owner, String itemId, BazaarOrderSide side, long price, int amount) {
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
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new IllegalStateException("注文IDを取得できませんでした。");
        } catch (SQLException exception) {
            throw new IllegalStateException("Bazaar注文の作成に失敗しました。", exception);
        }
    }

    public Optional<BazaarOrder> find(long id) {
        String sql = "SELECT * FROM bazaar_orders WHERE order_id=?";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Bazaar注文の取得に失敗しました。", exception);
        }
    }

    public List<BazaarOrder> findOpenByOwner(UUID owner) {
        return query("SELECT * FROM bazaar_orders WHERE owner_uuid=? AND status='OPEN' ORDER BY created_at ASC,order_id ASC", statement -> statement.setString(1, owner.toString()));
    }

    public List<BazaarOrder> findOpenByItem(String itemId, BazaarOrderSide side) {
        String order = side == BazaarOrderSide.BUY ? "price DESC" : "price ASC";
        return query("SELECT * FROM bazaar_orders WHERE item_id=? AND side=? AND status='OPEN' AND remaining_amount>0 ORDER BY " + order + ",created_at ASC,order_id ASC",
                statement -> { statement.setString(1, itemId); statement.setString(2, side.name()); });
    }

    public Optional<BazaarOrder> findBest(String itemId, BazaarOrderSide side) {
        List<BazaarOrder> orders = findOpenByItem(itemId, side);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.getFirst());
    }

    public void updateRemaining(long id, int remaining) {
        String status = remaining <= 0 ? BazaarOrderStatus.FILLED.name() : BazaarOrderStatus.OPEN.name();
        execute("UPDATE bazaar_orders SET remaining_amount=?,status=? WHERE order_id=?",
                statement -> { statement.setInt(1, Math.max(0, remaining)); statement.setString(2, status); statement.setLong(3, id); });
    }

    public boolean cancel(long id, UUID owner) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE bazaar_orders SET status='CANCELLED' WHERE order_id=? AND owner_uuid=? AND status='OPEN'")) {
            statement.setLong(1, id);
            statement.setString(2, owner.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Bazaar注文のキャンセルに失敗しました。", exception);
        }
    }

    public long coinClaim(UUID owner) {
        String sql = "SELECT amount FROM bazaar_coin_claims WHERE owner_uuid=?";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getLong(1) : 0L; }
        } catch (SQLException exception) {
            throw new IllegalStateException("コイン受取額の取得に失敗しました。", exception);
        }
    }

    public void addCoinClaim(UUID owner, long amount) {
        if (amount <= 0) return;
        execute("INSERT INTO bazaar_coin_claims(owner_uuid,amount) VALUES(?,?) ON DUPLICATE KEY UPDATE amount=amount+VALUES(amount)",
                statement -> { statement.setString(1, owner.toString()); statement.setLong(2, amount); });
    }

    public boolean clearCoinClaim(UUID owner, long expectedAmount) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM bazaar_coin_claims WHERE owner_uuid=? AND amount=?")) {
            statement.setString(1, owner.toString());
            statement.setLong(2, expectedAmount);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("コイン受取額の削除に失敗しました。", exception);
        }
    }

    public List<BazaarItemClaim> itemClaims(UUID owner) {
        List<BazaarItemClaim> claims = new ArrayList<>();
        String sql = "SELECT item_id,amount FROM bazaar_item_claims WHERE owner_uuid=? AND amount>0 ORDER BY item_id";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) claims.add(new BazaarItemClaim(rs.getString(1), rs.getLong(2)));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("アイテム受取一覧の取得に失敗しました。", exception);
        }
        return claims;
    }

    public void addItemClaim(UUID owner, String itemId, long amount) {
        if (amount <= 0) return;
        execute("INSERT INTO bazaar_item_claims(owner_uuid,item_id,amount) VALUES(?,?,?) ON DUPLICATE KEY UPDATE amount=amount+VALUES(amount)",
                statement -> { statement.setString(1, owner.toString()); statement.setString(2, itemId); statement.setLong(3, amount); });
    }

    public boolean decreaseItemClaim(UUID owner, String itemId, long amount) {
        if (amount <= 0) return false;
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE bazaar_item_claims SET amount=amount-? WHERE owner_uuid=? AND item_id=? AND amount>=?")) {
                update.setLong(1, amount); update.setString(2, owner.toString()); update.setString(3, itemId); update.setLong(4, amount);
                if (update.executeUpdate() == 0) { connection.rollback(); return false; }
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM bazaar_item_claims WHERE owner_uuid=? AND item_id=? AND amount<=0")) {
                delete.setString(1, owner.toString()); delete.setString(2, itemId); delete.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("アイテム受取額の更新に失敗しました。", exception);
        }
    }

    private List<BazaarOrder> query(String sql, SqlBinder binder) {
        List<BazaarOrder> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) { while (rs.next()) result.add(map(rs)); }
        } catch (SQLException exception) {
            logger.warning("Bazaar注文一覧の取得に失敗しました: " + exception.getMessage());
        }
        return result;
    }

    private void execute(String sql, SqlBinder binder) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement); statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Bazaar注文データの更新に失敗しました。", exception);
        }
    }

    private BazaarOrder map(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("created_at");
        Instant created = timestamp == null ? Instant.EPOCH : timestamp.toInstant();
        return new BazaarOrder(rs.getLong("order_id"), UUID.fromString(rs.getString("owner_uuid")), rs.getString("item_id"),
                BazaarOrderSide.valueOf(rs.getString("side")), rs.getLong("price"), rs.getInt("original_amount"),
                rs.getInt("remaining_amount"), BazaarOrderStatus.valueOf(rs.getString("status")), created);
    }

    @FunctionalInterface
    private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
}
