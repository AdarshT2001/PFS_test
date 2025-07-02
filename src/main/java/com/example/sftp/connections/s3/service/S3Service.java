package com.example.sftp.connections.s3.service;

import com.example.sftp.connections.s3.S3ObjClient;
import com.example.sftp.dto.request.DirectoryListingRequest;
import com.example.sftp.dto.request.FileTransferRequest;
import com.example.sftp.dto.request.TransferMonitorRequest;
import com.example.sftp.dto.response.DirectoryListingResponse;
import com.example.sftp.dto.response.MonitoringTransferResultsResponse;
import com.example.sftp.utils.PreProcessingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.transfer.TransferClient;
import software.amazon.awssdk.services.transfer.model.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {
    private final S3ObjClient s3ObjClient;

    public PutObjectResponse uploadFile(PutObjectRequest putObjectRequest, File tempFile) {
        return s3ObjClient.getS3Client().putObject(
                putObjectRequest,
                RequestBody.fromFile(tempFile)
        );
    }

    public InputStream downloadFile(String bucket, String key, String fileName) {

        //Add file name to the key
        String fullKey = PreProcessingUtils.constructFolderKey(key, fileName);

        //TODO: retry if the download fails
        log.info("Starting download from bucket: {}, path: {}", bucket, fullKey);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fullKey)
                .build();

        return s3ObjClient.getS3Client().getObject(request);
    }

    public String startInboundTransfer(TransferClient transferClient, FileTransferRequest fileTransferRequest) throws Exception {
        String transferId = null;
        try {
            StartFileTransferRequest request = StartFileTransferRequest.builder()
                    .connectorId(fileTransferRequest.getConnectorId())
                    .localDirectoryPath("/" + fileTransferRequest.getBucketName() + fileTransferRequest.getLocalDir())
                    .retrieveFilePaths(fileTransferRequest.getFileNames())
                    .build();
            if(testConnection(transferClient,fileTransferRequest.getConnectorId())){
                StartFileTransferResponse response = transferClient.startFileTransfer(request);
                transferId = response.transferId();
                log.info("Started Inbound transfer: {}", transferId);
            }
            return transferId;
        } catch (Exception e) {
            log.error("Failed to start inbound transfer: {}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    public static String startOutboundTransfer(TransferClient transferClient, FileTransferRequest fileTransferRequest) throws Exception {
        String transferId = null;
        try {
            StartFileTransferRequest request = StartFileTransferRequest.builder()
                    .connectorId(fileTransferRequest.getConnectorId())
                    .sendFilePaths(fileTransferRequest.getSendFilePaths())
                    .remoteDirectoryPath(fileTransferRequest.getRemoteDir())
                    .build();
            if(testConnection(transferClient,fileTransferRequest.getConnectorId())){
                StartFileTransferResponse response = transferClient.startFileTransfer(request);
                transferId = response.transferId();
                log.info("Started outbound transfer: {}", transferId);
            }
            return transferId;
        } catch (Exception e) {
            log.error("Failed to start outbound transfer: {}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    public static boolean testConnection(TransferClient transferClient, String connectorId) throws RuntimeException {
        try {
            TestConnectionRequest request = TestConnectionRequest.builder()
                    .connectorId(connectorId)
                    .build();

            TestConnectionResponse response = transferClient.testConnection(request);

            if ("OK".equals(response.status())) {
                log.info("Connection Test Succeeded for connector ID: {}", connectorId);
                return true;
            } else {
                throw new RuntimeException("Connection Test Failed for connector ID: " + connectorId);
            }
        } catch (TransferException e) {
            throw new RuntimeException("Connection test failed due to AWS Transfer exception: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Connection Test failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    public static DirectoryListingResponse startDirectoryListing(TransferClient transferClient, DirectoryListingRequest directoryListingRequest) {
        try {
            StartDirectoryListingRequest request = StartDirectoryListingRequest.builder()
                    .connectorId(directoryListingRequest.getConnectorId())
                    .outputDirectoryPath(directoryListingRequest.getOutputDirectoryPath())  // Local path to store the listing results "/test-pfs"
                    .remoteDirectoryPath(directoryListingRequest.getRemoteDirectoryPath())  // Path in the remote SFTP server where files are fetched from
                    .maxItems(directoryListingRequest.getMaxItems())
                    .build();
            log.info("Starting directory listing: {}", directoryListingRequest.getRemoteDirectoryPath());
            StartDirectoryListingResponse response = transferClient.startDirectoryListing(request);

            return DirectoryListingResponse.builder().ListingId(response.listingId()).outputFileName(response.outputFileName()).build();
        } catch (Exception e) {
            log.error("Error starting directory listing: {}", e.getMessage());
            return null;
        }
    }

    public static List<MonitoringTransferResultsResponse> monitorTransfer(TransferClient transferClient, TransferMonitorRequest transferMonitorRequest) {
        try {
            ListFileTransferResultsRequest request = ListFileTransferResultsRequest.builder()
                    .connectorId(transferMonitorRequest.getConnectorId())
                    .transferId(transferMonitorRequest.getTransferId())
                    .maxResults(transferMonitorRequest.getMaxRecords())
                    .build();

            List<MonitoringTransferResultsResponse> results = new ArrayList<>();
            ListFileTransferResultsResponse response = transferClient.listFileTransferResults(request);
            response.fileTransferResults().forEach(result -> {
                val resp = MonitoringTransferResultsResponse.builder()
                        .filePath(result.filePath())
                        .statusCode(result.statusCode())
                        .failureCode(result.failureCode())
                        .failureMessage(result.failureMessage())
                        .build();
                results.add(resp);
                log.info("File: {}, Status: {}", result.filePath(), result.statusCode());
            });
            return results;
        } catch (Exception e) {
            log.error("Failed to monitor transfer: {}", e.getMessage());
        }
        return Collections.emptyList();
    }


}
