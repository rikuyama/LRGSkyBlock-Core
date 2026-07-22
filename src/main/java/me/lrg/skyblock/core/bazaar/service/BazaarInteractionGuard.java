package me.lrg.skyblock.core.bazaar.service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bazaarの決済・注文操作を短時間に複数回実行させないためのガード。
 */
public final class BazaarInteractionGuard {
    private final long cooldownMillis;
    private final ConcurrentMap<UUID, Long> blockedUntil = new ConcurrentHashMap<>();

    public BazaarInteractionGuard(long cooldownMillis) {
        if (cooldownMillis < 0L) {
            throw new IllegalArgumentException("cooldownMillis must be >= 0");
        }
        this.cooldownMillis = cooldownMillis;
    }

    public boolean tryAcquire(UUID uuid, long nowMillis) {
        Objects.requireNonNull(uuid, "uuid");
        if (nowMillis < 0L) {
            throw new IllegalArgumentException("nowMillis must be >= 0");
        }

        final boolean[] acquired = {false};
        blockedUntil.compute(uuid, (ignored, currentUntil) -> {
            if (currentUntil != null && currentUntil > nowMillis) {
                return currentUntil;
            }
            acquired[0] = true;
            return safeAdd(nowMillis, cooldownMillis);
        });
        return acquired[0];
    }

    public void clear(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        blockedUntil.remove(uuid);
    }

    public int trackedPlayers() {
        return blockedUntil.size();
    }

    private static long safeAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
