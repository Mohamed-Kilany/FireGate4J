package api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaValidator {
    private final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void validate(String json, String schemaPath) throws Exception {
        try (InputStream schemaStream = Files.newInputStream(Paths.get(schemaPath))) {
            JsonSchema schema = SCHEMA_FACTORY.getSchema(schemaStream);
            JsonNode jsonNode = OBJECT_MAPPER.readTree(json);

            Set<ValidationMessage> errors = schema.validate(jsonNode);

            if (!errors.isEmpty()) {
                throw new AssertionError(formatErrors(errors));
            }
        }
    }

    private String formatErrors(Set<ValidationMessage> errors) {
        return "JSON Schema Validation Failed:\n" +
                errors.stream()
                        .map(e -> String.format("- [%s] %s",
                                e.getCode(), e.getMessage()))
                        .collect(Collectors.joining("\n"));
    }
}