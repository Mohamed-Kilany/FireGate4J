package definitions;

import api.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Steps {
    private final Pattern pattern = Pattern.compile("\\{(.+?)}");
    private final Context context;

    public Steps(Context context) {
        this.context = context;
    }

    @Given("set base url to {}")
    public void setBaseUrl(String baseUrl) {
        context.set("baseUrl", baseUrl);
    }

    @Given("set endpoint to {}")
    public void setEndpoint(String endpoint) {
        context.set("endpoint", endpoint);
    }

    @Given("add to headers")
    public void addToHeaders(Map<String, String> input) {
        var params = mapToLowerCase(input);
        var existing = context.get("headers", Map.class);
        var headers = new HashMap<String, Object>(existing != null ? existing : Map.of());
        resolveParameterizedMapValues(params, headers);
        context.set("headers", headers);
    }

    @Given("add to path parameters")
    public void addToPathParameters(Map<String, String> input) {
        var params = mapToLowerCase(input);
        var existing = context.get("pathParameters", Map.class);
        var pathParameters = new HashMap<String, Object>(existing != null ? existing : Map.of());
        resolveParameterizedMapValues(params, pathParameters);
        context.set("pathParameters", pathParameters);
    }

    @Given("add to form parameters")
    public void addToFormParameters(Map<String, String> input) {
        var params = mapToLowerCase(input);
        var existing = context.get("formParameters", Map.class);
        var formParameters = new HashMap<String, Object>(existing != null ? existing : Map.of());
        resolveParameterizedMapValues(params, formParameters);
        context.set("formParameters", formParameters);
    }

    @Given("add to query parameters")
    public void addToQueryParameters(Map<String, String> input) {
        var params = mapToLowerCase(input);
        var existing = context.get("queryParameters", Map.class);
        var queryParameters = new HashMap<String, Object>(existing != null ? existing : Map.of());
        resolveParameterizedMapValues(params, queryParameters);
        context.set("queryParameters", queryParameters);
    }

    @Given("add to body")
    public void addToBody(Map<String, String> input) {
        var params = mapToLowerCase(input);
        var existing = context.get("body", Map.class);
        var body = new HashMap<String, Object>(existing != null ? existing : Map.of());
        resolveParameterizedMapValues(params, body);
        context.set("body", body);
    }

    @Given("generate random values")
    public void generateRandomValues(Map<String, String> input) {
        var params = input.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                Map.Entry::getValue));
        RegexGenerator generator = context.get("RegexGenerator", RegexGenerator.class);
        params.forEach((k, v) -> {
            String[] parts = k.split(":");
            String key = parts[0];
            String type = parts.length > 1 ? parts[1] : "string";
            context.set(key, generator.generate(v, type));
        });
    }

    @When("send a {} request")
    public void sendRequest(String method) {
        method = method.toUpperCase();
        String baseUrl = context.get("baseUrl", String.class);
        var headers = context.get("headers", Map.class);
        var pathParameters = context.get("pathParameters", Map.class);
        var formParameters = context.get("formParameters", Map.class);
        var queryParameters = context.get("queryParameters", Map.class);
        String endpoint = context.get("endpoint", String.class);
        Object body = context.get("body", Object.class);
        AsyncRestClient restClient = context.get("RestClient", AsyncRestClient.class);
        CompletableFuture<HttpResponse<String>> response = restClient.sendAsync(
                method,
                baseUrl + endpoint,
                headers,
                pathParameters,
                queryParameters,
                formParameters,
                body
        );
        context.set("response", response);
    }

    @Then("validate status code of {}")
    public void validateStatusCode(Integer expectedStatusCode) {
        HttpResponse<String> response = (HttpResponse<String>) context.get("response", CompletableFuture.class).join();
        assert response.statusCode() == expectedStatusCode;
    }

    @Then("the response body should match schema: {}")
    public void validateSchema(String schemaPath) throws Exception {
        HttpResponse<String> response = (HttpResponse<String>) context.get("response", CompletableFuture.class).join();
        context.get("SchemaValidator", SchemaValidator.class).validate(response.body(), "src/test/resources/schemas/" + schemaPath);
    }

    @Then("extract values from response")
    public void extractValuesFromResponse(Map<String, String> input) {
        var values = mapToLowerCase(input);
        HttpResponse<String> response = (HttpResponse<String>) context.get("response", CompletableFuture.class).join();
        PathExtractor extractor = context.get("PathExtractor", PathExtractor.class);
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        values.forEach((jsonPath, k) -> {
            String[] parts = k.split(":");
            String key = parts[0];
            String type = parts.length > 1 ? parts[1] : "string";
            Object value = extractor.extractByPath(root, jsonPath, key, type);
            context.set(key, value);
        });
    }

    private Map<String, String> mapToLowerCase(Map<String, String> input) {
        return input.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                entry -> entry.getValue().toLowerCase()));
    }

    private void resolveParameterizedMapValues(Map<String, String> source, Map<String, Object> target) {
        TypeConverter typeConverter = context.get("TypeConverter", TypeConverter.class);
        source.forEach((k, v) -> {
            String[] parts = k.split(":");
            String key = parts[0];
            String type = parts.length > 1 ? parts[1] : "string";
            Object value;
            if (v.contains("{")) {
                String resolvedValue = v;
                Matcher matcher = pattern.matcher(v);
                while (matcher.find()) {
                    String placeholder = matcher.group(1);
                    Object contextValue = context.get(placeholder, Object.class);
                    if (contextValue != null) {
                        resolvedValue = resolvedValue.replace("{" + placeholder + "}", String.valueOf(contextValue));
                    }
                }
                value = typeConverter.convert(resolvedValue, type);
            } else {
                value = typeConverter.convert(v, type);
            }
            target.put(key, value);
        });
    }
}
