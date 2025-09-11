package com.webhook_wrapper.sheets;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
@ConditionalOnProperty(name = "google.sheetId", matchIfMissing = false)
public class SheetsAppender {
    private static final Logger logger = LoggerFactory.getLogger(SheetsAppender.class);
    
    private Sheets sheets;
    private final String sheetId;
    private final String range;
    private boolean initialized;

    public SheetsAppender(
            @Value("${google.sheetId:}") String sheetId,
            @Value("${google.range:Sheet1!A:G}") String range
    ) throws Exception {
        this.sheetId = sheetId;
        this.range = range;
        
        // Check if we have the required configuration
        if (sheetId == null || sheetId.trim().isEmpty()) {
            logger.warn("Google Sheets integration disabled: google.sheetId not configured");
            this.sheets = null;
            this.initialized = false;
            return;
        }

        logger.info("Initializing Google Sheets client for sheet: {}", sheetId);
        
        try {
            // Load service account credentials from JSON file
            GoogleCredentials creds;
            try (java.io.FileInputStream serviceAccountStream = new java.io.FileInputStream("service-account-key.json")) {
                creds = GoogleCredentials.fromStream(serviceAccountStream)
                        .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));
            }

            this.sheets = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(creds))
                    .setApplicationName("fb-comment-sheets")
                    .build();
                    
            this.initialized = true;
            logger.info("Google Sheets client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Google Sheets client: {}", e.getMessage());
            this.sheets = null;
            this.initialized = false;
            throw e;
        }
    }

    /**
     * Health check: verifies API + sheet access
     */
    public void readiness() throws IOException {
        if (!initialized) {
            throw new IOException("Google Sheets integration not initialized");
        }
        
        logger.debug("Performing readiness check for sheet: {}", sheetId);
        try {
            sheets.spreadsheets().get(sheetId).setIncludeGridData(false).execute();
            logger.debug("Readiness check passed for sheet: {}", sheetId);
        } catch (IOException e) {
            logger.error("Readiness check failed for sheet {}: {}", sheetId, e.getMessage());
            throw e;
        }
    }

    /**
     * Appends a new row to the configured Google Sheet
     */
    public void appendRow(String timestamp, String pageId, String commentId,
                         String name, String fromId, String message, String phone,
                         String postMessage, String postUrl, String postCreatedTime) throws IOException {
        if (!initialized) {
            throw new IOException("Google Sheets integration not initialized");
        }
        
        logger.debug("Appending row to sheet {}: timestamp={}, pageId={}, commentId={}", 
                    sheetId, timestamp, pageId, commentId);
        
        try {
            List<Object> row = List.of(timestamp, pageId, commentId, name, fromId, message, phone, 
                                     postMessage, postUrl, postCreatedTime);
            ValueRange body = new ValueRange().setValues(List.of(row));
            
            sheets.spreadsheets().values()
                    .append(sheetId, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
                    
            logger.info("Successfully appended row to sheet {}: commentId={}", sheetId, commentId);
        } catch (IOException e) {
            logger.error("Failed to append row to sheet {}: {}", sheetId, e.getMessage());
            throw e;
        }
    }
}
