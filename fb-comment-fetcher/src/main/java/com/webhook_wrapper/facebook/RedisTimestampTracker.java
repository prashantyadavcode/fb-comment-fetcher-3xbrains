package com.webhook_wrapper.facebook;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based timestamp tracker to store the last successful fetch time.
 * This allows multiple application instances to share the same timestamp state
 * and ensures consistency across distributed deployments.
 */
@Component
public class RedisTimestampTracker {
    private static final String TIMESTAMP_KEY = "fb_comments_last_fetch_timestamp";
    private static final String LOCK_KEY = "fb_comments_timestamp_lock";
    private static final long LOCK_TIMEOUT_SECONDS = 30; // Lock timeout to prevent deadlocks
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RedisTimestampTracker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        System.out.println("RedisTimestampTracker initialized with Redis connection");
    }
    
    /**
     * Get the last fetch timestamp as Unix epoch seconds.
     * If no timestamp exists, returns 0 to fetch all historical data.
     * 
     * @return Unix timestamp of last successful fetch, or 0 if none exists
     */
    public long getLastFetchTimestamp() {
        try {
            Object timestampObj = redisTemplate.opsForValue().get(TIMESTAMP_KEY);
            if (timestampObj != null) {
                String timestampStr = timestampObj.toString().trim();
                if (!timestampStr.isEmpty()) {
                    long timestamp = Long.parseLong(timestampStr);
                    System.out.println("✓ Successfully read timestamp from Redis: " + timestamp + " (" + Instant.ofEpochSecond(timestamp) + ")");
                    return timestamp;
                } else {
                    System.out.println("Empty timestamp string in Redis");
                }
            } else {
                System.out.println("Null timestamp object in Redis");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing timestamp from Redis - invalid number format: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error reading last fetch timestamp from Redis: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("No valid previous fetch timestamp found in Redis, returning 0 (will fetch all data)");
        return 0;
    }
    
    /**
     * Update the last fetch timestamp to the current time with distributed locking.
     * Uses Redis locking to prevent race conditions when multiple instances are running.
     */
    public void updateLastFetchTimestamp() {
        updateLastFetchTimestamp(Instant.now().getEpochSecond());
    }
    
    /**
     * Update the last fetch timestamp to a specific time with distributed locking.
     * 
     * @param epochSeconds Unix timestamp in seconds
     */
    public void updateLastFetchTimestamp(long epochSeconds) {
        String lockValue = String.valueOf(System.currentTimeMillis());
        
        try {
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                try {
                    String timestampStr = String.valueOf(epochSeconds);
                    redisTemplate.opsForValue().set(TIMESTAMP_KEY, timestampStr);
                    System.out.println("✓ Successfully updated Redis timestamp with lock: " + epochSeconds);
                    
                    Object verification = redisTemplate.opsForValue().get(TIMESTAMP_KEY);
                    System.out.println("Verification read: " + verification);
                } finally {
                    releaseLock(lockValue);
                }
            } else {
                System.out.println("WARNING: Could not acquire lock for timestamp update. Another instance may be updating the timestamp.");
                String timestampStr = String.valueOf(epochSeconds);
                redisTemplate.opsForValue().set(TIMESTAMP_KEY, timestampStr);
                System.out.println("✓ Updated timestamp without lock (fallback): " + epochSeconds);
                
                Object verification = redisTemplate.opsForValue().get(TIMESTAMP_KEY);
                System.out.println("Verification read: " + verification);
            }
        } catch (Exception e) {
            System.err.println("Error updating last fetch timestamp in Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Release the distributed lock if we still own it.
     */
    private void releaseLock(String lockValue) {
        try {
            Object currentValueObj = redisTemplate.opsForValue().get(LOCK_KEY);
            if (currentValueObj != null && lockValue.equals(currentValueObj.toString())) {
                redisTemplate.delete(LOCK_KEY);
                System.out.println("Released timestamp lock");
            }
        } catch (Exception e) {
            System.err.println("Error releasing timestamp lock: " + e.getMessage());
        }
    }
    
    /**
     * Get the last fetch timestamp as an Instant object.
     * 
     * @return Instant of last fetch, or null if none exists
     */
    public Instant getLastFetchInstant() {
        long timestamp = getLastFetchTimestamp();
        return timestamp > 0 ? Instant.ofEpochSecond(timestamp) : null;
    }
    
    /**
     * Reset the timestamp tracker (useful for testing or full resync).
     * Also removes any existing locks to ensure clean state.
     */
    public void reset() {
        try {
            redisTemplate.delete(TIMESTAMP_KEY);
            redisTemplate.delete(LOCK_KEY);
            System.out.println("Redis timestamp tracker reset - next sync will fetch all data");
        } catch (Exception e) {
            System.err.println("Error resetting Redis timestamp tracker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if Redis connection is healthy.
     * 
     * @return true if Redis is accessible, false otherwise
     */
    public boolean isRedisHealthy() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            System.err.println("Redis health check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get information about the current state for monitoring/debugging.
     * 
     * @return Status information
     */
    public String getStatus() {
        try {
            long timestamp = getLastFetchTimestamp();
            boolean hasLock = redisTemplate.hasKey(LOCK_KEY);
            boolean redisHealthy = isRedisHealthy();
            
            return String.format("Redis Timestamp Tracker Status:\n" +
                "  - Redis Healthy: %s\n" +
                "  - Last Timestamp: %d (%s)\n" +
                "  - Lock Active: %s\n",
                redisHealthy,
                timestamp, 
                timestamp > 0 ? Instant.ofEpochSecond(timestamp).toString() : "None",
                hasLock);
        } catch (Exception e) {
            return "Error getting status: " + e.getMessage();
        }
    }
}
