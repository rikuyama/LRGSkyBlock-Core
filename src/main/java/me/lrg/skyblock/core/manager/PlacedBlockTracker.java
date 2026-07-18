package me.lrg.skyblock.core.manager;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public final class PlacedBlockTracker implements Listener {
    private final JavaPlugin plugin;
    private final File file;
    private final Set<String> placed = new HashSet<>();

    public PlacedBlockTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-placed-blocks.yml");
        load();
    }

    @EventHandler(ignoreCancelled = true)
    public synchronized void onPlace(BlockPlaceEvent event) {
        placed.add(key(event.getBlockPlaced().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public synchronized void onBreak(BlockBreakEvent event) {
        placed.remove(key(event.getBlock().getLocation()));
    }

    public synchronized boolean isPlayerPlaced(Block block) {
        return block != null && placed.contains(key(block.getLocation()));
    }

    public synchronized boolean consume(Block block) {
        return block != null && placed.remove(key(block.getLocation()));
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("blocks", placed.stream().sorted().toList());
        try { config.save(file); }
        catch (IOException exception) { plugin.getLogger().log(Level.SEVERE, "設置ブロック記録の保存に失敗しました。", exception); }
    }

    private void load() {
        if (!file.exists()) return;
        placed.addAll(YamlConfiguration.loadConfiguration(file).getStringList("blocks"));
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
