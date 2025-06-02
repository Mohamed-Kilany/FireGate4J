package api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathExtractor {
    private static final Logger logger = LogManager.getLogger(PathExtractor.class);
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("(.+)\\[(\\d+)]");

    public Object extractByPath(JsonNode root, String path, String key, String type) {
        String effectivePath = path.startsWith("$.") ? path.substring(2) : path;
        String[] pathParts = effectivePath.split("\\.");
        List<JsonNode> nodes = new ArrayList<>();

        traverse(root, pathParts, 0, nodes, false);
        Object result = processResults(nodes, type);
        try {
            logger.debug("""
                    
                    ╔═══════════════════════════════════════════════════
                    ║ EXTRACTED VALUE FROM REQUEST
                    ╠═ PATH: {}
                    ╠═ KEY: {}
                    ╠═ VALUE: {}
                    ╠═ TYPE: {}
                    ╚═══════════════════════════════════════════════════""", path, key, result, type);
        } catch (Exception e) {
            logger.error("Failed to extracted value!", e);
        }
        Assert.assertNotNull(result, String.format("extracting value from path: %s returns null, seems it does not exist", path));
        return result;
    }

    private Object processResults(List<JsonNode> nodes, String type) {
        if (nodes.isEmpty()) return null;
        TypeConverter typeConverter = new TypeConverter();
        if (type.startsWith("list<") && type.endsWith(">")) {
            return nodes.stream()
                    .map(node -> typeConverter.convert(node.asText(), type.substring(5, type.length() - 1)))
                    .collect(Collectors.toList());
        }

        if (nodes.size() > 1) {
            logger.warn("multiple values found for single value path, using first value only");
        }
        return typeConverter.convert(nodes.getFirst().asText(), type);
    }

    private void traverse(JsonNode node, String[] pathParts, int depth,
                          List<JsonNode> results, boolean wildcardMode) {
        if (node == null || depth >= pathParts.length) return;

        String part = pathParts[depth];
        boolean isLastPart = (depth == pathParts.length - 1);

        Matcher arrayMatcher = ARRAY_INDEX_PATTERN.matcher(part);
        if (arrayMatcher.matches()) {
            handleArrayIndex(node, arrayMatcher.group(1),
                    Integer.parseInt(arrayMatcher.group(2)),
                    pathParts, depth, results, isLastPart);
            return;
        }

        if (!wildcardMode && node.isArray()) {
            wildcardMode = true;
            for (JsonNode child : node) {
                traverse(child, pathParts, depth, results, wildcardMode);
            }
            return;
        }

        JsonNode nextNode = node.path(part);
        if (nextNode.isMissingNode()) return;

        if (isLastPart) {
            collectResults(nextNode, results);
        } else {
            traverse(nextNode, pathParts, depth + 1, results, false);
        }
    }

    private void handleArrayIndex(JsonNode node, String fieldName, int index,
                                  String[] pathParts, int depth,
                                  List<JsonNode> results, boolean isLastPart) {
        JsonNode arrayNode = node.path(fieldName);
        if (!arrayNode.isArray() || index >= arrayNode.size()) return;

        JsonNode element = arrayNode.get(index);
        if (isLastPart) {
            results.add(element);
        } else {
            traverse(element, pathParts, depth + 1, results, false);
        }
    }

    private void collectResults(JsonNode node, List<JsonNode> results) {
        if (node.isArray()) {
            node.forEach(results::add);
        } else {
            results.add(node);
        }
    }
}