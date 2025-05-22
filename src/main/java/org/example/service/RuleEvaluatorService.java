package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RuleEvaluatorService {
    @Autowired
    IteratorService iteratorService;

    public ObjectNode RuleEvaluater (JsonNode ruleSets , Map<String, Object> flatMap , Integer decisionId , JsonNode ruleSetNameNode){
//        System.out.println(ruleSets);
        ObjectNode output = JsonNodeFactory.instance.objectNode();
        ArrayNode responsesArray = JsonNodeFactory.instance.arrayNode();
        ArrayNode alertsArray = JsonNodeFactory.instance.arrayNode();

        ruleSets.forEach(rule -> {
//            JsonNode ruleSetNameNode = rule.get("name"); // ruleSet name
            JsonNode ruleNameNode = rule.get("name");    // rule name
            JsonNode condition = rule.get("condition");
            JsonNode outcome = rule.get("outcome");

            JsonNode response = outcome.has("response") ? outcome.get("response") : null;
            JsonNode action = (response != null && response.has("action")) ? response.get("action") : null;

            JsonNode alert = outcome.has("alerts") ? outcome.get("alerts") : null;

            boolean isRuleCorrect = iteratorService.checkIteration(condition, flatMap);

            String ruleSetName = (ruleSetNameNode != null && ruleSetNameNode.isTextual()) ? ruleSetNameNode.textValue() : "unknownRuleSet";
            String ruleName = (ruleNameNode != null && ruleNameNode.isTextual()) ? ruleNameNode.textValue() : "unknownRule";

            if (isRuleCorrect && action != null && action.isTextual()) {
                ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
                responseNode.put("action", action.textValue());
                responseNode.put("decision", decisionId);
                responseNode.put("ruleSet", ruleSetName);
                responseNode.put("rule", ruleName);
                responsesArray.add(responseNode);
            }

            if (isRuleCorrect && alert != null && alert.isArray()) {
                for (JsonNode alertNode : alert) {
                    if (alertNode.has("type")) {
                        ObjectNode alertObject = JsonNodeFactory.instance.objectNode();
                        alertObject.put("type", alertNode.get("type").asText());
                        alertObject.put("decision", decisionId);
                        alertObject.put("ruleSet", ruleSetName);
                        alertObject.put("rule", ruleName);
                        alertsArray.add(alertObject);
                    }
                }
            }
        });

        output.set("responses", responsesArray);
        output.set("alerts", alertsArray);
        return output;
    }
}