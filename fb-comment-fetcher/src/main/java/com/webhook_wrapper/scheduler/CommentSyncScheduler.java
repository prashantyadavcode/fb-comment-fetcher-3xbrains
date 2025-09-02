package com.webhook_wrapper.scheduler;

import com.webhook_wrapper.config.AppProperties;
import com.webhook_wrapper.facebook.CommentTracker;
import com.webhook_wrapper.facebook.FacebookClient;
import com.webhook_wrapper.sheets.SheetsAppender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Component
public class CommentSyncScheduler {
    private final FacebookClient fbClient;
    private final CommentTracker tracker;
    private final SheetsAppender sheetsAppender;
    private final AppProperties appProperties;

    public CommentSyncScheduler(FacebookClient fbClient, CommentTracker tracker, SheetsAppender sheetsAppender, AppProperties appProperties) {
        this.fbClient = fbClient;
        this.tracker = tracker;
        this.sheetsAppender = sheetsAppender;
        this.appProperties = appProperties;
    }

    @Scheduled(fixedRateString = "#{${app.fb.fetch-interval-seconds} * 1000}")
    public void syncComments() {
        Map<String, Object> response = fbClient.fetchPostsAndComments();
        List<Map<String, Object>> posts = (List<Map<String, Object>>) response.get("data");

        if (posts != null) {
            // Step 1: Identify posts with new comments and collect new comments
            Set<String> postsWithNewComments = new HashSet<>();
            Map<String, List<Map<String, Object>>> newCommentsByPost = new HashMap<>();
            
            for (Map<String, Object> post : posts) {
                String postId = (String) post.get("id");
                List<Map<String, Object>> newComments = new java.util.ArrayList<>();

                Map<String, Object> commentsWrapper = (Map<String, Object>) post.get("comments");
                if (commentsWrapper != null) {
                    List<Map<String, Object>> comments = (List<Map<String, Object>>) commentsWrapper.get("data");
                    if (comments != null) {
                        for (Map<String, Object> comment : comments) {
                            String commentId = (String) comment.get("id");
                            if (tracker.isNewComment(commentId)) {
                                newComments.add(comment);
                            }
                        }
                    }
                }
                
                if (!newComments.isEmpty()) {
                    postsWithNewComments.add(postId);
                    newCommentsByPost.put(postId, newComments);
                }
            }

            // Step 2: Fetch post details for posts with new comments
            Map<String, Map<String, Object>> postDetailsCache = new HashMap<>();
            for (String postId : postsWithNewComments) {
                try {
                    Map<String, Object> postDetails = fbClient.fetchPostDetails(postId);
                    postDetailsCache.put(postId, postDetails);
                } catch (Exception e) {
                    System.err.println("Failed to fetch post details for " + postId + ", using basic info");
                    // Create basic post info if detailed fetch fails
                    Map<String, Object> basicPostInfo = new HashMap<>();
                    basicPostInfo.put("id", postId);
                    basicPostInfo.put("message", "");
                    basicPostInfo.put("permalink_url", "");
                    basicPostInfo.put("created_time", "");
                    postDetailsCache.put(postId, basicPostInfo);
                }
            }

            // Step 3: Save comments to Google Sheets
            for (String postId : postsWithNewComments) {
                Map<String, Object> postDetails = postDetailsCache.get(postId);
                List<Map<String, Object>> comments = newCommentsByPost.get(postId);
                
                for (Map<String, Object> comment : comments) {
                    String commentId = (String) comment.get("id");
                    String message = (String) comment.get("message");
                    String createdTime = (String) comment.get("created_time");
                    Map<String, Object> from = (Map<String, Object>) comment.get("from");
                    String user = from != null ? (String) from.get("name") : "Unknown";
                    String fromId = from != null ? (String) from.get("id") : "Unknown";

                    // Extract phone number from message if present (you can customize this logic)
                    String phone = extractPhoneNumber(message);
                    
                    // Format timestamp for Google Sheets
                    String formattedTimestamp = formatTimestamp(createdTime);

                    try {
                        sheetsAppender.appendRow(
                            formattedTimestamp,  // timestamp
                            postId,              // pageId (using postId as pageId)
                            commentId,           // commentId
                            user,                // name
                            fromId,              // fromId
                            message,             // message
                            phone                // phone
                        );
                        System.out.println("Successfully saved comment to Google Sheets: " + commentId);
                    } catch (Exception e) {
                        System.err.println("Error saving to Google Sheets: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Extract phone number from message text
     * You can customize this logic based on your needs
     */
    private String extractPhoneNumber(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        // Simple regex to find phone numbers (you can enhance this)
        String phoneRegex = "(\\+?\\d{1,3}[-\\s]?)?\\(?\\d{3}\\)?[-\\s]?\\d{3}[-\\s]?\\d{4}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(phoneRegex);
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return "";
    }

    /**
     * Format timestamp for Google Sheets
     */
    private String formatTimestamp(String facebookTimestamp) {
        try {
            // Facebook timestamp format: "2025-08-30T10:00:00+0000"
            Instant instant = Instant.parse(facebookTimestamp);
            return DateTimeFormatter.ISO_INSTANT.format(instant);
        } catch (Exception e) {
            // Fallback to current time if parsing fails
            return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        }
    }
}
