package me.lrg.skyblock.core.playerlevel.unlock;

import me.lrg.skyblock.core.playerlevel.manager.PlayerLevelManager;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

public final class PlayerLevelUnlockManager {
    public static final String WARDROBE = "wardrobe";
    public static final String AUTO_PICKUP = "auto_pickup";
    public static final String BAZAAR = "bazaar";
    public static final String ACCESSORY_BAG = "accessory_bag";

    private final PlayerLevelManager levelManager;
    private final Map<String, Integer> requiredLevels = new LinkedHashMap<>();
    private final Map<String, String> displayNames = new LinkedHashMap<>();

    public PlayerLevelUnlockManager(PlayerLevelManager levelManager) {
        this.levelManager = Objects.requireNonNull(levelManager, "levelManager");
        register(WARDROBE, "Wardrobe", 5);
        register(AUTO_PICKUP, "自動回収", 6);
        register(BAZAAR, "バザー", 7);
        register(ACCESSORY_BAG, "Accessory Bag", 10);
    }

    public boolean isUnlocked(UUID uuid, String featureId) {
        Integer required = requiredLevels.get(normalize(featureId));
        return required != null && levelManager.getLevel(uuid) >= required;
    }

    public OptionalInt getRequiredLevel(String featureId) {
        Integer required = requiredLevels.get(normalize(featureId));
        return required == null ? OptionalInt.empty() : OptionalInt.of(required);
    }

    public String getDisplayName(String featureId) {
        String normalized = normalize(featureId);
        return displayNames.getOrDefault(normalized, normalized);
    }

    public Map<String, Integer> getUnlocks() {
        return Map.copyOf(requiredLevels);
    }

    public void register(String featureId, int requiredLevel) {
        register(featureId, featureId, requiredLevel);
    }

    public void register(String featureId, String displayName, int requiredLevel) {
        if (requiredLevel < 1) throw new IllegalArgumentException("requiredLevel must be at least 1");
        String normalized = normalize(featureId);
        requiredLevels.put(normalized, requiredLevel);
        displayNames.put(normalized, Objects.requireNonNull(displayName, "displayName"));
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "featureId").trim().toLowerCase(Locale.ROOT);
    }
}
