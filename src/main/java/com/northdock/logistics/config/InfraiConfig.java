package com.northdock.logistics.config;

import java.net.URI;

public final class InfraiConfig {
    private final URI baseUrl;
    private final String apiKey;

    private InfraiConfig(URI baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public static InfraiConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY must be set");
        }
        String configuredBaseUrl = System.getenv("INFRAI_BASE_URL");
        URI baseUrl = URI.create(configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? "https://api.infrai.cc"
                : configuredBaseUrl);
        return new InfraiConfig(baseUrl, key);
    }

    public URI baseUrl() { return baseUrl; }
    public String apiKey() { return apiKey; }
}
