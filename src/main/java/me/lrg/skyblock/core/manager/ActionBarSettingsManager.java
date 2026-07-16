package me.lrg.skyblock.core.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * プレイヤーごとのActionBar表示設定を管理する。
 * 設定はPlayer PersistentDataContainerへ保存される。
 */
public final class ActionBarSettingsManager {

    private static final byte ENABLED = 1;
    private static final byte DISABLED = 0;

    private final NamespacedKey enabledKey;

    public ActionBarSettingsManager(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.enabledKey = new NamespacedKey(plugin, "stats_action_bar_enabled");
    }

    public boolean isEnabled(Player player) {
        Objects.requireNonNull(player, "player");

        PersistentDataContainer container = player.getPersistentDataContainer();
        Byte value = container.get(enabledKey, PersistentDataType.BYTE);

        return value == null || value == ENABLED;
    }

    public void setEnabled(Player player, boolean enabled) {
        Objects.requireNonNull(player, "player");
        player.getPersistentDataContainer().set(
                enabledKey,
                PersistentDataType.BYTE,
                enabled ? ENABLED : DISABLED
        );
    }

    public boolean toggle(Player player) {
        Objects.requireNonNull(player, "player");
        boolean newState = !isEnabled(player);
        setEnabled(player, newState);
        return newState;
    }
}
