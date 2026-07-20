package me.lrg.skyblock.core.bazaar.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class BazaarMessages {
    private final YamlConfiguration config;

    private BazaarMessages(YamlConfiguration config) {
        this.config = config;
    }

    public static BazaarMessages load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        return new BazaarMessages(YamlConfiguration.loadConfiguration(file));
    }

    public String text(String path) {
        return color(config.getString(path, path));
    }

    public String text(String path, Map<String, String> replacements) {
        String value = text(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    public String category(String id) {
        return text("bazaar.categories." + id.toLowerCase());
    }

    public static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
