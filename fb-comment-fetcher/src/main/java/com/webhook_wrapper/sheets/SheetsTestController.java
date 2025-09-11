package com.webhook_wrapper.sheets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sheets")
@ConditionalOnProperty(name = "google.sheetId", matchIfMissing = false)
public class SheetsTestController {
    private static final Logger logger = LoggerFactory.getLogger(SheetsTestController.class);
    
    private final SheetsAppender appender;
    
    @Autowired
    public SheetsTestController(SheetsAppender appender) { 
        this.appender = appender; 
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.info("Health check requested");
        try {
            appender.readiness();
            logger.info("Health check passed");
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("NOT OK: " + e.getMessage());
        }
    }

    @PostMapping("/append")
    public ResponseEntity<?> append(@RequestBody Map<String, String> body) {
        logger.info("Append request received: {}", body);
        try {
            appender.appendRow(
                    body.getOrDefault("timestamp", ""),
                    body.getOrDefault("pageId", ""),
                    body.getOrDefault("commentId", ""),
                    body.getOrDefault("name", ""),
                    body.getOrDefault("fromId", ""),
                    body.getOrDefault("message", ""),
                    body.getOrDefault("phone", ""),
                    body.getOrDefault("postMessage", ""),
                    body.getOrDefault("postUrl", ""),
                    body.getOrDefault("postCreatedTime", "")
            );
            logger.info("Append request completed successfully");
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            logger.error("Append request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
