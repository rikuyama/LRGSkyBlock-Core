package me.lrg.skyblock.core;

import me.lrg.skyblock.core.command.CoinCommand;
import me.lrg.skyblock.core.command.StatsCommand;
import me.lrg.skyblock.core.command.StatsCacheCommand;
import me.lrg.skyblock.core.command.StatsDebugCommand;
import me.lrg.skyblock.core.config.FortuneTargetSettings;
import me.lrg.skyblock.core.config.PlayerDefaultSettings;
import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.database.StatsSchemaMigrator;
import me.lrg.skyblock.core.listener.FarmingFortuneListener;
import me.lrg.skyblock.core.listener.ForagingFortuneListener;
import me.lrg.skyblock.core.listener.HealthListener;
import me.lrg.skyblock.core.listener.MiningFortuneListener;
import me.lrg.skyblock.core.listener.ManaListener;
import me.lrg.skyblock.core.listener.PlayerCombatListener;
import me.lrg.skyblock.core.listener.PlayerListener;
import me.lrg.skyblock.core.manager.CoinManager;
import me.lrg.skyblock.core.manager.FortuneManager;
import me.lrg.skyblock.core.manager.PlayerManager;
import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.repository.PlayerRepository;
import me.lrg.skyblock.core.repository.StatsRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class LRGSkyBlockCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private StatsRepository statsRepository;
    private PlayerManager playerManager;
    private CoinManager coinManager;
    private StatsManager statsManager;
    private FortuneManager fortuneManager;
    private PlayerDefaultSettings playerDefaultSettings;
    private FortuneTargetSettings fortuneTargetSettings;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            setupDatabase();
            migrateDatabaseSchema();
            setupConfigs();
            setupRepositories();
            setupManagers();
            registerListeners();
            registerCommands();
            startTasks();

            getLogger().info("LRG SkyBlock Core を起動しました。");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "LRG SkyBlock Core の起動に失敗しました。", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        stopTasks();
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

    private void migrateDatabaseSchema() {
        StatsSchemaMigrator statsSchemaMigrator = new StatsSchemaMigrator(databaseManager, getLogger());
        statsSchemaMigrator.migrate();
    }

    private void setupConfigs() {
        this.playerDefaultSettings = PlayerDefaultSettings.from(this);
        this.fortuneTargetSettings = FortuneTargetSettings.load(this);
    }

    private void setupRepositories() {
        this.playerRepository = new PlayerRepository(databaseManager, getLogger());
        this.statsRepository = new StatsRepository(databaseManager, getLogger());
    }

    private void setupManagers() {
        this.playerManager = new PlayerManager(this, playerRepository, playerDefaultSettings);
        this.coinManager = new CoinManager(playerManager);
        this.statsManager = new StatsManager(this, statsRepository);
        this.fortuneManager = new FortuneManager(statsManager, fortuneTargetSettings);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(statsManager), this);
        getServer().getPluginManager().registerEvents(new HealthListener(this, statsManager), this);
        getServer().getPluginManager().registerEvents(new ManaListener(this, statsManager), this);
        getServer().getPluginManager().registerEvents(new FarmingFortuneListener(fortuneManager), this);
        getServer().getPluginManager().registerEvents(new MiningFortuneListener(fortuneManager), this);
        getServer().getPluginManager().registerEvents(new ForagingFortuneListener(fortuneManager), this);
    }

    private void registerCommands() {
        PluginCommand coinsCommand = getCommand("coins");

        if (coinsCommand == null) {
            getLogger().warning("plugin.yml に coins コマンドが登録されていません。");
        } else {
            CoinCommand coinCommand = new CoinCommand(coinManager);
            coinsCommand.setExecutor(coinCommand);
            coinsCommand.setTabCompleter(coinCommand);
        }

        PluginCommand statsCommand = getCommand("stats");

        if (statsCommand == null) {
            getLogger().warning("plugin.yml に stats コマンドが登録されていません。");
        } else {
            StatsCommand statsCommandExecutor = new StatsCommand(statsManager);
            statsCommand.setExecutor(statsCommandExecutor);
            statsCommand.setTabCompleter(statsCommandExecutor);
        }

        PluginCommand statsCacheCommand = getCommand("statscache");

        if (statsCacheCommand == null) {
            getLogger().warning("plugin.yml に statscache コマンドが登録されていません。");
        } else {
            statsCacheCommand.setExecutor(new StatsCacheCommand(statsManager));
        }


        PluginCommand lrgCommand = getCommand("lrg");

        if (lrgCommand == null) {
            getLogger().warning("plugin.yml に lrg コマンドが登録されていません。");
        } else {
            StatsDebugCommand statsDebugCommand = new StatsDebugCommand(statsManager);
            lrgCommand.setExecutor(statsDebugCommand);
            lrgCommand.setTabCompleter(statsDebugCommand);
        }
    }

    private void startTasks() {
        if (statsManager != null) {
            statsManager.startActionBarTask();
            statsManager.startManaRegenTask();
            statsManager.startAutoSaveTask();
        }
    }

    private void stopTasks() {
        if (statsManager != null) {
            statsManager.stopAutoSaveTask();
            statsManager.stopManaRegenTask();
            statsManager.stopActionBarTask();
        }
    }

    private void savePlayerDataOnShutdown() {
        if (playerManager != null) {
            playerManager.saveAllSynchronously();
        }
    }

    private void saveStatsDataOnShutdown() {
        if (statsManager != null) {
            statsManager.saveAllSynchronously();
        }
    }

    private void clearPlayerCache() {
        if (playerManager != null) {
            playerManager.clear();
        }
    }

    private void clearStatsCache() {
        if (statsManager != null) {
            statsManager.clear();
        }
    }

    private void closeDatabase() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerRepository getPlayerRepository() { return playerRepository; }
    public StatsRepository getStatsRepository() { return statsRepository; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public CoinManager getCoinManager() { return coinManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public FortuneManager getFortuneManager() { return fortuneManager; }
    public FortuneTargetSettings getFortuneTargetSettings() { return fortuneTargetSettings; }
}
