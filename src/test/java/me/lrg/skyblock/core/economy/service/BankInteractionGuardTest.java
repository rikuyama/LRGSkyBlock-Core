package me.lrg.skyblock.core.economy.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankInteractionGuardTest {
    @Test
    void rejectsRapidRepeatedAction() {
        BankInteractionGuard guard = new BankInteractionGuard(750L);
        UUID player = UUID.randomUUID();
        assertTrue(guard.tryAcquire(player, 1_000L));
        assertFalse(guard.tryAcquire(player, 1_749L));
        assertTrue(guard.tryAcquire(player, 1_750L));
    }

    @Test
    void playersAreIndependent() {
        BankInteractionGuard guard = new BankInteractionGuard(750L);
        assertTrue(guard.tryAcquire(UUID.randomUUID(), 1_000L));
        assertTrue(guard.tryAcquire(UUID.randomUUID(), 1_000L));
    }

    @Test
    void clearRemovesTrackedState() {
        BankInteractionGuard guard = new BankInteractionGuard(750L);
        UUID player = UUID.randomUUID();
        guard.tryAcquire(player, 1_000L);
        guard.clear(player);
        assertEquals(0, guard.trackedPlayers());
        assertTrue(guard.tryAcquire(player, 1_001L));
    }
}
