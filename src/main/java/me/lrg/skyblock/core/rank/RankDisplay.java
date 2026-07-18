package me.lrg.skyblock.core.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class RankDisplay {
    private static final NamedTextColor BRACKET_COLOR = NamedTextColor.DARK_GRAY;

    public Component fullPrefix(int level, RankProfile profile) {
        TextComponent.Builder builder = Component.text();
        builder.append(levelPrefix(level));
        if (profile.youtube()) {
            builder.append(youtubePrefix());
        }
        builder.append(rankPrefix(profile.rank()));
        return builder.build();
    }

    public Component levelPrefix(int level) {
        return bracketed(Component.text(Math.max(1, level), levelColor(level))).append(Component.space());
    }

    public Component youtubePrefix() {
        return bracketed(Component.text("YouTube", NamedTextColor.RED)).append(Component.space());
    }

    public Component rankPrefix(PlayerRank rank) {
        if (!rank.isVisible()) {
            return Component.empty();
        }

        Component content = switch (rank) {
            case VIP_PLUS -> Component.text("VIP", NamedTextColor.GREEN)
                    .append(Component.text("+", NamedTextColor.YELLOW));
            case MVP_PLUS -> Component.text("MVP", NamedTextColor.AQUA)
                    .append(Component.text("+", NamedTextColor.RED));
            case MVP_PLUS_PLUS -> Component.text("MVP", NamedTextColor.AQUA)
                    .append(Component.text("++", NamedTextColor.RED));
            default -> Component.text(rank.displayName(), rank.nameColor())
                    .decoration(TextDecoration.BOLD, rank.bold());
        };

        return bracketed(content).append(Component.space());
    }

    public NamedTextColor nameColor(RankProfile profile) {
        return profile.rank() == PlayerRank.PLAYER && profile.youtube()
                ? NamedTextColor.RED
                : profile.rank().nameColor();
    }

    public NamedTextColor levelColor(int level) {
        if (level >= 480) return NamedTextColor.DARK_RED;
        if (level >= 440) return NamedTextColor.RED;
        if (level >= 400) return NamedTextColor.GOLD;
        if (level >= 360) return NamedTextColor.DARK_PURPLE;
        if (level >= 320) return NamedTextColor.LIGHT_PURPLE;
        if (level >= 280) return NamedTextColor.BLUE;
        if (level >= 240) return NamedTextColor.AQUA;
        if (level >= 200) return NamedTextColor.DARK_AQUA;
        if (level >= 160) return NamedTextColor.DARK_GREEN;
        if (level >= 120) return NamedTextColor.GREEN;
        if (level >= 80) return NamedTextColor.YELLOW;
        if (level >= 40) return NamedTextColor.WHITE;
        return NamedTextColor.GRAY;
    }

    private Component bracketed(Component content) {
        return Component.text("[", BRACKET_COLOR)
                .append(content)
                .append(Component.text("]", BRACKET_COLOR));
    }
}
