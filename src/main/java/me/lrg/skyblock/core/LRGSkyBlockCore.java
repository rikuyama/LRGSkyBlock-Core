package me.lrg.skyblock.core;

import me.lrg.skyblock.core.config.PlayerDefaultSettings;
import me.lrg.skyblock.core.command.CoinCommand;
import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.listener.PlayerListener;
import me.lrg.skyblock.core.manager.CoinManager;
import me.lrg.skyblock.core.manager.PlayerManager;
import me.lrg.skyblock.core.repository.PlayerRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * LRG SkyBlock Core のメインクラス。
 */
public final class LRGSkyBlockCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private PlayerManager playerManager;
    private CoinManager coinManager;
    private PlayerDefaultSettings playerDefaultSettings;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            setupDatabase();
            setupConfigs();
            setupRepositories();
            setupManagers();
            registerListeners();
            registerCommands();

            getLogger().info("LRG SkyBlock Core を起動しました。");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "LRG SkyBlock Core の起動に失敗しました。", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        savePlayerDataOnShutdown();
        clearPlayerCache();
        closeDatabase();

        getLogger().info("LRG SkyBlock Core を停止しました。");
    }

    private void setupDatabase() {
        this.databaseManager = new DatabaseManager(this);
    }

    private void setupConfigs() {
        this.playerDefaultSettings = PlayerDefaultSettings.from(this);
    }

    private void setupRepositories() {
        this.playerRepository = new PlayerRepository(databaseManager, getLogger());
    }

    private void setupManagers() {
        this.playerManager = new PlayerManager(
                this,
                playerRepository,
                playerDefaultSettings
        );

        this.coinManager = new CoinManager(playerManager);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerListener(playerManager),
                this
        );
    }

    private void registerCommands() {
        if (getCommand("coins") == null) {
            getLogger().warning("plugin.yml に coins コマンドが登録されていません。");
            return;
        }

        getCommand("coins").setExecutor(new CoinCommand(coinManager));
    }

    private void savePlayerDataOnShutdown() {
        if (playerManager == null) {
            return;
        }

        playerManager.saveAllSynchronously();
    }

    private void clearPlayerCache() {
        if (playerManager == null) {
            return;
        }

        playerManager.clear();
    }

    private void closeDatabase() {
        if (databaseManager == null) {
            return;
        }

        databaseManager.close();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public CoinManager getCoinManager() {
        return coinManager;
    }
}