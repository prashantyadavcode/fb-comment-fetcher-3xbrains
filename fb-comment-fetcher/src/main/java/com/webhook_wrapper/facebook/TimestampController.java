package com.webhook_wrapper.facebook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing the Redis-based timestamp tracker.
 * Useful for debugging, testing, and manual management of the last fetch timestamp.
 * Works across multiple application instances sharing the same Redis store.
 */
@RestController
@RequestMapping("/api/timestamp")
public class TimestampController {
    private final RedisTimestampTracker timestampTracker;

    public TimestampController(RedisTimestampTracker timestampTracker) {
        this.timestampTracker = timestampTracker;
    }

    /**
     * Get the current last fetch timestamp from Redis.
     * 
     * @return Response with timestamp information
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentTimestamp() {
        long timestamp = timestampTracker.getLastFetchTimestamp();
        Instant instant = timestamp > 0 ? Instant.ofEpochSecond(timestamp) : null;
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", timestamp);
        response.put("instant", instant != null ? instant.toString() : null);
        response.put("hasTimestamp", timestamp > 0);
        response.put("redisHealthy", timestampTracker.isRedisHealthy());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed status information including Redis health.
     * 
     * @return Response with detailed status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long timestamp = timestampTracker.getLastFetchTimestamp();
            Instant instant = timestamp > 0 ? Instant.ofEpochSecond(timestamp) : null;
            boolean redisHealthy = timestampTracker.isRedisHealthy();
            
            response.put("timestamp", timestamp);
            response.put("instant", instant != null ? instant.toString() : null);
            response.put("hasTimestamp", timestamp > 0);
            response.put("redisHealthy", redisHealthy);
            response.put("statusDetails", timestampTracker.getStatus());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to get status: " + e.getMessage());
            response.put("status", "error");
            response.put("redisHealthy", false);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Reset the Redis timestamp tracker.
     * This will cause the next sync to fetch all historical data.
     * Affects all application instances using the same Redis store.
     * 
     * @return Response confirming the reset
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetTimestamp() {
        timestampTracker.reset();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Redis timestamp tracker reset. Next sync will fetch all data.");
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Manually update the timestamp to a specific value.
     * 
     * @param epochSeconds The Unix timestamp in seconds to set
     * @return Response confirming the update
     */
    @PostMapping("/update/{epochSeconds}")
    public ResponseEntity<Map<String, Object>> updateTimestamp(@PathVariable long epochSeconds) {
        if (epochSeconds < 0) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Timestamp must be non-negative");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
        
        timestampTracker.updateLastFetchTimestamp(epochSeconds);
        Instant instant = Instant.ofEpochSecond(epochSeconds);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Timestamp updated successfully");
        response.put("timestamp", epochSeconds);
        response.put("instant", instant.toString());
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update the timestamp to the current time.
     * 
     * @return Response confirming the update
     */
    @PostMapping("/update-now")
    public ResponseEntity<Map<String, Object>> updateToNow() {
        long now = Instant.now().getEpochSecond();
        timestampTracker.updateLastFetchTimestamp(now);
    
        long verifyTimestamp = timestampTracker.getLastFetchTimestamp();
        boolean updateSuccessful = (verifyTimestamp == now);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Timestamp updated to current time");
        response.put("timestamp", now);
        response.put("instant", Instant.ofEpochSecond(now).toString());
        response.put("verifiedTimestamp", verifyTimestamp);
        response.put("updateSuccessful", updateSuccessful);
        response.put("status", updateSuccessful ? "success" : "warning");
        
        if (!updateSuccessful) {
            response.put("warning", "Timestamp update verification failed. Expected: " + now + ", Got: " + verifyTimestamp);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test Redis connection and basic read/write operations
     * 
     * @return Response with test results
     */
    @PostMapping("/test-redis")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test write
            long testTimestamp = 1234567890L;
            System.out.println("Testing Redis with timestamp: " + testTimestamp);
            timestampTracker.updateLastFetchTimestamp(testTimestamp);
            
            // Test read
            long readTimestamp = timestampTracker.getLastFetchTimestamp();
            
            boolean success = (readTimestamp == testTimestamp);
            response.put("testWrite", testTimestamp);
            response.put("testRead", readTimestamp);
            response.put("success", success);
            response.put("status", success ? "success" : "failed");
            
            if (!success) {
                response.put("error", "Write/Read mismatch. Wrote: " + testTimestamp + ", Read: " + readTimestamp);
            }
            
        } catch (Exception e) {
            response.put("error", "Redis test failed: " + e.getMessage());
            response.put("status", "failed");
            response.put("success", false);
        }
        
        return ResponseEntity.ok(response);
    }
}
