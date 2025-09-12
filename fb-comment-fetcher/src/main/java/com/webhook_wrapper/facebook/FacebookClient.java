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
        
        System.setProperty("REDACTED", "true");
        System.setProperty("REDACTED", "dns,sun");
        
        System.out.println("FacebookClient initialized with configured RestTemplate");
    }
    
    private RestTemplate createConfiguredRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
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
        System.out.println("Fetching all posts with comments from Facebook API");

        Map<String, Object> postsResponse = fetchAllPosts();
        
        if (postsResponse == null || postsResponse.get("data") == null) {
            System.out.println("No posts found in Facebook API response");
            return java.util.Collections.emptyMap();
        }

        Object dataObj = postsResponse.get("data");
        if (dataObj instanceof List<?>) {
            List<?> posts = (List<?>) dataObj;
            System.out.println("Step 1: Successfully fetched " + posts.size() + " posts from Facebook API");
            
            System.out.println("Step 2: Fetching comments for each post individually...");
            
            for (Object postObj : posts) {
                if (!(postObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> post = (Map<String, Object>) postObj;
                String postId = (String) post.get("id");
                if (postId == null) continue;

                List<Map<String, Object>> comments = fetchCommentsForPost(postId);
                
                Map<String, Object> commentsWrapper = new HashMap<>();
                commentsWrapper.put("data", comments);
                post.put("comments", commentsWrapper);
                
                System.out.println("Fetched " + comments.size() + " comments for post " + postId);
            }
            
            System.out.println("Completed n+1 API calls: 1 for posts + " + posts.size() + " for comments");
        }

        return postsResponse;
    }
    
    /**
     * Step 1: Fetch all posts from the Facebook page (without comments)
     */
    private Map<String, Object> fetchAllPosts() {
        String urlString = "https://graph.facebook.com/" + appProperties.getFb().getApiVersion() + "/" + 
                          appProperties.getFb().getPageId() + "/posts" +
                          "?fields=id,message,created_time,permalink_url" +
                          "&access_token=" + appProperties.getFb().getAccessToken();
        
        System.out.println("Making posts API call: " + urlString);

        try {
            URI postsUri = URI.create(urlString);
            ResponseEntity<Map> postsResp = restTemplate.exchange(
                postsUri,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
            );
            return postsResp.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching posts: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Step 2: Fetch comments for a specific post (this method will be called n times)
     */
    private List<Map<String, Object>> fetchCommentsForPost(String postId) {
        String urlString = "https://graph.facebook.com/" + appProperties.getFb().getApiVersion() + "/" + 
                          postId + "/comments" +
                          "?fields=id,message,from,created_time" +
                          "&access_token=" + appProperties.getFb().getAccessToken();
        
        System.out.println("Making comments API call for post " + postId + ": " + urlString);

        try {
            URI commentsUri = URI.create(urlString);
            ResponseEntity<Map> commentsResp = restTemplate.exchange(
                commentsUri,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
            );
            
            Map<String, Object> body = commentsResp.getBody();
            if (body == null) return new ArrayList<>();
            
            Object data = body.get("data");
            if (data instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> commentsList = (List<Map<String, Object>>) data;
                return commentsList;
            }
        } catch (Exception e) {
            System.err.println("Error fetching comments for post " + postId + ": " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    
    public Map<String, Object> fetchPostDetails(String postId) {
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
