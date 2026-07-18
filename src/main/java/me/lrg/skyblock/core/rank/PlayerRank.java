package me.lrg.skyblock.core.rank;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

public enum PlayerRank {
    ADMIN("Admin", "lrgskyblock.rank.admin", 900, NamedTextColor.RED, true),
    STAFF("Staff", "lrgskyblock.rank.staff", 800, NamedTextColor.AQUA, true),
    YOUTUBE("YouTube", "lrgskyblock.rank.youtube", 700, NamedTextColor.WHITE, true),
    MVP_PLUS_PLUS("MVP++", "lrgskyblock.rank.mvp-plus-plus", 600, NamedTextColor.GOLD, false),
    MVP_PLUS("MVP+", "lrgskyblock.rank.mvp-plus", 500, NamedTextColor.AQUA, false),
    MVP("MVP", "lrgskyblock.rank.mvp", 400, NamedTextColor.AQUA, false),
    VIP_PLUS("VIP+", "lrgskyblock.rank.vip-plus", 300, NamedTextColor.GREEN, false),
    VIP("VIP", "lrgskyblock.rank.vip", 200, NamedTextColor.GREEN, false),
    MEMBER("", "", 0, NamedTextColor.WHITE, false);

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
    public boolean isVisible() { return this != MEMBER; }

    public static PlayerRank parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("++", "_PLUS_PLUS")
                .replace("+", "_PLUS")
                .replace('-', '_');
        if (normalized.equals("PLAYER")) normalized = "MEMBER";
        return PlayerRank.valueOf(normalized);
    }
}
