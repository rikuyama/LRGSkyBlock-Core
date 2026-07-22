package me.lrg.skyblock.core.bazaar.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BazaarInteractionGuardTest {
    @Test
    void blocksRepeatedActionDuringCooldown() {
        BazaarInteractionGuard guard = new BazaarInteractionGuard(750L);
        UUID player = UUID.randomUUID();

        assertTrue(guard.tryAcquire(player, 1_000L));
        assertFalse(guard.tryAcquire(player, 1_749L));
        assertTrue(guard.tryAcquire(player, 1_750L));
    }

    @Test
    void playersHaveIndependentCooldowns() {
        BazaarInteractionGuard guard = new BazaarInteractionGuard(750L);

        assertTrue(guard.tryAcquire(UUID.randomUUID(), 1_000L));
        assertTrue(guard.tryAcquire(UUID.randomUUID(), 1_000L));
    }

    @Test
    void clearAllowsImmediateRetry() {
        BazaarInteractionGuard guard = new BazaarInteractionGuard(750L);
        UUID player = UUID.randomUUID();

        assertTrue(guard.tryAcquire(player, 1_000L));
        guard.clear(player);
        assertTrue(guard.tryAcquire(player, 1_001L));
    }
}
