package me.lrg.skyblock.core.rank;

import java.util.Objects;

public record RankProfile(PlayerRank rank) {
    public RankProfile {
        Objects.requireNonNull(rank, "rank");
    }
}
