package me.lrg.skyblock.core.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

public final class FortuneTargetRule {

    private final Material blockMaterial;
    private final FortuneCategory category;
    private final int baseDropAmount;
    private final boolean fortuneEnabled;
    private final boolean silkTouchEnabled;
    private final boolean collectionEnabled;
    private final boolean skillExperienceEnabled;
    private final List<Material> fortuneDropMaterials;
    private final String customDropId;

    public FortuneTargetRule(
            Material blockMaterial,
            FortuneCategory category,
            int baseDropAmount,
            boolean fortuneEnabled,
            boolean silkTouchEnabled,
            boolean collectionEnabled,
            boolean skillExperienceEnabled,
            List<Material> fortuneDropMaterials,
            String customDropId
    ) {
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        this.category = Objects.requireNonNull(category, "category");
        this.baseDropAmount = Math.max(1, baseDropAmount);
        this.fortuneEnabled = fortuneEnabled;
        this.silkTouchEnabled = silkTouchEnabled;
        this.collectionEnabled = collectionEnabled;
        this.skillExperienceEnabled = skillExperienceEnabled;
        this.fortuneDropMaterials = List.copyOf(fortuneDropMaterials);
        this.customDropId = customDropId == null ? "" : customDropId.trim();
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public FortuneCategory getCategory() {
        return category;
    }

    public int getBaseDropAmount() {
        return baseDropAmount;
    }

    public boolean isFortuneEnabled() {
        return fortuneEnabled;
    }

    public boolean isSilkTouchEnabled() {
        return silkTouchEnabled;
    }

    public boolean isCollectionEnabled() {
        return collectionEnabled;
    }

    public boolean isSkillExperienceEnabled() {
        return skillExperienceEnabled;
    }

    public List<Material> getFortuneDropMaterials() {
        return fortuneDropMaterials;
    }

    public String getCustomDropId() {
        return customDropId;
    }

    public boolean hasCustomDropId() {
        return !customDropId.isBlank();
    }

    public boolean acceptsDrop(Material dropMaterial) {
        Objects.requireNonNull(dropMaterial, "dropMaterial");
        return fortuneDropMaterials.isEmpty() || fortuneDropMaterials.contains(dropMaterial);
    }
}
