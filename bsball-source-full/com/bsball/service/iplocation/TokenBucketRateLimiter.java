/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.iplocation.TokenBucketRateLimiter
 */
package com.bsball.service.iplocation;

public final class TokenBucketRateLimiter {
    private final Object lock = new Object();
    private final double maxQps;
    private final int extraSpacingMs;
    private double tokens = 1.0;
    private long lastRefillNanos = System.nanoTime();

    public TokenBucketRateLimiter(double maxQps, int extraSpacingMs) {
        this.maxQps = Math.max(0.1, maxQps);
        this.extraSpacingMs = Math.max(0, extraSpacingMs);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void acquire() {
        long waitMs;
        Object object = this.lock;
        synchronized (object) {
            long now = System.nanoTime();
            double elapsedSeconds = (double)(now - this.lastRefillNanos) / 1.0E9;
            this.tokens = Math.min(1.0, this.tokens + elapsedSeconds * this.maxQps);
            if (this.tokens < 1.0) {
                double need = 1.0 - this.tokens;
                waitMs = (long)Math.ceil(need / this.maxQps * 1000.0);
                waitMs += (long)this.extraSpacingMs;
                this.tokens = 0.0;
            } else {
                this.tokens -= 1.0;
                waitMs = 0L;
            }
        }
        if (waitMs > 0L) {
            try {
                Thread.sleep(waitMs);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void markHttpComplete() {
        Object object = this.lock;
        synchronized (object) {
            this.lastRefillNanos = System.nanoTime();
        }
    }
}

