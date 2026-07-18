package me.lrg.skyblock.core.playerlevel.repository;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.playerlevel.model.PlayerLevelData;
import me.lrg.skyblock.core.repository.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerLevelRepository {

    private static final String LOAD_SQL = """
            SELECT level, current_xp, total_xp
            FROM player_levels
            WHERE uuid = ?
            LIMIT 1
            """;

    private static final String CREATE_SQL = """
            INSERT INTO player_levels (uuid, level, current_xp, total_xp)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SAVE_SQL = """
            INSERT INTO player_levels (uuid, level, current_xp, total_xp)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                level = VALUES(level),
                current_xp = VALUES(current_xp),
                total_xp = VALUES(total_xp)
            """;

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public PlayerLevelRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Optional<PlayerLevelData> load(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOAD_SQL)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlayerLevelData(
                        uuid,
                        resultSet.getInt("level"),
                        resultSet.getLong("current_xp"),
                        resultSet.getLong("total_xp")
                ));
            }
        } catch (SQLException | IllegalArgumentException exception) {
            logger.log(Level.SEVERE, "Player Levelの読み込みに失敗しました。uuid=" + uuid, exception);
            throw new RepositoryException("Failed to load player level. uuid=" + uuid, exception);
        }
    }

    public void create(PlayerLevelData data) {
        Objects.requireNonNull(data, "data");
        executeWrite(CREATE_SQL, data, "作成");
    }

    public void save(PlayerLevelData data) {
        Objects.requireNonNull(data, "data");
        executeWrite(SAVE_SQL, data, "保存");
    }

    private void executeWrite(String sql, PlayerLevelData data, String action) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setInt(2, data.getLevel());
            statement.setLong(3, data.getCurrentXp());
            statement.setLong(4, data.getTotalXp());
            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Player Levelの" + action + "に失敗しました。uuid=" + data.getUuid(), exception);
            throw new RepositoryException("Failed to " + action + " player level. uuid=" + data.getUuid(), exception);
        }
    }
}
