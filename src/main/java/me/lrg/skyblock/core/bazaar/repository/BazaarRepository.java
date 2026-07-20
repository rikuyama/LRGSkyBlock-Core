package me.lrg.skyblock.core.bazaar.repository;

import me.lrg.skyblock.core.bazaar.model.BazaarItem;
import me.lrg.skyblock.core.bazaar.util.BazaarItemCodec;
import me.lrg.skyblock.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class BazaarRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public BazaarRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public List<BazaarItem> loadAll() {
        List<BazaarItem> result = new ArrayList<>();
        String sql = "SELECT item_id,item_data,buy_price,sell_price,category,enabled FROM bazaar_items";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new BazaarItem(rs.getString("item_id"), BazaarItemCodec.decode(rs.getString("item_data")), rs.getLong("buy_price"), rs.getLong("sell_price"), rs.getString("category"), rs.getBoolean("enabled")));
            }
        } catch (Exception exception) {
            logger.warning("Bazaar一覧の読み込みに失敗しました: " + exception.getMessage());
        }
        return result;
    }

    public void save(BazaarItem item) {
        String sql = """
                INSERT INTO bazaar_items(item_id,item_data,buy_price,sell_price,category,enabled)
                VALUES(?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE item_data=VALUES(item_data),buy_price=VALUES(buy_price),sell_price=VALUES(sell_price),category=VALUES(category),enabled=VALUES(enabled)
                """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.id());
            statement.setString(2, BazaarItemCodec.encode(item.template()));
            statement.setLong(3, item.buyPrice());
            statement.setLong(4, item.sellPrice());
            statement.setString(5, item.category());
            statement.setBoolean(6, item.enabled());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("Bazaarアイテムの保存に失敗しました。", exception);
        }
    }

    public void delete(String id) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM bazaar_items WHERE item_id=?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("Bazaarアイテムの削除に失敗しました。", exception);
        }
    }
}
