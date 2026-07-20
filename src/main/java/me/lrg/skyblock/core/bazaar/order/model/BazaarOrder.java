package me.lrg.skyblock.core.bazaar.order.model;

import java.time.Instant;
import java.util.UUID;

public record BazaarOrder(
        long id,
        UUID owner,
        String itemId,
        BazaarOrderSide side,
        long price,
        int originalAmount,
        int remainingAmount,
        BazaarOrderStatus status,
        Instant createdAt
) {
    public BazaarOrder withRemaining(int remaining) {
        return new BazaarOrder(id, owner, itemId, side, price, originalAmount, remaining,
                remaining <= 0 ? BazaarOrderStatus.FILLED : status, createdAt);
    }
}
