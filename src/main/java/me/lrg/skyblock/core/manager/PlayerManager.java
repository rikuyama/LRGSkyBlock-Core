package me.lrg.skyblock.core.manager;

import me.lrg.skyblock.core.model.PlayerData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public void addPlayer(Player player) {

        PlayerData data = new PlayerData(
                player.getUniqueId(),
                player.getName()
        );

        playerDataMap.put(player.getUniqueId(), data);

    }

    public void removePlayer(Player player) {

        playerDataMap.remove(player.getUniqueId());

    }

    public PlayerData getPlayerData(Player player) {

        return playerDataMap.get(player.getUniqueId());

    }

    public boolean hasPlayer(Player player) {

        return playerDataMap.containsKey(player.getUniqueId());

    }

}