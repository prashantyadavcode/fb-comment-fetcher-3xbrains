package com.webhook_wrapper.facebook;

import com.webhook_wrapper.config.AppProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Component
public class FacebookClient {
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public FacebookClient(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restTemplate = createConfiguredRestTemplate();
        
        // Add system properties to help with network debugging
        System.setProperty("REDACTED", "true");
        System.setProperty("REDACTED", "dns,sun");
        
        System.out.println("FacebookClient initialized with configured RestTemplate");
    }
    
    private RestTemplate createConfiguredRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Set timeouts
        factory.setConnectTimeout(15000); // 15 seconds connection timeout
        factory.setReadTimeout(30000);    // 30 seconds read timeout
        
        RestTemplate template = new RestTemplate(factory);
        
        System.out.println("RestTemplate configured - Connect timeout: 15s, Read timeout: 30s");
        return template;
    }

    public Map<String, Object> fetchPostsAndComments() {
        return fetchPostsAndComments(null);
    }
    
    public Map<String, Object> fetchPostsAndComments(Long sinceTimestamp) {
        StringBuilder postsUrl = new StringBuilder();
        postsUrl.append("https://graph.facebook.com/")
                .append(appProperties.getFb().getApiVersion()).append("/")
                .append(appProperties.getFb().getPageId()).append("/posts")
                .append("?fields=id,message,created_time")
                .append("&access_token=")
                .append(appProperties.getFb().getAccessToken());

        if (sinceTimestamp != null && sinceTimestamp > 0) {
            postsUrl.append("&since=").append(sinceTimestamp);
            System.out.println("Fetching posts since timestamp: " + sinceTimestamp + " (" + java.time.Instant.ofEpochSecond(sinceTimestamp) + ")");
        } else {
            System.out.println("Fetching all posts (no timestamp filter)");
        }

        String postsUrlString = postsUrl.toString();
        System.out.println("Making request to URL: " + postsUrlString);

        Map<String, Object> postsResponse;
        try {
            URI postsUri;
            try {
                postsUri = new URI(postsUrlString);
            } catch (java.net.URISyntaxException e) {
                System.err.println("Invalid URL syntax (posts): " + postsUrlString);
                throw new RuntimeException("Invalid Facebook posts URL: " + e.getMessage(), e);
            }
            ResponseEntity<Map> postsResp = restTemplate.exchange(
                postsUri,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
            );
            postsResponse = postsResp.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching posts: " + e.getMessage());
            throw e;
        }

        if (postsResponse == null) {
            return java.util.Collections.emptyMap();
        }

        // Step 2: For each post, fetch comments separately and attach to match existing scheduler expectations
        Object dataObj = postsResponse.get("data");
        if (dataObj instanceof List<?>) {
            List<?> posts = (List<?>) dataObj;
            for (Object postObj : posts) {
                if (!(postObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> post = (Map<String, Object>) postObj;
                String postId = (String) post.get("id");
                if (postId == null) continue;

                List<Map<String, Object>> comments = fetchCommentsForPost(postId, sinceTimestamp);
                Map<String, Object> commentsWrapper = new HashMap<>();
                commentsWrapper.put("data", comments);
                post.put("comments", commentsWrapper);
            }
        }

        return postsResponse;
    }

    private List<Map<String, Object>> fetchCommentsForPost(String postId, Long sinceTimestamp) {
        StringBuilder commentsUrl = new StringBuilder();
        commentsUrl.append("https://graph.facebook.com/")
                .append(appProperties.getFb().getApiVersion()).append("/")
                .append(postId).append("/comments")
                .append("?fields=id,message,from,created_time")
                .append("&access_token=")
                .append(appProperties.getFb().getAccessToken());

        if (sinceTimestamp != null && sinceTimestamp > 0) {
            commentsUrl.append("&since=").append(sinceTimestamp);
        }

        String commentsUrlString = commentsUrl.toString();
        try {
            URI commentsUri;
            try {
                commentsUri = new URI(commentsUrlString);
            } catch (java.net.URISyntaxException e) {
                System.err.println("Invalid URL syntax (comments): " + commentsUrlString);
                throw new RuntimeException("Invalid Facebook comments URL: " + e.getMessage(), e);
            }
            ResponseEntity<Map> commentsResp = restTemplate.exchange(
                commentsUri,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
            );
            Map body = commentsResp.getBody();
            if (body == null) return new ArrayList<>();
            Object data = body.get("data");
            if (data instanceof java.util.List<?>) {
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> list = (List<Map<String, Object>>) data;
                return list;
            }
        } catch (Exception e) {
            System.err.println("Error fetching comments for post " + postId + ": " + e.getMessage());
        }
        return new ArrayList<>();
    }
    
    public Map<String, Object> fetchPostDetails(String postId) {
        // Fetch individual post details with id, message, permalink_url, created_time
        String fieldsParam = "id,message,permalink_url,created_time";
        String urlString = String.format("https://graph.facebook.com/%s/%s?fields=%s&access_token=%s",
                appProperties.getFb().getApiVersion(),
                postId,
                fieldsParam,
                appProperties.getFb().getAccessToken());
        
        System.out.println("Fetching post details for: " + postId);
        
        try {
            URI uri = URI.create(urlString);
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            System.out.println("Successfully received post details for: " + postId);
            return response;
        } catch (Exception e) {
            System.err.println("Error fetching post details for " + postId + ": " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                System.err.println("Root cause: " + e.getCause().getMessage());
                System.err.println("Root cause type: " + e.getCause().getClass().getSimpleName());
            }
            e.printStackTrace();
            throw e;
        }
    }
}
