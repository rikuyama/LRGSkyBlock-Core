package me.lrg.skyblock.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL接続を管理するクラス。
 *
 * このクラスの役割:
 * - config.yml からDB設定を読む
 * - HikariCPを初期化する
 * - ConnectionをRepositoryへ渡す
 * - プラグイン停止時に接続プールを閉じる
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();

        setupDataSource();
    }

    private void setupDataSource() {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.name", "lrgskyblock");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "peka2305");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Tokyo"
                + "&characterEncoding=utf8";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("LRGSkyBlock-HikariPool");

        this.dataSource = new HikariDataSource(config);

        logger.info("MySQL接続プールを初期化しました。database=" + database);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariDataSource is not initialized.");
        }

        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource == null) {
            return;
        }

        if (!dataSource.isClosed()) {
            dataSource.close();
            logger.info("MySQL接続プールを終了しました。");
        }}

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "MySQL接続テストに失敗しました。", exception);
            return false;
        }
    }
}