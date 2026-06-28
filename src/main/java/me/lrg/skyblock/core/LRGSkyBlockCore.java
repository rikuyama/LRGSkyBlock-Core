package me.lrg.skyblock.core;

import me.lrg.skyblock.core.database.DatabaseManager;
import me.lrg.skyblock.core.database.table.DatabaseSetup;
import me.lrg.skyblock.core.listener.PlayerListener;
import me.lrg.skyblock.core.manager.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LRGSkyBlockCore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerManager playerManager;

    @Override
    public void onEnable() {

        getLogger().info("LRG SkyBlock Core 起動しました");

        saveDefaultConfig();

        databaseManager = new DatabaseManager();
        databaseManager.connect();

        DatabaseSetup setup = new DatabaseSetup(databaseManager);
        setup.createTables();

        playerManager = new PlayerManager();

        getServer().getPluginManager().registerEvents(
                new PlayerListener(playerManager),
                this
        );

    }

    @Override
    public void onDisable() {

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("LRG SkyBlock Core 終了しました");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}