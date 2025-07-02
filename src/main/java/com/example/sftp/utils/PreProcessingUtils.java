package com.example.sftp.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@UtilityClass
@Slf4j
public class PreProcessingUtils {
    public static String constructFolderKey(String folderPath, String fileName) {
        if (folderPath == null || folderPath.isEmpty()) {
            return fileName;
        }
        if (folderPath.endsWith("/")) {
            return folderPath + fileName;
        } else {
            return folderPath + "/" + fileName;
        }
    }

    public static JsonNode parseJsonContent(String content, String filename) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(content);
            log.info("Successfully parsed JSON file: {}", filename);
            return jsonNode;
        } catch (IOException e) {
            log.error("File is not valid JSON: {}", filename, e);
            throw new IllegalArgumentException("File is not valid JSON: " + filename, e);
        }
    }
}
