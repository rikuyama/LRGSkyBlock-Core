package me.lrg.skyblock.core.bazaar.model;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class BazaarItem {
    private final String id;
    private final ItemStack template;
    private long buyPrice;
    private long sellPrice;
    private String category;
    private boolean enabled;

    public BazaarItem(String id, ItemStack template, long buyPrice, long sellPrice, String category, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.template = Objects.requireNonNull(template, "template").clone();
        this.buyPrice = Math.max(1L, buyPrice);
        this.sellPrice = Math.max(0L, sellPrice);
        this.category = Objects.requireNonNullElse(category, "OTHER");
        this.enabled = enabled;
    }

    public String id() { return id; }
    public ItemStack template() { return template.clone(); }
    public long buyPrice() { return buyPrice; }
    public long sellPrice() { return sellPrice; }
    public String category() { return category; }
    public boolean enabled() { return enabled; }
    public void setBuyPrice(long value) { buyPrice = Math.max(1L, value); }
    public void setSellPrice(long value) { sellPrice = Math.max(0L, value); }
    public void setCategory(String value) { category = Objects.requireNonNullElse(value, "OTHER"); }
    public void setEnabled(boolean value) { enabled = value; }
}
