package com.example.sftp.service;

import com.example.sftp.connections.s3.service.S3Service;
import com.example.sftp.utils.PreProcessingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class AWSTransferService {
    private final S3Service s3Service;

    public JsonNode parseFilePathsFromJSON(String bucketName, String key, String filename) {
        log.info("Starting to parse JSON file from S3: bucket={}, key={}, filename={}", bucketName, key, filename);

        try (InputStream inputStream = s3Service.downloadFile(bucketName, key, filename)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to download file from S3: " + filename);
            }

            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return PreProcessingUtils.parseJsonContent(content, filename);

        } catch (IOException e) {
            log.error("Error reading file from S3: {}", filename, e);
            throw new RuntimeException("Error reading file from S3: " + filename, e);
        } catch (Exception e) {
            log.error("Unexpected error while parsing JSON file: {}", filename, e);
            throw new RuntimeException("Unexpected error while parsing JSON file: " + filename, e);
        }
    }
}
