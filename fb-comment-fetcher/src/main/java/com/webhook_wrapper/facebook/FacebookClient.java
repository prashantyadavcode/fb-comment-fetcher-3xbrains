package com.webhook_wrapper.facebook;

import com.webhook_wrapper.config.AppProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

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
        
        // Create RestTemplate with the configured factory
        RestTemplate template = new RestTemplate(factory);
        
        System.out.println("RestTemplate configured - Connect timeout: 15s, Read timeout: 30s");
        return template;
    }

    public Map<String, Object> fetchPostsAndComments() {
        // Build URL with properly encoded curly braces for Facebook Graph API
        String fieldsParam = "id,comments%7Bcreated_time,from,message,id%7D"; // %7B = { and %7D = }
        String urlString = String.format("https://graph.facebook.com/%s/%s/feed?fields=%s&access_token=%s",
                appProperties.getFb().getApiVersion(),
                appProperties.getFb().getPageId(),
                fieldsParam,
                appProperties.getFb().getAccessToken());
        
        System.out.println("Making request to URL: " + urlString);
        
        try {
            URI uri = URI.create(urlString);
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            System.out.println("Successfully received response from Facebook API");
            return response;
        } catch (Exception e) {
            System.err.println("Error making request to Facebook API: " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                System.err.println("Root cause: " + e.getCause().getMessage());
                System.err.println("Root cause type: " + e.getCause().getClass().getSimpleName());
            }
            e.printStackTrace();
            throw e;
        }
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
