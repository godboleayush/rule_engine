package org.example.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private final AmazonS3 s3Client;
    private final String bucketName = "ayush2604";

    public S3Service() {
//        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
//        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String region = "ap-south-1";

        // If any credential is missing, throw an exception
//        if (accessKey == null || secretKey == null || region == null) {
//            throw new IllegalArgumentException("AWS credentials not set correctly in environment variables.");
//        }

//        if (region == null) {
//            throw new IllegalArgumentException("AWS credentials not set correctly in environment variables.");
//        }



//        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
    }

    public void uploadJsonString(String jsonContent , String fileName) {
        try {
            byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(contentBytes);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentBytes.length);
            metadata.setContentType("application/json");

//            String key = "uploaded-" + System.currentTimeMillis() + ".json"; // auto-generated unique filename
            String key = fileName + ".json";

            s3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload JSON to S3", e);
        }
    }

    public String getJsonString(String filename) {
        try {
            String key = filename; // same key used for upload
            S3Object s3Object = s3Client.getObject(bucketName, key);
            InputStream inputStream = s3Object.getObjectContent();

            // Read input stream to string
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON from S3", e);
        }
    }

    public void flattenJson(JsonNode node, String prefix, Map<String, Object> map) {
        if (node.isObject()) {
            // If it's an object, iterate over its fields and recursively call flattenJson
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                flattenJson(entry.getValue(), prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey(), map);
            }
        } else {
            // Check if the value is an integer or string and store accordingly
            if (node.isInt()) {
                // If it's an integer, store it as an Integer object
                map.put(prefix, Integer.valueOf(node.intValue()));
            } else if (node.isTextual()) {
                // If it's a string, store it as a String object
                map.put(prefix, node.textValue());
            } else if (node.isBoolean()) {
                // If it's a boolean, store it as a Boolean object
                map.put(prefix, Boolean.valueOf(node.booleanValue()));
            } else if (node.isDouble()) {
                // If it's a double (floating-point number), store it as a Double object
                map.put(prefix, Double.valueOf(node.doubleValue()));
            } else if (node.isLong()) {
                // If it's a long (integer > Integer.MAX_VALUE), store it as a Long object
                map.put(prefix, Long.valueOf(node.longValue()));
            } else {
                // For other types, store as text by default (could be array, null, etc.)
                map.put(prefix, node.asText());
            }
        }
    }


    public static String fileNameProvider(JsonNode jsonContent){
        JsonNode conf = jsonContent.get("configurationPropertiesValues");
        if (conf == null || conf.isMissingNode()) {
            throw new IllegalArgumentException("Missing 'configurationPropertiesValues' in JSON.");
        }

        JsonNode strategyIdNode = conf.get("strategyId");
        JsonNode versionNameNode = conf.get("versionName");
        JsonNode versionNumberNode = conf.get("versionNumber");

        if (strategyIdNode == null || versionNameNode == null || versionNumberNode == null) {
            throw new IllegalArgumentException("One or more required fields are missing in 'configurationPropertiesValues' - strategyIdNode , versionName , versionNumber.");
        }

        String strategyId = strategyIdNode.asText();
        String versionName = versionNameNode.asText();
        String versionNumber = versionNumberNode.asText();

        return strategyId + "_" + versionName + "_" + versionNumber;
    }
}
