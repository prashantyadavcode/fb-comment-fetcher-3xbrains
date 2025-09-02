package com.webhook_wrapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="app")
public class AppProperties {
    private Fb fb = new Fb();

    public static class Fb {
        private String pageId;
        private String accessToken;
        private String apiVersion;
        private int fetchIntervalSeconds;

        // getters and setters
        public String getPageId() {
            return pageId;
        }
        public void setPageId(String pageId) {
            this.pageId = pageId;
        }
        public String getAccessToken() {
            return accessToken;
        }
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        public String getApiVersion() {
            return apiVersion;
        }
        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }
        public int getFetchIntervalSeconds() {
            return fetchIntervalSeconds;
        }

        public void setFetchIntervalSeconds(int fetchIntervalSeconds) {
            this.fetchIntervalSeconds = fetchIntervalSeconds;
        }
    }

    public Fb getFb() {
        return fb;
    }
}
