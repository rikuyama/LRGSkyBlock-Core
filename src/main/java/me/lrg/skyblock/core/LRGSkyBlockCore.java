package me.lrg.skyblock.core;

import me.lrg.skyblock.core.command.ActionBarCommand;
import me.lrg.skyblock.core.command.CoinCommand;
import me.lrg.skyblock.core.command.FortuneGuiCommand;
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
import me.lrg.skyblock.core.listener.FortuneGuiListener;
import me.lrg.skyblock.core.listener.MiningFortuneListener;
import me.lrg.skyblock.core.listener.ManaListener;
import me.lrg.skyblock.core.listener.PlayerCombatListener;
import me.lrg.skyblock.core.listener.PlayerListener;
import me.lrg.skyblock.core.manager.ActionBarSettingsManager;
import me.lrg.skyblock.core.manager.CoinManager;
import me.lrg.skyblock.core.manager.FortuneManager;
import me.lrg.skyblock.core.manager.PlayerManager;
import me.lrg.skyblock.core.manager.StatsManager;
import me.lrg.skyblock.core.gui.FortuneGui;
import me.lrg.skyblock.core.repository.PlayerRepository;
import me.lrg.skyblock.core.repository.StatsRepository;
import me.lrg.skyblock.core.playerlevel.command.PlayerLevelCommand;
import me.lrg.skyblock.core.playerlevel.database.PlayerLevelSchemaMigrator;
import me.lrg.skyblock.core.playerlevel.listener.PlayerLevelEffectListener;
import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;
import me.lrg.skyblock.core.playerlevel.repository.PlayerLevelRepository;
import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class LRGSkyBlockCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private StatsRepository statsRepository;
    private PlayerLevelRepository playerLevelRepository;
    private PlayerManager playerManager;
    private CoinManager coinManager;
    private ActionBarSettingsManager actionBarSettingsManager;
    private StatsManager statsManager;
    private FortuneManager fortuneManager;
    private PlayerLevelManager playerLevelManager;
    private PlayerLevelUnlockManager playerLevelUnlockManager;
    private PlayerDefaultSettings playerDefaultSettings;
    private FortuneTargetSettings fortuneTargetSettings;
    private FortuneGui fortuneGui;

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
        savePlayerLevelDataOnShutdown();
        clearPlayerCache();
        clearStatsCache();
        clearPlayerLevelCache();
        closeDatabase();
        getLogger().info("LRG SkyBlock Core を停止しました。");
    }

    private void setupDatabase() {
        this.databaseManager = new DatabaseManager(this);
    }

    private void migrateDatabaseSchema() {
        StatsSchemaMigrator statsSchemaMigrator = new StatsSchemaMigrator(databaseManager, getLogger());
        statsSchemaMigrator.migrate();
        new PlayerLevelSchemaMigrator(databaseManager, getLogger()).migrate();
    }

    private void setupConfigs() {
        this.playerDefaultSettings = PlayerDefaultSettings.from(this);
        this.fortuneTargetSettings = FortuneTargetSettings.load(this);
    }

    private void setupRepositories() {
        this.playerRepository = new PlayerRepository(databaseManager, getLogger());
        this.statsRepository = new StatsRepository(databaseManager, getLogger());
        this.playerLevelRepository = new PlayerLevelRepository(databaseManager, getLogger());
    }

    private void setupManagers() {
        this.playerManager = new PlayerManager(this, playerRepository, playerDefaultSettings);
        this.coinManager = new CoinManager(playerManager);
        this.actionBarSettingsManager = new ActionBarSettingsManager(this);
        this.statsManager = new StatsManager(this, statsRepository, actionBarSettingsManager);
        this.fortuneManager = new FortuneManager(statsManager, fortuneTargetSettings);
        this.playerLevelManager = new PlayerLevelManager(this, playerLevelRepository);
        this.playerLevelUnlockManager = new PlayerLevelUnlockManager(playerLevelManager);
        this.fortuneGui = new FortuneGui(fortuneTargetSettings);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, statsManager, playerLevelManager), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(statsManager), this);
        getServer().getPluginManager().registerEvents(new HealthListener(this, statsManager), this);
        getServer().getPluginManager().registerEvents(new ManaListener(this, statsManager), this);
        getServer().getPluginManager().registerEvents(new FarmingFortuneListener(fortuneManager), this);
        getServer().getPluginManager().registerEvents(new MiningFortuneListener(fortuneManager), this);
        getServer().getPluginManager().registerEvents(new ForagingFortuneListener(fortuneManager), this);
        getServer().getPluginManager().registerEvents(new FortuneGuiListener(fortuneGui, fortuneTargetSettings), this);
        getServer().getPluginManager().registerEvents(new PlayerLevelEffectListener(), this);
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


        PluginCommand actionBarCommand = getCommand("actionbar");

        if (actionBarCommand == null) {
            getLogger().warning("plugin.yml に actionbar コマンドが登録されていません。");
        } else {
            ActionBarCommand actionBarCommandExecutor = new ActionBarCommand(
                    actionBarSettingsManager,
                    statsManager
            );
            actionBarCommand.setExecutor(actionBarCommandExecutor);
            actionBarCommand.setTabCompleter(actionBarCommandExecutor);
        }

        PluginCommand statsCacheCommand = getCommand("statscache");

        if (statsCacheCommand == null) {
            getLogger().warning("plugin.yml に statscache コマンドが登録されていません。");
        } else {
            statsCacheCommand.setExecutor(new StatsCacheCommand(statsManager));
        }


        PluginCommand fortuneGuiCommand = getCommand("fortunegui");

        if (fortuneGuiCommand == null) {
            getLogger().warning("plugin.yml に fortunegui コマンドが登録されていません。");
        } else {
            fortuneGuiCommand.setExecutor(new FortuneGuiCommand(fortuneGui));
        }

        PluginCommand levelCommand = getCommand("level");
        if (levelCommand == null) {
            getLogger().warning("plugin.yml に level コマンドが登録されていません。");
        } else {
            PlayerLevelCommand executor = new PlayerLevelCommand(playerLevelManager, playerLevelUnlockManager);
            levelCommand.setExecutor(executor);
            levelCommand.setTabCompleter(executor);
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

    private void savePlayerLevelDataOnShutdown() {
        if (playerLevelManager != null) {
            playerLevelManager.saveAllSynchronously();
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

    private void clearPlayerLevelCache() {
        if (playerLevelManager != null) {
            playerLevelManager.clear();
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
    public PlayerLevelRepository getPlayerLevelRepository() { return playerLevelRepository; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public CoinManager getCoinManager() { return coinManager; }
    public ActionBarSettingsManager getActionBarSettingsManager() { return actionBarSettingsManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public FortuneManager getFortuneManager() { return fortuneManager; }
    public PlayerLevelManager getPlayerLevelManager() { return playerLevelManager; }
    public PlayerLevelUnlockManager getPlayerLevelUnlockManager() { return playerLevelUnlockManager; }
    public FortuneTargetSettings getFortuneTargetSettings() { return fortuneTargetSettings; }
}
