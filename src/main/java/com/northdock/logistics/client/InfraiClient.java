package com.northdock.logistics.client;

import com.northdock.logistics.config.InfraiConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final HttpClient http;
    private final InfraiConfig config;

    public InfraiClient(InfraiConfig config) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), config);
    }

    InfraiClient(HttpClient http, InfraiConfig config) {
        this.http = http;
        this.config = config;
    }

    public String createTemporaryKey(String projectId, String name, String scopes, String idempotencyKey) {
        String body = "{\"project_id\":\"" + json(projectId) + "\",\"name\":\"" + json(name)
                + "\",\"scopes\":\"" + json(scopes) + "\",\"idempotency_key\":\"" + json(idempotencyKey) + "\"}";
        return request("POST", "/v1/account/keys/create", body);
    }

    public String reportSuspectedCompromise(String keyId) {
        return request("POST", "/v1/account/keys/suspected_compromise/" + keyId,
                "{\"confirmed_leak\":true,\"auto_rotate\":false}");
    }

    public String rotateTemporaryKey(String keyId, int graceHours, String idempotencyKey) {
        return request("POST", "/v1/account/keys/rotate/" + keyId,
                "{\"grace_hours\":" + graceHours + ",\"idempotency_key\":\"" + json(idempotencyKey) + "\"}");
    }

    public String searchLogs() {
        return request("GET", "/v1/logs/search", null);
    }

    public static String responseId(String envelope) {
        Matcher matcher = ID.matcher(envelope);
        if (!matcher.find()) {
            throw new IllegalStateException("create response did not include an id");
        }
        return matcher.group(1);
    }

    private String request(String method, String path, String body) {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUrl().resolve(path))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(20));
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            try {
                HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                String envelope = response.body();
                Boolean ok = envelopeOk(envelope);
                if (response.statusCode() == 429 && attempt < 2) {
                    pause(response.headers().firstValue("Retry-After").orElse(null), attempt);
                    continue;
                }
                if (ok != null && !ok) {
                    throw new InfraiException(response.statusCode(), value(CODE, envelope, "REQUEST_REJECTED"), envelope);
                }
                if (response.statusCode() >= 500 && attempt < 2) {
                    pause(null, attempt);
                    continue;
                }
                if (response.statusCode() >= 400 || ok == null) {
                    throw new InfraiException(response.statusCode(), "HTTP_ERROR", envelope);
                }
                return envelope;
            } catch (IOException e) {
                if (attempt == 2) throw new IllegalStateException("transport request failed", e);
                pause(null, attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("request interrupted", e);
            }
        }
        throw new IllegalStateException("request exhausted retries");
    }

    private static Boolean envelopeOk(String envelope) {
        Matcher matcher = OK.matcher(envelope);
        return matcher.find() ? Boolean.valueOf(matcher.group(1)) : null;
    }

    private static String value(Pattern pattern, String input, String fallback) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static void pause(String retryAfter, int attempt) {
        long millis = retryAfter == null ? 200L << attempt : Long.parseLong(retryAfter) * 1000L;
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
