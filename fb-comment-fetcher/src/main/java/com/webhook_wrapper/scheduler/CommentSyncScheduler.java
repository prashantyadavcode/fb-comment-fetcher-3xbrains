package com.webhook_wrapper.scheduler;

import com.webhook_wrapper.config.AppProperties;
import com.webhook_wrapper.facebook.FacebookClient;
import com.webhook_wrapper.facebook.RedisTimestampTracker;
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
    private final RedisTimestampTracker timestampTracker;
    private final SheetsAppender sheetsAppender;
    private final AppProperties appProperties;

    public CommentSyncScheduler(FacebookClient fbClient, RedisTimestampTracker timestampTracker, SheetsAppender sheetsAppender, AppProperties appProperties) {
        this.fbClient = fbClient;
        this.timestampTracker = timestampTracker;
        this.sheetsAppender = sheetsAppender;
        this.appProperties = appProperties;
    }

    @Scheduled(fixedRateString = "#{${app.fb.fetch-interval-seconds} * 1000}")
    public void syncComments() {
        System.out.println("Starting scheduler....");

        long lastFetchTimestamp = timestampTracker.getLastFetchTimestamp();
        System.out.println("Last fetch timestamp: " + lastFetchTimestamp);
        
        // Fetch posts and comments since the last timestamp (or all if no previous timestamp)
        Map<String, Object> response = fbClient.fetchPostsAndComments(lastFetchTimestamp > 0 ? lastFetchTimestamp : null);
        System.out.println("Response: " + response);
        List<Map<String, Object>> posts = (List<Map<String, Object>>) response.get("data");

        if (posts != null && !posts.isEmpty()) {
            System.out.println("Found " + posts.size() + " posts to process");
            
            Set<String> postsWithComments = new HashSet<>();
            Map<String, List<Map<String, Object>>> commentsByPost = new HashMap<>();
            
            for (Map<String, Object> post : posts) {
                String postId = (String) post.get("id");
                List<Map<String, Object>> comments = new java.util.ArrayList<>();

                Map<String, Object> commentsWrapper = (Map<String, Object>) post.get("comments");
                if (commentsWrapper != null) {
                    List<Map<String, Object>> commentsList = (List<Map<String, Object>>) commentsWrapper.get("data");
                    if (commentsList != null) {
                        for (Map<String, Object> comment : commentsList) {
                            String commentId = (String) comment.get("id");
                            String createdTime = (String) comment.get("created_time");
                            
                            if (isCommentNewerThanLastFetch(createdTime, lastFetchTimestamp)) {
                                comments.add(comment);
                                System.out.println("Found new comment: " + commentId + " created at: " + createdTime);
                            }
                        }
                    }
                }
                
                if (!comments.isEmpty()) {
                    postsWithComments.add(postId);
                    commentsByPost.put(postId, comments);
                }
            }

            Map<String, Map<String, Object>> postDetailsCache = new HashMap<>();
            for (String postId : postsWithComments) {
                try {
                    Map<String, Object> postDetails = fbClient.fetchPostDetails(postId);
                    postDetailsCache.put(postId, postDetails);
                } catch (Exception e) {
                    System.err.println("Failed to fetch post details for " + postId + ", using basic info");
                    Map<String, Object> basicPostInfo = new HashMap<>();
                    basicPostInfo.put("id", postId);
                    basicPostInfo.put("message", "");
                    basicPostInfo.put("permalink_url", "");
                    basicPostInfo.put("created_time", "");
                    postDetailsCache.put(postId, basicPostInfo);
                }
            }

            int totalCommentsProcessed = 0;
            for (String postId : postsWithComments) {
                Map<String, Object> postDetails = postDetailsCache.get(postId);
                List<Map<String, Object>> comments = commentsByPost.get(postId);
                
                for (Map<String, Object> comment : comments) {
                    String commentId = (String) comment.get("id");
                    String message = (String) comment.get("message");
                    String createdTime = (String) comment.get("created_time");
                    Map<String, Object> from = (Map<String, Object>) comment.get("from");
                    String user = from != null ? (String) from.get("name") : "Unknown";
                    String fromId = from != null ? (String) from.get("id") : "Unknown";

                    String phone = extractPhoneNumber(message);
                    
                    String formattedTimestamp = formatTimestamp(createdTime);

                    String postMessage = postDetails != null ? (String) postDetails.get("message") : "";
                    String postUrl = postDetails != null ? (String) postDetails.get("permalink_url") : "";
                    String postCreatedTime = postDetails != null ? (String) postDetails.get("created_time") : "";
                    String formattedPostTime = formatTimestamp(postCreatedTime);

                    try {
                        sheetsAppender.appendRow(
                            formattedTimestamp,  
                            postId,            
                            commentId,          
                            user,             
                            fromId,             
                            message,            
                            phone,
                            postMessage != null ? postMessage : "",
                            postUrl != null ? postUrl : "",
                            formattedPostTime
                        );
                        totalCommentsProcessed++;
                        System.out.println("Successfully saved comment to Google Sheets: " + commentId);
                    } catch (Exception e) {
                        System.err.println("Error saving to Google Sheets: " + e.getMessage());
                    }
                }
            }
            
            if (totalCommentsProcessed > 0) {
                timestampTracker.updateLastFetchTimestamp();
                System.out.println("Successfully processed " + totalCommentsProcessed + " new comments. Timestamp updated.");
            } else {
                System.out.println("No new comments found to process.");
            }
        } else {
            System.out.println("No posts found in the API response.");
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
    
    /**
     * Check if a comment is newer than the last fetch timestamp.
     * Uses timestamp comparison instead of ID tracking for better performance.
     * 
     * @param commentCreatedTime Facebook comment created_time string
     * @param lastFetchTimestamp Unix timestamp of last successful fetch
     * @return true if comment is newer than last fetch, false otherwise
     */
    private boolean isCommentNewerThanLastFetch(String commentCreatedTime, long lastFetchTimestamp) {
        if (commentCreatedTime == null || commentCreatedTime.trim().isEmpty()) {
            System.out.println("WARNING: Comment has null/empty created_time, treating as new");
            return true;
        }
        
        if (lastFetchTimestamp <= 0) {
            return true;
        }
        
        try {
            Instant commentInstant = Instant.parse(commentCreatedTime);
            long commentTimestamp = commentInstant.getEpochSecond();
            
            return commentTimestamp > lastFetchTimestamp;
        } catch (Exception e) {
            System.err.println("Error parsing comment timestamp '" + commentCreatedTime + "': " + e.getMessage());
            return true;
        }
    }
}
