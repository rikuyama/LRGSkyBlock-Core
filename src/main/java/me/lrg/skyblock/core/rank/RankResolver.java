package me.lrg.skyblock.core.rank;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class RankResolver {
    public static final String YOUTUBE_PERMISSION = "lrgskyblock.badge.youtube";

    public RankProfile resolve(Set<String> permissions) {
        Objects.requireNonNull(permissions, "permissions");
        return resolve(permissions::contains);
    }

    public RankProfile resolve(Predicate<String> permissionChecker) {
        Objects.requireNonNull(permissionChecker, "permissionChecker");

        PlayerRank rank = Arrays.stream(PlayerRank.values())
                .filter(PlayerRank::isVisible)
                .filter(candidate -> permissionChecker.test(candidate.permission()))
                .max(Comparator.comparingInt(PlayerRank::priority))
                .orElse(PlayerRank.PLAYER);

        return new RankProfile(rank, permissionChecker.test(YOUTUBE_PERMISSION));
    }
}
