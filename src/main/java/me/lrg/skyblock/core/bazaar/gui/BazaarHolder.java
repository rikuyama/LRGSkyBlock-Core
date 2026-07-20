package me.lrg.skyblock.core.bazaar.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BazaarHolder implements InventoryHolder {
    private final boolean admin;
    public BazaarHolder(boolean admin) { this.admin = admin; }
    public boolean admin() { return admin; }
    @Override public Inventory getInventory() { return null; }
}
