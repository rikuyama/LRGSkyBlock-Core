package me.lrg.skyblock.core.economy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BankHolder implements InventoryHolder {
    public enum Screen { HOME, HISTORY }

    private final Screen screen;

    public BankHolder(Screen screen) {
        this.screen = screen;
    }

    public Screen screen() {
        return screen;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
