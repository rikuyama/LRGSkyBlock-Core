package me.lrg.skyblock.core.autopickup;

import me.lrg.skyblock.core.playerlevel.unlock.PlayerLevelUnlockManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

/**
 * Auto Pickupの共通入口。
 *
 * ブロック、Fortune、Mob、釣り、将来のMinionなど、ドロップ元に関係なく
 * このクラスを通すことで、解放判定と満杯時のフォールバックを統一する。
 */
public final class AutoPickupManager {
    private final PlayerLevelUnlockManager unlockManager;
    private final InventoryDelivery delivery;

    public AutoPickupManager(PlayerLevelUnlockManager unlockManager, InventoryDelivery delivery) {
        this.unlockManager = Objects.requireNonNull(unlockManager, "unlockManager");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    public boolean isUnlocked(Player player) {
        Objects.requireNonNull(player, "player");
        return unlockManager.isUnlocked(player.getUniqueId(), PlayerLevelUnlockManager.AUTO_PICKUP);
    }

    /**
     * 解放済みならインベントリへ配送する。
     * 未解放の場合はfalseを返し、呼び出し側が通常ドロップを続行できるようにする。
     */
    public boolean collect(Player player, Collection<ItemStack> items, Location fallbackLocation) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(fallbackLocation, "fallbackLocation");
        if (!isUnlocked(player)) {
            return false;
        }
        delivery.deliver(player, items, fallbackLocation);
        return true;
    }

    /**
     * 解放済みなら経験値を直接付与する。
     */
    public boolean collectExperience(Player player, int experience) {
        Objects.requireNonNull(player, "player");
        if (experience <= 0 || !isUnlocked(player)) {
            return false;
        }
        player.giveExp(experience);
        return true;
    }
}
