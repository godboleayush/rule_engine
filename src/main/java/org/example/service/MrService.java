package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MrService {

    public Object extractValue(JsonNode d, Map<String, Object> flatMap) {
        if (d.isTextual() && d.textValue().startsWith("$")) {
            // This fetches directly from flatMap
            return flatMap.get(d.textValue().substring(1));
        } else if (d.isTextual()) {
            // If it's directly a string (like "1"), return as is
            return d.textValue();
        } else if (d.isNumber()) {
            // Direct numeric value (e.g., 1) → return as number
            return d.numberValue();
        } else {
            return null;
        }
    }


    public JsonNode convertToJsonNode(Object value, ObjectMapper mapper) {
        if (value == null) return null;

        if (value instanceof String) {
            return mapper.getNodeFactory().textNode((String) value);
        } else if (value instanceof Number) {
            return mapper.valueToTree(value); // int, long, double etc.
        } else {
            return mapper.valueToTree(value); // fallback
        }
    }

    public JsonNode resolveToJsonNode(JsonNode inputNode, Map<String, Object> flatMap, ObjectMapper mapper) {
        if (inputNode.isTextual() && inputNode.textValue().startsWith("$")) {
            String key = inputNode.textValue().substring(1);
            Object resolved = flatMap.get(key);

            if (resolved == null) {
                return NullNode.getInstance();
            }

            // Preserve type from flatMap
            if (resolved instanceof Number) {
                return mapper.valueToTree(resolved); // Integer, Double, etc.
            } else if (resolved instanceof String) {
                return TextNode.valueOf((String) resolved); // Explicitly create text node
            } else if (resolved instanceof Boolean) {
                return BooleanNode.valueOf((Boolean) resolved);
            } else {
                return mapper.valueToTree(resolved); // Fallback
            }

        } else {
            // If it's not a reference, return it as-is
            return inputNode;
        }
    }

    public ObjectNode transformToOutputFormat(List<ObjectNode> inputNodes , JsonNode input) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode allResponses = mapper.createArrayNode();
        ArrayNode allAlerts = mapper.createArrayNode();

        for (ObjectNode node : inputNodes) {
            JsonNode responses = node.get("responses");
            JsonNode alerts = node.get("alerts");

            if (responses != null && responses.isArray()) {
                responses.forEach(allResponses::add);
            }

            if (alerts != null && alerts.isArray()) {
                alerts.forEach(allAlerts::add);
            }
        }


        ObjectNode outputNode = mapper.createObjectNode();

        outputNode.set("Input", input);

        ObjectNode outputContent = mapper.createObjectNode();
        outputContent.set("responses", allResponses);
        outputContent.set("alerts", allAlerts);
        outputNode.set("Output", outputContent);

        return outputNode;
    }
}

