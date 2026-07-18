package me.lrg.skyblock.core.rank;

import net.kyori.adventure.text.format.NamedTextColor;

public enum PlayerRank {
    ADMIN("Admin", "lrgskyblock.rank.admin", 1000, NamedTextColor.RED, true),
    ENGINEER("Engineer", "lrgskyblock.rank.engineer", 900, NamedTextColor.AQUA, true),
    BUILDER("Builder", "lrgskyblock.rank.builder", 800, NamedTextColor.GOLD, false),
    MODERATOR("Moderator", "lrgskyblock.rank.moderator", 700, NamedTextColor.GREEN, false),
    MVP_PLUS_PLUS("MVP++", "lrgskyblock.rank.mvp-plus-plus", 600, NamedTextColor.AQUA, false),
    MVP_PLUS("MVP+", "lrgskyblock.rank.mvp-plus", 500, NamedTextColor.AQUA, false),
    MVP("MVP", "lrgskyblock.rank.mvp", 400, NamedTextColor.AQUA, false),
    VIP_PLUS("VIP+", "lrgskyblock.rank.vip-plus", 300, NamedTextColor.GREEN, false),
    VIP("VIP", "lrgskyblock.rank.vip", 200, NamedTextColor.GREEN, false),
    PLAYER("", "", 0, NamedTextColor.WHITE, false);

    private final String displayName;
    private final String permission;
    private final int priority;
    private final NamedTextColor nameColor;
    private final boolean bold;

    PlayerRank(String displayName, String permission, int priority, NamedTextColor nameColor, boolean bold) {
        this.displayName = displayName;
        this.permission = permission;
        this.priority = priority;
        this.nameColor = nameColor;
        this.bold = bold;
    }

    public String displayName() { return displayName; }
    public String permission() { return permission; }
    public int priority() { return priority; }
    public NamedTextColor nameColor() { return nameColor; }
    public boolean bold() { return bold; }
    public boolean isVisible() { return this != PLAYER; }
}
