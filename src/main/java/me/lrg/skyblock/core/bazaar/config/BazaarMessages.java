package me.lrg.skyblock.core.bazaar.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class BazaarMessages {
    private final YamlConfiguration config;

    private BazaarMessages(YamlConfiguration config) { this.config = config; }

    public static BazaarMessages load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
                config.save(file);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("messages.ymlの既定値統合に失敗しました: " + exception.getMessage());
        }
        return new BazaarMessages(config);
    }

    public String text(String path) { return color(config.getString(path, path)); }

    public String text(String path, Map<String, String> replacements) {
        String value = text(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        return value;
    }

    public String category(String id) { return text("bazaar.categories." + id.toLowerCase()); }
    public static String color(String value) { return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value); }
}
