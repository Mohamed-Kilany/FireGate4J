package api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsyncRestClient {
    private static final Logger logger = LogManager.getLogger(AsyncRestClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final HttpClient httpClient;

    public AsyncRestClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .executor(Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().factory())).build();
    }

    public CompletableFuture<HttpResponse<String>> sendAsync(
            String method,
            String url,
            Map<String, String> headers,
            Map<String, String> pathParams,
            Map<String, String> queryParams,
            Map<String, String> formParams,
            Object body) {

        Instant start = Instant.now();

        if (pathParams != null && !pathParams.isEmpty()) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            String queryString = queryParams.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            url += (url.contains("?") ? "&" : "?") + queryString;
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        HttpRequest.BodyPublisher bodyPublisher;

        if (body != null) {
            bodyPublisher = HttpRequest.BodyPublishers.ofString(body.toString());
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        if (formParams != null && !formParams.isEmpty()) {
            String formBody = formParams.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            bodyPublisher = HttpRequest.BodyPublishers.ofString(formBody);
        }

        requestBuilder.method(method, bodyPublisher);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }

        String finalUrl = url;

        logRequest(method, finalUrl, headers, body);
        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Duration duration = Duration.between(start, Instant.now());
                    logResponse(method, finalUrl, response, duration);
                    return response;
                });
    }

    private void logRequest(String method, String url, Map<String, String> headers, Object body) {
        try {
            logger.debug("""
                            
                            ╔═══════════════════════════════════════════════════
                            ║ HTTP REQUEST
                            ╠═ Method: {}
                            ╠═ URL: {}
                            ╠═ Headers:
                            {}
                            ╠═ Body:
                            {}
                            ╚═══════════════════════════════════════════════════""",
                    method,
                    url,
                    formatHeaders(headers),
                    body != null ? OBJECT_MAPPER.writeValueAsString(body) : "<EMPTY>");
        } catch (Exception e) {
            logger.error("Failed to log request", e);
        }
    }

    private void logResponse(String method, String url,
                             HttpResponse<String> response, Duration duration) {
        try {
            String prettyBody = response.body() != null
                    ? OBJECT_MAPPER.readTree(response.body()).toPrettyString()
                    : "<EMPTY>";

            logger.debug("""
                            
                            ╔═══════════════════════════════════════════════════
                            ║ HTTP RESPONSE
                            ╠═ Method: {}
                            ╠═ URL: {}
                            ╠═ Status: {}
                            ╠═ Duration: {} ms
                            ╠═ Headers:
                            {}
                            ╠═ Body:
                            {}
                            ╚═══════════════════════════════════════════════════""",
                    method,
                    url,
                    response.statusCode(),
                    duration.toMillis(),
                    formatHeaders(response.headers().map()),
                    prettyBody);
        } catch (Exception e) {
            logger.error("Failed to log response", e);
        }
    }

    private String formatHeaders(Map<String, ?> headers) {
        return headers.entrySet().stream()
                .flatMap(entry -> {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        return Stream.of(String.format("| %-40s = %-40s |", entry.getKey(), value));
                    } else if (value instanceof List) {
                        return ((List<?>) value).stream()
                                .map(item -> String.format("| %-40s = %-40s |", entry.getKey(), item));
                    }
                    return Stream.of(String.format("| %-40s = %-40s |", entry.getKey(), "⚠️ Invalid Header"));
                })
                .collect(Collectors.joining("\n"));
    }

    private String serializeBody(Object body) {
        try {
            return OBJECT_MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            logger.error("Failed to serialize body", e);
            throw new RuntimeException(e);
        }
    }
}