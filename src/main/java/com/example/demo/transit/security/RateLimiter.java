package com.example.demo.transit.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory sliding-window limiter (per process). Good enough for a single backend
 * instance; swap for a Redis-backed limiter when running several replicas.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records a hit and returns true when the key is still within {@code max} hits per {@code window}. */
    public boolean tryAcquire(String key, int max, Duration window) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= max) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /** Seconds until the oldest hit in the window expires (for Retry-After style messages). */
    public long secondsUntilFree(String key, Duration window) {
        Deque<Instant> deque = hits.get(key);
        if (deque == null) {
            return 0;
        }
        synchronized (deque) {
            Instant first = deque.peekFirst();
            if (first == null) {
                return 0;
            }
            return Math.max(0, Duration.between(clock.instant(), first.plus(window)).getSeconds());
        }
    }

    public void clear() {
        hits.clear();
    }
}
