package me.lrg.skyblock.core;

import me.lrg.skyblock.core.command.CoinCommand;
import me.lrg.skyblock.core.config.PlayerDefaultSettings;
import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.listener.PlayerListener;
import me.lrg.skyblock.core.manager.CoinManager;
import me.lrg.skyblock.core.manager.PlayerManager;
import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.repository.PlayerRepository;
import me.lrg.skyblock.core.repository.StatsRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * LRG SkyBlock Core のメインクラス。
 */
public final class LRGSkyBlockCore extends JavaPlugin {

    private DatabaseManager databaseManager;

    private PlayerRepository playerRepository;
    private StatsRepository statsRepository;

    private PlayerManager playerManager;
    private CoinManager coinManager;
    private StatsManager statsManager;

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
        saveStatsDataOnShutdown();

        clearPlayerCache();
        clearStatsCache();

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
        this.statsRepository = new StatsRepository(databaseManager, getLogger());
    }

    private void setupManagers() {
        this.playerManager = new PlayerManager(
                this,
                playerRepository,
                playerDefaultSettings
        );

        this.coinManager = new CoinManager(playerManager);
        this.statsManager = new StatsManager(this, statsRepository);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerListener(playerManager, statsManager),
                this
        );
    }

    private void registerCommands() {
        PluginCommand coinsCommand = getCommand("coins");

        if (coinsCommand == null) {
            getLogger().warning("plugin.yml に coins コマンドが登録されていません。");
            return;
        }

        CoinCommand coinCommand = new CoinCommand(coinManager);

        coinsCommand.setExecutor(coinCommand);
        coinsCommand.setTabCompleter(coinCommand);
    }

    private void savePlayerDataOnShutdown() {
        if (playerManager == null) {
            return;
        }

        playerManager.saveAllSynchronously();
    }

    private void saveStatsDataOnShutdown() {
        if (statsManager == null) {
            return;
        }

        statsManager.saveAllSynchronously();
    }

    private void clearPlayerCache() {
        if (playerManager == null) {
            return;
        }

        playerManager.clear();
    }

    private void clearStatsCache() {
        if (statsManager == null) {
            return;
        }

        statsManager.clear();
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

    public StatsRepository getStatsRepository() {
        return statsRepository;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public CoinManager getCoinManager() {
        return coinManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }
}