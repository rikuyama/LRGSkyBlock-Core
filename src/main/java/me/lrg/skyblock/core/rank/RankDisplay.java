package me.lrg.skyblock.core.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class RankDisplay {
    private static final NamedTextColor BRACKET_COLOR = NamedTextColor.DARK_GRAY;
    private static final TextColor[] RAINBOW_COLORS = {
            NamedTextColor.RED,
            NamedTextColor.GOLD,
            NamedTextColor.YELLOW,
            NamedTextColor.GREEN,
            NamedTextColor.AQUA,
            NamedTextColor.BLUE,
            NamedTextColor.LIGHT_PURPLE
    };

    public Component fullPrefix(int level, RankProfile profile) {
        return fullPrefix(level, profile, 0);
    }

    public Component fullPrefix(int level, RankProfile profile, int animationPhase) {
        TextComponent.Builder builder = Component.text();
        builder.append(levelPrefix(level, animationPhase));
        if (profile.youtube()) {
            builder.append(youtubePrefix());
        }
        builder.append(rankPrefix(profile.rank()));
        return builder.build();
    }

    public Component levelPrefix(int level) {
        return levelPrefix(level, 0);
    }

    public Component levelPrefix(int level, int animationPhase) {
        int safeLevel = Math.max(1, level);
        Component content = isRainbowLevel(safeLevel)
                ? rainbowNumber(safeLevel, animationPhase)
                : Component.text(safeLevel, levelColor(safeLevel));
        return bracketed(content).append(Component.space());
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

    boolean isRainbowLevel(int level) {
        return level >= 520;
    }

    TextColor rainbowColor(int characterIndex, int animationPhase) {
        int colorIndex = Math.floorMod(characterIndex + animationPhase, RAINBOW_COLORS.length);
        return RAINBOW_COLORS[colorIndex];
    }

    private Component rainbowNumber(int level, int animationPhase) {
        String value = Integer.toString(level);
        TextComponent.Builder builder = Component.text();
        for (int index = 0; index < value.length(); index++) {
            builder.append(Component.text(
                    String.valueOf(value.charAt(index)),
                    rainbowColor(index, animationPhase)
            ));
        }
        return builder.build();
    }

    private Component bracketed(Component content) {
        return Component.text("[", BRACKET_COLOR)
                .append(content)
                .append(Component.text("]", BRACKET_COLOR));
    }
}
