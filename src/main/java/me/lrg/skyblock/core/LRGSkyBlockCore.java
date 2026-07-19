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
import me.lrg.skyblock.core.rank.RankCommand;
import me.lrg.skyblock.core.rank.RankNameTagListener;
import me.lrg.skyblock.core.rank.RankNameTagManager;
import me.lrg.skyblock.core.playerlevel.command.PlayerLevelCommand;
import me.lrg.skyblock.core.playerlevel.database.PlayerLevelSchemaMigrator;
import me.lrg.skyblock.core.playerlevel.listener.PlayerLevelEffectListener;
import me.lrg.skyblock.core.playerlevel.listener.PlayerLevelStatsListener;
import me.lrg.skyblock.core.autopickup.AutoPickupListener;
import me.lrg.skyblock.core.autopickup.AutoPickupManager;
import me.lrg.skyblock.core.autopickup.InventoryDelivery;
import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;
import me.lrg.skyblock.core.playerlevel.repository.PlayerLevelRepository;
import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import me.lrg.skyblock.core.wardrobe.command.WardrobeCommand;
import me.lrg.skyblock.core.wardrobe.database.WardrobeSchemaMigrator;
import me.lrg.skyblock.core.wardrobe.gui.WardrobeGui;
import me.lrg.skyblock.core.wardrobe.listener.WardrobeListener;
import me.lrg.skyblock.core.wardrobe.listener.WardrobePlayerListener;
import me.lrg.skyblock.core.wardrobe.manager.WardrobeManager;
import me.lrg.skyblock.core.wardrobe.repository.WardrobeRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public final class LRGSkyBlockCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private StatsRepository statsRepository;
    private PlayerLevelRepository playerLevelRepository;
    private WardrobeRepository wardrobeRepository;
    private PlayerManager playerManager;
    private CoinManager coinManager;
    private ActionBarSettingsManager actionBarSettingsManager;
    private StatsManager statsManager;
    private FortuneManager fortuneManager;
    private me.lrg.skyblock.core.manager.PlacedBlockTracker placedBlockTracker;
    private PlayerLevelManager playerLevelManager;
    private PlayerLevelUnlockManager playerLevelUnlockManager;
    private AutoPickupManager autoPickupManager;
    private WardrobeManager wardrobeManager;
    private WardrobeGui wardrobeGui;
    private PlayerDefaultSettings playerDefaultSettings;
    private FortuneTargetSettings fortuneTargetSettings;
    private FortuneGui fortuneGui;
    private RankNameTagManager rankNameTagManager;
    private BukkitTask rankRefreshTask;

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
        if (placedBlockTracker != null) placedBlockTracker.save();
        clearStatsCache();
        clearPlayerLevelCache();
        clearWardrobeCache();
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
        new WardrobeSchemaMigrator(databaseManager, getLogger()).migrate();
    }

    private void setupConfigs() {
        this.playerDefaultSettings = PlayerDefaultSettings.from(this);
        this.fortuneTargetSettings = FortuneTargetSettings.load(this);
    }

    private void setupRepositories() {
        this.playerRepository = new PlayerRepository(databaseManager, getLogger());
        this.statsRepository = new StatsRepository(databaseManager, getLogger());
        this.playerLevelRepository = new PlayerLevelRepository(databaseManager, getLogger());
        this.wardrobeRepository = new WardrobeRepository(databaseManager, getLogger());
    }

    private void setupManagers() {
        this.playerManager = new PlayerManager(this, playerRepository, playerDefaultSettings);
        this.coinManager = new CoinManager(playerManager);
        this.actionBarSettingsManager = new ActionBarSettingsManager(this);
        this.statsManager = new StatsManager(this, statsRepository, actionBarSettingsManager);
        this.placedBlockTracker = new me.lrg.skyblock.core.manager.PlacedBlockTracker(this);
        this.fortuneManager = new FortuneManager(statsManager, fortuneTargetSettings);
        this.playerLevelManager = new PlayerLevelManager(this, playerLevelRepository);
        this.playerLevelUnlockManager = new PlayerLevelUnlockManager(playerLevelManager);
        this.autoPickupManager = new AutoPickupManager(playerLevelUnlockManager, new InventoryDelivery());
        this.wardrobeManager = new WardrobeManager(this, wardrobeRepository);
        this.wardrobeGui = new WardrobeGui(wardrobeManager);
        this.fortuneGui = new FortuneGui(fortuneTargetSettings);
        this.rankNameTagManager = new RankNameTagManager(playerLevelManager);
    }

    /**
     * Minionなど別システムからAuto Pickupへドロップを渡すための共通API。
     */
    public AutoPickupManager getAutoPickupManager() {
        return autoPickupManager;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager, statsManager, playerLevelManager), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(statsManager), this);
        getServer().getPluginManager().registerEvents(new HealthListener(this, statsManager), this);
        getServer().getPluginManager().registerEvents(new ManaListener(this, statsManager), this);
        getServer().getPluginManager().registerEvents(placedBlockTracker, this);
        getServer().getPluginManager().registerEvents(new FarmingFortuneListener(fortuneManager, placedBlockTracker, autoPickupManager), this);
        getServer().getPluginManager().registerEvents(new MiningFortuneListener(fortuneManager, placedBlockTracker, autoPickupManager), this);
        getServer().getPluginManager().registerEvents(new ForagingFortuneListener(fortuneManager, placedBlockTracker, autoPickupManager), this);
        getServer().getPluginManager().registerEvents(new FortuneGuiListener(fortuneGui, fortuneTargetSettings), this);
        getServer().getPluginManager().registerEvents(new PlayerLevelEffectListener(this, playerLevelUnlockManager), this);
        getServer().getPluginManager().registerEvents(new PlayerLevelStatsListener(playerLevelManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new AutoPickupListener(autoPickupManager), this);
        getServer().getPluginManager().registerEvents(new WardrobePlayerListener(wardrobeManager), this);
        getServer().getPluginManager().registerEvents(new WardrobeListener(wardrobeManager, wardrobeGui), this);
        getServer().getPluginManager().registerEvents(new RankNameTagListener(rankNameTagManager), this);
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

        PluginCommand wardrobeCommand = getCommand("wardrobe");
        if (wardrobeCommand == null) {
            getLogger().warning("plugin.yml に wardrobe コマンドが登録されていません。");
        } else {
            wardrobeCommand.setExecutor(new WardrobeCommand(playerLevelUnlockManager, wardrobeManager, wardrobeGui));
        }

        PluginCommand rankCommand = getCommand("rank");
        if (rankCommand == null) {
            getLogger().warning("plugin.yml に rank コマンドが登録されていません。");
        } else {
            RankCommand executor = new RankCommand(this, rankNameTagManager);
            rankCommand.setExecutor(executor);
            rankCommand.setTabCompleter(executor);
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
        if (rankNameTagManager != null) {
            rankRefreshTask = getServer().getScheduler().runTaskTimer(
                    this,
                    rankNameTagManager::tick,
                    5L,
                    5L
            );
        }
    }

    private void stopTasks() {
        if (rankRefreshTask != null) {
            rankRefreshTask.cancel();
            rankRefreshTask = null;
        }
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

    private void clearWardrobeCache() {
        if (wardrobeManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                wardrobeManager.unload(player.getUniqueId());
            }
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
    public WardrobeRepository getWardrobeRepository() { return wardrobeRepository; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public CoinManager getCoinManager() { return coinManager; }
    public ActionBarSettingsManager getActionBarSettingsManager() { return actionBarSettingsManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public FortuneManager getFortuneManager() { return fortuneManager; }
    public me.lrg.skyblock.core.manager.PlacedBlockTracker getPlacedBlockTracker() { return placedBlockTracker; }
    public PlayerLevelManager getPlayerLevelManager() { return playerLevelManager; }
    public PlayerLevelUnlockManager getPlayerLevelUnlockManager() { return playerLevelUnlockManager; }
    public WardrobeManager getWardrobeManager() { return wardrobeManager; }
    public FortuneTargetSettings getFortuneTargetSettings() { return fortuneTargetSettings; }
}
