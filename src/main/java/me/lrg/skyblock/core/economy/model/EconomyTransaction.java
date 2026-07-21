package me.lrg.skyblock.core.economy.model;

import java.time.Instant;
import java.util.UUID;

public record EconomyTransaction(
        long id,
        String operationId,
        UUID source,
        UUID target,
        long amount,
        long fee,
        String type,
        String reason,
        Instant createdAt
) {
}
