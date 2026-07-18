package me.lrg.skyblock.core.playerlevel.unlock;

import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

public final class PlayerLevelUnlockManager {
    public static final String AUTO_PICKUP = "auto_pickup";
    private final PlayerLevelManager levelManager;
    private final Map<String, Integer> requiredLevels = new LinkedHashMap<>();

    public PlayerLevelUnlockManager(PlayerLevelManager levelManager) {
        this.levelManager = Objects.requireNonNull(levelManager, "levelManager");
        requiredLevels.put(AUTO_PICKUP, 6);
    }

    public boolean isUnlocked(UUID uuid, String featureId) {
        Integer required = requiredLevels.get(normalize(featureId));
        return required != null && levelManager.getLevel(uuid) >= required;
    }

    public OptionalInt getRequiredLevel(String featureId) {
        Integer required = requiredLevels.get(normalize(featureId));
        return required == null ? OptionalInt.empty() : OptionalInt.of(required);
    }

    public Map<String, Integer> getUnlocks() {
        return Map.copyOf(requiredLevels);
    }

    public void register(String featureId, int requiredLevel) {
        if (requiredLevel < 1) throw new IllegalArgumentException("requiredLevel must be at least 1");
        requiredLevels.put(normalize(featureId), requiredLevel);
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "featureId").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
