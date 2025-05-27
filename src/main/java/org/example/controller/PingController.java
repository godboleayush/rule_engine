package org.example.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.service.IteratorService;
import org.example.service.MrService;
import org.example.service.RuleEvaluatorService;
import org.example.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@EnableWebMvc
public class PingController {
    @RequestMapping(path = "/ping", method = RequestMethod.GET)
    public Map<String, String> ping() {
        Map<String, String> pong = new HashMap<>();
        pong.put("pong", "Hello, World!");
        return pong;
    }

    @Autowired
    RuleEvaluatorService ruleEvaluatorService;

    @Autowired
    IteratorService iteratorService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MrService mrService;

    @GetMapping
    public String Test(){
        return "Sab sahi";
    }

    @PostMapping(value = "/upload", consumes = "application/json")
    public String uploadJsonFile(@RequestBody JsonNode jsonContent) {
        try {
            String fileName = S3Service.fileNameProvider(jsonContent);
            String jsonString = new ObjectMapper().writeValueAsString(jsonContent);
            s3Service.uploadJsonString(jsonString , fileName);
            return "JSON content uploaded to S3.";
        } catch (Exception e) {
            return "Upload failed: " + e.getMessage();
        }
    }

    @PostMapping("/evaluate")
    public ObjectNode everything (@RequestBody JsonNode rootNode){

        try {

            if (rootNode.get("DecisionId") == null) {
                throw new IllegalArgumentException("Missing required field: DecisionId");
            }
            if (rootNode.get("Input") == null) {
                throw new IllegalArgumentException("Missing required field: Input");
            }
            if (rootNode.get("strategyId") == null) {
                throw new IllegalArgumentException("Missing required field: strategyId");
            }
            if (rootNode.get("versionName") == null) {
                throw new IllegalArgumentException("Missing required field: versionName");
            }
            if (rootNode.get("versionNumber") == null) {
                throw new IllegalArgumentException("Missing required field: versionNumber");
            }

            Integer decisionId = rootNode.get("DecisionId").asInt();

            Map<String, Object> flatMap = new HashMap<>();
            JsonNode input = rootNode.get("Input");
            s3Service.flattenJson(input, "", flatMap);

            String strategyId = rootNode.get("strategyId").asText();
            String versionName = rootNode.get("versionName").asText();
            String versionNumber = rootNode.get("versionNumber").asText();
            String filename = strategyId + "_" + versionName + "_" + versionNumber + ".json";

            String data = s3Service.getJsonString(filename);
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode json_data = objectMapper.readTree(data);
            JsonNode configurationPropertiesValues = json_data.get("configurationPropertiesValues");
            JsonNode decisions = configurationPropertiesValues.get("decisions");
            final List<ObjectNode>[] val = new List[]{new ArrayList<>()};
            decisions.forEach(decision -> {
                if(decisionId == decision.get("DecisionId").asInt()){
                    List<ObjectNode> v2 = new ArrayList<>();
                    JsonNode ruleSets = decision.get("ruleSets");
                    ruleSets.forEach(ruleSet -> {
                        JsonNode ruleSetNameNode = ruleSet.get("name");
                        JsonNode x = ruleSet.get("rules");
                        ObjectNode v1 = ruleEvaluatorService.RuleEvaluater(x , flatMap , decisionId , ruleSetNameNode);
                        v2.add(v1);
                    });
                    val[0] = v2;
                }
            });

            ObjectNode resp = mrService.transformToOutputFormat(val[0] , input);

            return resp;

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid input: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON data: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("IO error while reading JSON or S3 data: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while evaluating decision logic.", e);
        }
    }

}
