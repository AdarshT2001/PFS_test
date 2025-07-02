package com.example.sftp.service;

import com.example.sftp.dto.request.ConnectorUpdateRequest;
import com.example.sftp.dto.request.DirectoryListingRequest;
import com.example.sftp.dto.request.FileTransferRequest;
import com.example.sftp.dto.request.TransferMonitorRequest;
import com.example.sftp.dto.response.ConnectorDescriptionResponse;
import com.example.sftp.dto.response.ConnectorResponse;
import com.example.sftp.dto.response.DirectoryListingResponse;
import com.example.sftp.dto.response.MonitoringTransferResultsResponse;
import lombok.val;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.transfer.TransferClient;
import software.amazon.awssdk.services.transfer.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.transfer.model.Tag;


import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AwsTransferFamilyListDirectories {

    private static final String ENDPOINT_URL = "http://localhost:4566"; // LocalStack
    private static final String REGION = "us-east-1";
    private static final String SFTP_HOST = "localhost";
    private static final int SFTP_PORT = 2222;
    private static final String SFTP_USERNAME = "testuser1";
    private static final String SFTP_PASSWORD = "password1";
    private static final String BUCKET_NAME = "wps-dev-validations-v1";
    private static final String LOCAL_DIR = "/test-pfs/test_may_26.pub";
    private static final String REMOTE_DIR = "/sftpfiles";
    private static final String ACCESS_ROLE_ARN = "arn:aws:iam::000000000000:role/localstack-role";
    private static final String TEST_SECRET_ID = "";
    private static final String TEST_ACC_KEY = "";
    private static final String TEST_SECRET_KEY = "";

//    public static void main(String[] args) {
//        // Initialize AWS clients
//        TransferClient transferClient = createTransferClient();
//        S3Client s3Client = createS3Client();
//        SecretsManagerClient secretsManagerClient = createSecretsManagerClient();
//
//        try {
//            // Create S3 bucket
////            createS3Bucket(s3Client);
////
//            // Create secret for SFTP credentials
////            String secretArn = createSecret(secretsManagerClient);
//
//            // Create SFTP connector
////            String connectorId = createConnector(transferClient, secretArn);
//            String connectorId = "c-32561b335ad048fe8";
//
//            // List all connectors
//            listConnectors(transferClient);
//
//            // Describe connector
//            describeConnector(transferClient, connectorId);
//
//            //list directories in the SFTP server
//            String res = startDirectoryListing(transferClient, connectorId, REMOTE_DIR);
//            System.out.println("Directory listing started with ID: " + res);
//
//            // Update connector
////            updateConnector(transferClient, connectorId);
//
//            // Start inbound transfer (SFTP to S3)
//            String inboundTransferId = startInboundTransfer(transferClient, connectorId);
////            monitorTransfer(transferClient, connectorId, inboundTransferId);
//
//            // Start outbound transfer (S3 to SFTP)
//            String outboundTransferId = startOutboundTransfer(transferClient, connectorId);
////            monitorTransfer(transferClient, connectorId, outboundTransferId);
//
//            // Delete connector
////            deleteConnector(transferClient, connectorId);
////
//        } catch (Exception e) {
//            System.err.println("Error: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            transferClient.close();
//            s3Client.close();
//            secretsManagerClient.close();
//        }
//    }

    /*
            Purpose: Initializes the TransferClient using STS (Security Token Service) credentials. This is needed to perform operations in AWS Transfer Family, like creating connectors or starting file transfers.
            Request: None (uses AWS credentials and assumes a role).
            Response: A TransferClient instance that can be used to interact with the AWS Transfer Family service.
            Sample Request: AWS credentials (TEST_ACC_KEY and TEST_SECRET_KEY), along with the IAM Role ARN to assume a role.
            Sample Response: A TransferClient instance configured with the provided credentials.
            Limitations:
                This is synchronous by default.
                Assumes the IAM role for STS access, so your IAM policy must allow this role assumption.
                Ensure you have the appropriate permissions for the specified Transfer Family resources.
            Sync/Async: Synchronous (blocking call).
     */

    public static TransferClient createTransferClient() {
        try {
            StsClient stsClient = StsClient.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(TEST_ACC_KEY, TEST_SECRET_KEY)))
                    .build();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn("arn:aws:iam::072389562270:role/WPS-dev-role") // Replace with your IAM Role ARN
                    .roleSessionName("WPS-dev-role")
                    .build();

            Supplier<AssumeRoleRequest> assumeRoleRequestSupplier = () -> assumeRoleRequest;

            StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequestSupplier)// Use the STS client
                    .build();


            return TransferClient.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(stsAssumeRoleCredentialsProvider)
//                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(TEST_ACC_KEY, TEST_SECRET_KEY)))
//                    .endpointOverride(URI.create(ENDPOINT_URL))
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to create TransferClient: " + e.getMessage());
            throw new RuntimeException("TransferClient creation failed", e);
        }
    }

    /*
        Purpose: Initializes the S3Client using assumed STS credentials, enabling you to interact with Amazon S3.
        AWS S3 will be used for storing files involved in the SFTP file transfer process.
        Request: None (uses AWS credentials and assumes a role).
        Response: A S3Client instance that you can use to interact with the S3 service.
        Sample Request: AWS credentials and IAM Role ARN to assume a role.
        Sample Response: A S3Client instance configured with the appropriate credentials.
        Limitations:
            S3Client interacts with Amazon S3 only.
            STS credentials might expire, requiring you to refresh them periodically.
            Access permissions to the S3 bucket should be configured.
        Sync/Async: Synchronous (blocking call).
     */

    private static S3Client createS3Client() {
        try {

            StsClient stsClient = StsClient.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(TEST_ACC_KEY, TEST_SECRET_KEY)))
                    .build();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn("arn:aws:iam::072389562270:role/WPS-dev-role") // Replace with your IAM Role ARN
                    .roleSessionName("WPS-dev-role")
                    .build();


            Supplier<AssumeRoleRequest> assumeRoleRequestSupplier = () -> assumeRoleRequest;


            StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequestSupplier)// Use the STS client
                    .build();

            return S3Client.builder()
                    .region(Region.of(REGION))
//                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .credentialsProvider(stsAssumeRoleCredentialsProvider)
//                    .endpointOverride(URI.create(ENDPOINT_URL))
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to create S3Client: " + e.getMessage());
            throw new RuntimeException("S3Client creation failed", e);
        }
    }

    /*
        Purpose: Initializes the SecretsManagerClient using STS credentials to manage secrets (such as SFTP credentials). This service is used to securely store the SFTP credentials.
        Request: None (uses AWS credentials and assumes a role).
        Response: A SecretsManagerClient instance.
        Sample Request: AWS credentials and IAM Role ARN to assume a role.
        Sample Response: A SecretsManagerClient instance.
        Limitations:
            Needs to be integrated with Secrets Manager to store and retrieve secrets.
            Be mindful of Secret rotation and expiration.
        Sync/Async: Synchronous (blocking call).
     */

    private static SecretsManagerClient createSecretsManagerClient() {
        try {
            StsClient stsClient = StsClient.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(TEST_ACC_KEY, TEST_SECRET_KEY)))
                    .build();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn("arn:aws:iam::192868178773:role/WPS-dev-role") // Replace with your IAM Role ARN
                    .roleSessionName("WPS-dev-role")
                    .build();

            Supplier<AssumeRoleRequest> assumeRoleRequestSupplier = () -> assumeRoleRequest;


            StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequestSupplier)// Use the STS client
                    .build();


            return SecretsManagerClient.builder()
                    .region(Region.of(REGION))
//                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .credentialsProvider(stsAssumeRoleCredentialsProvider)
//                    .endpointOverride(URI.create(ENDPOINT_URL))
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to create SecretsManagerClient: " + e.getMessage());
            throw new RuntimeException("SecretsManagerClient creation failed", e);
        }
    }

    /*
        Purpose: Creates an S3 bucket to store files used in the SFTP file transfer.
        Request: S3 bucket name.
        Sample Request: aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket
        Sample Response: Created S3 bucket: test-bucket
        Limitations:
            LocalStack is used here (endpoint-url=http://localhost:4566).
            Bucket name must be globally unique.
            This command will fail if the bucket already exists or if the LocalStack service is not running.
        Sync/Async: Synchronous (blocking call).
     */

    private static void createS3Bucket(S3Client s3Client) {
        try {
            String command = String.format("aws --endpoint-url=%s s3 mb s3://%s", ENDPOINT_URL, BUCKET_NAME);
            Runtime.getRuntime().exec(command).waitFor();
            System.out.println("Created S3 bucket: " + BUCKET_NAME);
        } catch (Exception e) {
            System.err.println("Failed to create S3 bucket: " + e.getMessage());
        }
    }

    /*
        Purpose: Creates a secret in Secrets Manager to store SFTP credentials.
        Request: The secret name and secret string (e.g., SFTP username and password).
        Sample Request:
                Map<String, String> credentials = Map.of("username", "testuser1", "password", "password1");
                CreateSecretRequest request = CreateSecretRequest.builder()
                        .name("sftp-credentials-1")
                        .secretString(credentials.toString())
                        .description("SFTP credentials")
                        .build();
        Sample Response: Created secret: arn:aws:secretsmanager:us-east-1:123456789012:secret:sftp-credentials-1-123456
        Limitations:
                Secret values must be properly encoded (e.g., JSON or string format).
                AWS Secrets Manager has limits on the size of secrets (typically 64 KB).
        Sync/Async: Synchronous (blocking call).
     */

    private static String createSecret(SecretsManagerClient secretsManagerClient) {
        try {
            Map<String, String> credentials = Map.of("username", SFTP_USERNAME, "password", SFTP_PASSWORD);
            CreateSecretRequest request = CreateSecretRequest.builder()
                    .name(TEST_SECRET_ID)
                    .secretString(credentials.toString())
                    .description("SFTP credentials")
                    .build();
            CreateSecretResponse response = secretsManagerClient.createSecret(request);
            System.out.println("Created secret: " + response.arn());
            return response.arn();
        } catch (Exception e) {
            System.err.println("Failed to create secret: " + e.getMessage());
            throw new RuntimeException("Secret creation failed", e);
        }
    }
    /*
        Purpose: Creates an SFTP connector in AWS Transfer Family, which establishes the connection details for the SFTP server.
        Request: SFTP host, port, and credentials (stored in Secrets Manager).
        Sample Request:
        SftpConnectorConfig sftpConfig = SftpConnectorConfig.builder()
                .maxConcurrentConnections(5)
                .userSecretId("sftp-credentials-1") // Secret ID for credentials
                .build();

        CreateConnectorRequest request = CreateConnectorRequest.builder()
                .accessRole("arn:aws:iam::123456789012:role/TransferRole")
                .url("sftp://localhost:2222")
                .sftpConfig(sftpConfig)
                .securityPolicyName("TransferSecurityPolicy-2022-10")
                .build();
        Sample Response: Created connector: c-32561b335ad048fe8
        Limitations:
                Ensure that the SFTP server is reachable from the AWS Transfer Family service.
                The connector must be configured properly with correct IAM roles and policies.
                The role used must have the permissions to interact with the SFTP server and store/retrieve files from S3.
        Sync/Async: Synchronous (blocking call).
     */

    private static String createConnector(TransferClient transferClient, String secretArn) {
        try {
            SftpConnectorConfig sftpConfig = SftpConnectorConfig.builder()
                    .maxConcurrentConnections(5)
                    .userSecretId(TEST_SECRET_ID)
                    .build();
            CreateConnectorRequest request = CreateConnectorRequest.builder()
                    .accessRole(ACCESS_ROLE_ARN)
                    .url("sftp://" + SFTP_HOST + ":" + SFTP_PORT)
                    .sftpConfig(sftpConfig)
                    .securityPolicyName("TransferSecurityPolicy-2022-10")
                    .tags(Tag.builder().key("Name").value("TestConnector").build())
                    .build();
            CreateConnectorResponse response = transferClient.createConnector(request);
            String connectorId = response.connectorId();
            System.out.println("Created connector: " + connectorId);
            return connectorId;
        } catch (Exception e) {
            System.err.println("Failed to create connector: " + e.getMessage());
            throw new RuntimeException("Connector creation failed", e);
        }
    }

    /*
        Purpose: Lists all connectors created in AWS Transfer Family.
        Request: None, just a call to list connectors.
        Sample Request:
                ListConnectorsRequest request = ListConnectorsRequest.builder().maxResults(10).build();
                ListConnectorsResponse response = transferClient.listConnectors(request);
        Sample Response: Connector ID: c-32561b335ad048fe8, ARN: arn:aws:transfer:us-east-1:123456789012:connector/c-32561b335ad048fe8
        [
            {
                "arn": "",
                "connectorId": "",
                "url": ""
            }
        ]
        Limitations:
                Pagination: You may need to handle pagination if there are more connectors than the maximum number (maxResults).
        Sync/Async: Synchronous (blocking call).
     */

    public List<ConnectorResponse> listConnectors(TransferClient transferClient) {
        try {
            ListConnectorsRequest request = ListConnectorsRequest.builder().maxResults(10).build();
            ListConnectorsResponse response = transferClient.listConnectors(request);
            return response.connectors().stream().map(connector ->
                    ConnectorResponse.builder()
                            .connectorId(connector.connectorId())
                            .arn(connector.arn())
                            .url(connector.url())
                            .build()
            ).toList();
        } catch (Exception e) {
            System.err.println("Failed to list connectors: " + e.getMessage());
            return null;
        }
    }

    /*
        Purpose: Retrieves detailed information about a specific connector.
        Request: Connector ID.
        Sample Request: Retrieves connector-id from path variable
                DescribeConnectorRequest request = DescribeConnectorRequest.builder()
                        .connectorId("c-32561b335ad048fe8")
                        .build();
        Sample Response:
        {
            "arn": "",
            "connectorId": "",
            "url": "",
            "accessRole": "",
            "loggingRole": "",
            "tags": {
                "Name": "wps_external_sftp_server"
            },
            "sftpConfig": {
                "trustedHostKeys": [
                    ""
                ],
                "userSecretId": "",
                "maxConcurrentConnections": 1
            },
            "serviceManagedEgressIpAddresses": "[]",
            "securityPolicyName": ""
        }
        Limitations:
                Connector details depend on its configuration, and the response may not contain data if the connector is misconfigured.
        Sync/Async: Synchronous (blocking call).
     */

    public ConnectorDescriptionResponse describeConnector(TransferClient transferClient, String connectorId) {
        try {
            DescribeConnectorRequest request = DescribeConnectorRequest.builder()
                    .connectorId(connectorId)
                    .build();
            DescribeConnectorResponse response = transferClient.describeConnector(request);
            DescribedConnector connector = response.connector();
            Map<String, Object> sftpConfigMap = convertSftpConfigToMap(connector.sftpConfig());

            Map<String, String> tagsMap = connector.tags().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value));
            System.out.println("Connector Details: " + connector);
            return ConnectorDescriptionResponse.builder()
                    .arn(connector.arn())
                    .connectorId(connector.connectorId())
                    .url(connector.url())
                    .accessRole(connector.accessRole())
                    .loggingRole(connector.loggingRole())
                    .sftpConfig(sftpConfigMap)
                    .securityPolicyName(connector.securityPolicyName())
                    .serviceManagedEgressIpAddresses(Arrays.toString(connector.serviceManagedEgressIpAddresses().toArray(new String[0])))
                    .tags(tagsMap)
                    .build();

        } catch (Exception e) {
            System.err.println("Failed to describe connector: " + e.getMessage());
            return null;
        }
    }

    /*
        Purpose: Updates an existing Transfer Family connector's configuration.
        Request: Connector ID and the updated configurations (e.g., increasing maxConcurrentConnections).
        Sample Request:
        {
            "connectorId": "",
            "sftpConfig": {
                "trustedHostKeys": [
                    ""
                ],
                "userSecretId": "",
                "maxConcurrentConnections": 3
            },
            "securityPolicyName": ""
        }
                UpdateConnectorRequest request = UpdateConnectorRequest.builder()
                        .connectorId("c-32561b335ad048fe8")
                        .securityPolicyName("TransferSecurityPolicy-2022-10")
                        .sftpConfig(SftpConnectorConfig.builder()
                                .userSecretId("sftp-credentials-1")
                                .maxConcurrentConnections(3)
                                .build())
                        .build();
        Sample Response: Updated connector: c-32561b335ad048fe8
        Limitations: You can only update certain fields (e.g., maxConcurrentConnections).
        Sync/Async: Synchronous (blocking call).
     */

    public String updateConnector(TransferClient transferClient, ConnectorUpdateRequest connectorUpdateRequest) {
        try {
            UpdateConnectorRequest request = UpdateConnectorRequest.builder()
                    .connectorId(connectorUpdateRequest.getConnectorId())
                    .securityPolicyName(connectorUpdateRequest.getSecurityPolicyName())
                    .sftpConfig(SftpConnectorConfig.builder()
                            .userSecretId(connectorUpdateRequest.getUserSecretId())
                            .maxConcurrentConnections(connectorUpdateRequest.getMaxConcurrentConnections())
                            .build())
                    .build();
            UpdateConnectorResponse response = transferClient.updateConnector(request);
            System.out.println("Updated connector: " + response.connectorId());
            return response.connectorId();

        } catch (Exception e) {
            System.err.println("Failed to update connector: " + e.getMessage());
            return null;
        }
    }
    /*
        startInboundTransfer & startOutboundTransfer
        Purpose: Starts an SFTP file transfer from SFTP to S3 (inbound transfer) or from S3 to SFTP (outbound transfer).
        Request: File paths to be transferred and the connector ID.
        Sample Request:
        {
            "connectorId": "c-32561b335ad048fe8",
            "bucketName": "wps-dev-validations-v1",
            "localDir":"/test-pfs",
            "fileNames": ["/mft/Output_logs.txt"]
        }
                StartFileTransferRequest request = StartFileTransferRequest.builder()
                        .connectorId("c-32561b335ad048fe8")
                        .localDirectoryPath("s3://test-bucket/destination/")
                        .remoteDirectoryPath("/upload/")
                        .sendFilePaths(List.of("file1.csv"))
                        .build();
        Sample Response: Started outbound transfer: transfer-id-123456
        Limitations:
                Transfers can be interrupted if the connector or SFTP server has issues.
        Sync/Async: Asynchronous. (Use startFileTransfer and check the transfer status separately).
     */

    public String startInboundTransfer(TransferClient transferClient, FileTransferRequest fileTransferRequest) throws Exception {
        String transferId = null;
        try {
            StartFileTransferRequest request = StartFileTransferRequest.builder()
                    .connectorId(fileTransferRequest.getConnectorId())
                    .localDirectoryPath("/" + fileTransferRequest.getBucketName() + fileTransferRequest.getLocalDir())
//                    .remoteDirectoryPath(fileTransferRequest.getRemoteDir())
                    .retrieveFilePaths(fileTransferRequest.getFileNames())
                    .build();
            if(testConnection(transferClient,fileTransferRequest.getConnectorId())){
                StartFileTransferResponse response = transferClient.startFileTransfer(request);
                transferId = response.transferId();
                System.out.println("Started inbound transfer: " + transferId);

            }
            return transferId;
        } catch (Exception e) {
            System.err.println("Failed to start inbound transfer: " + e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    /*
        startOutboundTransfer
        Purpose: Starts an SFTP file transfer from S3 to SFTP (outbound transfer).
        Request: File paths to be transferred and the connector ID.
        Sample Request:
                StartFileTransferRequest request = StartFileTransferRequest.builder()
                        .connectorId("c-32561b335ad048fe8")
                        .remoteDirectoryPath("/upload/")
                        .sendFilePaths(List.of("file1.csv"))
                        .build();
                Request JSON:
                {
                    "connectorId": "c-32561b335ad048fe8",
                    "bucketName": "wps-dev-validations-v1",
                    "remote-dir":"/sftpfiles",
                    "sendFilePaths": ["/test-pfs/Output_logs.txt"]
                }
        Sample Response: Started outbound transfer: transfer-id-123456
            {
               "TransferId": "string"
            }
        Limitations:
                Transfers can be interrupted if the connector or SFTP server has issues.
        Response time: approximately 3s for one file
        Sync/Async: Asynchronous. (Use startFileTransfer and check the transfer status separately).
     */
    public static String startOutboundTransfer(TransferClient transferClient,FileTransferRequest fileTransferRequest) throws Exception {
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
                System.out.println("Started outbound transfer: " + transferId);
            }
            return transferId;
        } catch (Exception e) {
            System.err.println("Failed to start outbound transfer: " + e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    /*
        Purpose: Monitors the status of an ongoing transfer.
        Request: Connector ID and transfer ID.
        Sample Request:
                ListFileTransferResultsRequest request = ListFileTransferResultsRequest.builder()
                        .connectorId("c-32561b335ad048fe8")
                        .transferId("transfer-id-123456")
                        .build();
        Sample Response: File: /upload/file1.csv, Status: SUCCESS
            {
               "FileTransferResults": [
                  {
                     "FailureCode": "string",
                     "FailureMessage": "string",
                     "FilePath": "string",
                     "StatusCode": "string"
                  }
               ],
               "NextToken": "string"
            }
        Limitations:
                Transfer results might not be immediately available.
        Response time: approximately 3s
        Sync/Async: Synchronous (blocking call).
     */

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
                System.out.println("File: " + result.filePath() + ", Status: " + result.statusCode());
                System.out.println("Failure code: " + result.failureCode() + ", Failure message: " + result.failureMessage());
            });
            return results;
        } catch (Exception e) {
            System.err.println("Failed to monitor transfer: " + e.getMessage());
            System.out.println("Mock transfer status: COMPLETED for transfer ID " + transferMonitorRequest.getTransferId());
        }
        return null;
    }
    /*
        Purpose: Deletes an existing connector.
        Request: Connector ID.
        Sample Request:
                DeleteConnectorRequest request = DeleteConnectorRequest.builder()
                        .connectorId("c-32561b335ad048fe8")
                        .build();
        Sample Response: Deleted connector: c-32561b335ad048fe8
            If the action is successful, the service sends back an HTTP 200 response with an empty HTTP body.
        Limitations: Deleting a connector may affect ongoing or future file transfers.
        Sync/Async: Synchronous (blocking call).
     */

    private static void deleteConnector(TransferClient transferClient, String connectorId) {
        try {
            DeleteConnectorRequest request = DeleteConnectorRequest.builder()
                    .connectorId(connectorId)
                    .build();
            transferClient.deleteConnector(request);
            System.out.println("Deleted connector: " + connectorId);
        } catch (Exception e) {
            System.err.println("Failed to delete connector: " + e.getMessage());
        }
    }

    /*
        Purpose: Retrieves a list of the contents of a directory from a remote SFTP server.
        Request: Connector ID and Remote Path of SFTP server.
        Sample Request:
                StartDirectoryListingRequest request = StartDirectoryListingRequest.builder()
                    .connectorId(connectorId)
                    .outputDirectoryPath("/local-directory-path")
                    .remoteDirectoryPath(remotePath)
                    .build();
        Sample Response: {
                            "ListingId": "",
                            "OutputFileName": ""
                        }
        Response time: approximately 3s
        Default maxItems: 1000
        Sync/Async: Asynchronous (non-blocking call).
     */
    public static DirectoryListingResponse startDirectoryListing(TransferClient transferClient, DirectoryListingRequest directoryListingRequest) {
        try {
            StartDirectoryListingRequest request = StartDirectoryListingRequest.builder()
                    .connectorId(directoryListingRequest.getConnectorId())
                    .outputDirectoryPath(directoryListingRequest.getOutputDirectoryPath())  // Local path to store the listing results "/test-pfs"
                    .remoteDirectoryPath(directoryListingRequest.getRemoteDirectoryPath())  // Path in the remote SFTP server where files are fetched from
                    .maxItems(directoryListingRequest.getMaxItems())
                    .build();

            // Start directory listing
            StartDirectoryListingResponse response = transferClient.startDirectoryListing(request);
                try {
                    String fileKey = "test-pfs-10/" + response.outputFileName();  // This is the key for the file in the S3 bucket.
                    S3Client s3Client = createS3Client();
                    // Download the file to the specified local path
                    InputStream inputStream = s3Client.getObject(GetObjectRequest.builder().bucket(BUCKET_NAME).key(fileKey).build());
                    downloadFile(inputStream, "/Users/adarshteeparthi/Downloads/temp.json");  // Download the file to the specified local path
                    // If no exception is thrown, the file exists
                    System.out.println("Response file exists in S3: " + fileKey);
                } catch (NoSuchKeyException e) {
                    System.out.println("Response file does not exist in S3 yet: " + response.outputFileName());
                } catch (Exception e) {
                    System.err.println("Error checking if response file exists in S3: " + e.getMessage());
                }
            // Return the listing ID to track the status
            return DirectoryListingResponse.builder().ListingId(response.listingId()).outputFileName(response.outputFileName()).build();
        } catch (Exception e) {
            System.err.println("Error starting directory listing: " + e.getMessage());
            return null;
        }
    }
    public static void downloadFile(InputStream inputStream, String outputFilePath) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(outputFilePath)) {
            // Buffer for reading and writing
            byte[] buffer = new byte[4096]; // 4 KB buffer, you can adjust the size as needed
            int bytesRead;

            // Read from the InputStream and write to the OutputStream (local file)
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Close the InputStream if it was provided
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static Map<String, Object> convertSftpConfigToMap(SftpConnectorConfig sftpConfig) {
        if (sftpConfig == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("userSecretId", sftpConfig.userSecretId());
        map.put("trustedHostKeys", sftpConfig.trustedHostKeys());
        map.put("maxConcurrentConnections", sftpConfig.maxConcurrentConnections());
        // Add other fields from SftpConnectorConfig as necessary

        return map;
    }

    /*
    * Purpose: Used to check connection between sftp server and s3
    * Parameters: Connector ID and TransferClient.
    */
    public static boolean testConnection(TransferClient transferClient, String connectorId) throws RuntimeException {
        try {
            // Create a request to test the connection
            TestConnectionRequest request = TestConnectionRequest.builder()
                    .connectorId(connectorId)  // Provide your connector ID
                    .build();

            // Call the TestConnection API
            TestConnectionResponse response = transferClient.testConnection(request);

            // If the connection is successful, check the response status
            if ("OK".equals(response.status())) {
                System.out.println("Connection Test Succeeded for connector ID: " + connectorId);
                return true; // Return true if connection is successful
            } else {
                // If the connection fails, handle the error gracefully
                throw new RuntimeException("Connection Test Failed for connector ID: " + connectorId);
            }

        } catch (TransferException e) {
            // Handle the TransferException that is thrown by the AWS SDK if the connection test fails
            throw new RuntimeException("Connection test failed due to AWS Transfer exception: " + e.getMessage(), e);
        } catch (Exception e) {
            // Catch any other unexpected errors and wrap them in a RuntimeException
            throw new RuntimeException("Connection Test failed due to unexpected error: " + e.getMessage(), e);
        }
    }
}
