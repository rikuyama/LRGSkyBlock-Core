package me.lrg.skyblock.core.economy.service;

import java.util.UUID;

public final class EconomyOperationIds {
    private EconomyOperationIds() {
    }

    public static String create(String prefix, UUID player) {
        return prefix + ":" + player + ":" + UUID.randomUUID();
    }
}
