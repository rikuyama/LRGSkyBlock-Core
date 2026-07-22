package me.lrg.skyblock.core.economy.service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Bank GUIの連打による二重操作を抑止する。 */
public final class BankInteractionGuard {
    private final long cooldownMillis;
    private final ConcurrentMap<UUID, Long> lastAcceptedAt = new ConcurrentHashMap<>();

    public BankInteractionGuard(long cooldownMillis) {
        if (cooldownMillis < 0L) throw new IllegalArgumentException("cooldownMillis must be >= 0");
        this.cooldownMillis = cooldownMillis;
    }

    public boolean tryAcquire(UUID playerId, long nowMillis) {
        Objects.requireNonNull(playerId, "playerId");
        final boolean[] accepted = {false};
        lastAcceptedAt.compute(playerId, (ignored, previous) -> {
            if (previous == null || nowMillis - previous >= cooldownMillis || nowMillis < previous) {
                accepted[0] = true;
                return nowMillis;
            }
            return previous;
        });
        return accepted[0];
    }

    public void clear(UUID playerId) {
        if (playerId != null) lastAcceptedAt.remove(playerId);
    }

    public int trackedPlayers() {
        return lastAcceptedAt.size();
    }
}
