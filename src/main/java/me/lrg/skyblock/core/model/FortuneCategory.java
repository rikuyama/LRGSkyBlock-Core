package me.lrg.skyblock.core.model;

import java.util.Locale;
import java.util.Optional;

public enum FortuneCategory {

    ORE,
    BLOCK,
    GEMSTONE,
    DWARVEN_METAL,
    FARMING,
    FORAGING;

    public static Optional<FortuneCategory> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
