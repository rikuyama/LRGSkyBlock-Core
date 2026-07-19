package me.lrg.skyblock.core.wardrobe.repository;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.repository.RepositoryException;
import me.lrg.skyblock.core.wardrobe.model.WardrobeSet;
import me.lrg.skyblock.core.wardrobe.util.WardrobeItemCodec;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WardrobeRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public WardrobeRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Map<Integer, WardrobeSet> load(UUID uuid) {
        String sql = "SELECT slot_index, helmet, chestplate, leggings, boots FROM wardrobe_sets WHERE uuid = ?";
        Map<Integer, WardrobeSet> result = new HashMap<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    int slot = rows.getInt("slot_index");
                    ItemStack[] armor = new ItemStack[] {
                            WardrobeItemCodec.decode(rows.getString("boots")),
                            WardrobeItemCodec.decode(rows.getString("leggings")),
                            WardrobeItemCodec.decode(rows.getString("chestplate")),
                            WardrobeItemCodec.decode(rows.getString("helmet"))
                    };
                    result.put(slot, new WardrobeSet(armor));
                }
            }
            return result;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Wardrobeの読み込みに失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to load wardrobe", exception);
        }
    }

    public void save(UUID uuid, int slot, WardrobeSet set) {
        if (set.isEmpty()) {
            delete(uuid, slot);
            return;
        }
        ItemStack[] armor = set.getArmor();
        String sql = """
                INSERT INTO wardrobe_sets (uuid, slot_index, helmet, chestplate, leggings, boots)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    helmet = VALUES(helmet),
                    chestplate = VALUES(chestplate),
                    leggings = VALUES(leggings),
                    boots = VALUES(boots)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, slot);
            statement.setString(3, WardrobeItemCodec.encode(armor[3]));
            statement.setString(4, WardrobeItemCodec.encode(armor[2]));
            statement.setString(5, WardrobeItemCodec.encode(armor[1]));
            statement.setString(6, WardrobeItemCodec.encode(armor[0]));
            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Wardrobeの保存に失敗しました。uuid=" + uuid + ", slot=" + slot, exception);
            throw new RepositoryException("Failed to save wardrobe", exception);
        }
    }
    private void delete(UUID uuid, int slot) {
        String sql = "DELETE FROM wardrobe_sets WHERE uuid = ? AND slot_index = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, slot);
            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Wardrobeの削除に失敗しました。uuid=" + uuid + ", slot=" + slot, exception);
            throw new RepositoryException("Failed to delete wardrobe", exception);
        }
    }
}
