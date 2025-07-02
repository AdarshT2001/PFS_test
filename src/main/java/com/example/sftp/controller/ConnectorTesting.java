package com.example.sftp.controller;


import com.example.sftp.dto.request.ConnectorUpdateRequest;
import com.example.sftp.dto.request.DirectoryListingRequest;
import com.example.sftp.dto.request.FileTransferRequest;
import com.example.sftp.dto.request.TransferMonitorRequest;
import com.example.sftp.dto.response.ConnectorDescriptionResponse;
import com.example.sftp.dto.response.ConnectorResponse;
import com.example.sftp.dto.response.DirectoryListingResponse;
import com.example.sftp.dto.response.MonitoringTransferResultsResponse;
import com.example.sftp.service.AwsTransferFamilyListDirectories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.transfer.TransferClient;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class ConnectorTesting {
    private static final String connectorId = "c-32561b335ad048fe8";

    private final AwsTransferFamilyListDirectories awsTransferFamilyListDirectories;

    /*
     Sample response:
     [
	{
		"arn": "arn:aws:transfer:us-east-1:072389562270:connector/c-32561b335ad048fe8",
		"connectorId": "c-32561b335ad048fe8",
		"url": "sftp://34.226.222.129"
	}
     ]
     Response time: 2.25sec
    */
    @GetMapping("/connectors/list")
    public List<ConnectorResponse> listConnectors() {
        log.info("Listing AWS Transfer Family connectors");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.listConnectors(transferClient);
    }


    /*
    sample request: pass connectionId in path parameter
    sample response:
    {
        "arn": "arn:aws:transfer:us-east-1:072389562270:connector/c-32561b335ad048fe8",
        "connectorId": "c-32561b335ad048fe8",
        "url": "sftp://34.226.222.129",
        "accessRole": "arn:aws:iam::072389562270:role/transfer-s3-role",
        "loggingRole": "arn:aws:iam::072389562270:role/transfer-s3-role",
        "tags": {
            "Name": "wps_external_sftp_server"
        },
        "sftpConfig": {
            "trustedHostKeys": [
                "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCplbhLwUamb4u+qkVDlLJ8VoKrUixdIQKRbF3UfPfEuOKrIugLb14xeLRmX8CEeo/Srf6PmP8o+3/sph2C/QF27ka8trmPMyro5UOUrci09mjeZf6YO0ICIAYrzGunAqmZUSL3Z/40xJjenDkvBwPNLr17bXHsrH3F2l0uCBbYYkHOIzUD4ge7PgGXMCVnT2WThj4Ho3SJyRYIRbwwvFcNBIPZAP3ood9qQmRRM3KB08E2MOtH3fRDVzyvqu4WRGiMw+GzA92u+xMCjxpzIyINA0Tx3AM8crzuyBMRmvHr1kZRFz/Q5cF7fuCkfIlMWFGutkmIK3Bz7pIBs6lJLHUbBXM2WaPWowBr9IOdyr9JvxXCkR+sX4otzu3ENaiot5uAi0kS8D3hYnS7O35g46hBtazCU0+0yBUTVVxxfAAfgzLelWTboL+Bz1XR8I4umaiTtekyLJ1s55T3dPzrlCCph/UPsS7215954QimnP4DVSXEcgNQTjib7EIWxbUlKBU="
            ],
            "userSecretId": "arn:aws:secretsmanager:us-east-1:072389562270:secret:wps_external_sftp_server_user-CkuJ6e",
            "maxConcurrentConnections": 1
        },
        "serviceManagedEgressIpAddresses": "[3.211.82.34, 54.198.172.21, 54.82.183.181]",
        "securityPolicyName": "TransferSFTPConnectorSecurityPolicy-2024-03"
    }
    Response time: 2.27sec
    */
    @GetMapping(value = "/connectors/description/{connectorId}",  produces = MediaType.APPLICATION_JSON_VALUE)
    public ConnectorDescriptionResponse getConnectorDescription(@PathVariable("connectorId") String connectorId) {
        log.info("Getting AWS Transfer Family connector description");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.describeConnector(transferClient, connectorId);
    }

    /*
    sample request:
    {
        "connectorId": "c-32561b335ad048fe8",
        "sftpConfig": {
            "trustedHostKeys": [
                ""
            ],
            "userSecretId": "arn:aws:secretsmanager:us-east-1:072389562270:secret:wps_external_sftp_server_user-CkuJ6e",
            "maxConcurrentConnections": 3
        },
        "securityPolicyName": "TransferSFTPConnectorSecurityPolicy-2024-03"
    }
    sample response:
    return the connectorId of the updated connector
    Response time: 1.65sec
    */
    @PostMapping(value = "/update/connector", produces = MediaType.APPLICATION_JSON_VALUE)
    public String updateConnector(@RequestBody ConnectorUpdateRequest connectorUpdateRequest) {
        log.info("Updating AWS Transfer Family connector");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.updateConnector(transferClient, connectorUpdateRequest);
    }

    /*
    sample request:
    {
        "connectorId": "c-32561b335ad048fe8",
        "bucketName": "wps-dev-validations-v1",
        "localDir":"/test-pfs",
        "fileNames": ["/mft/Output_logs.txt"]
    }
    sample response:
    return the transferId of the started transfer
    response time: 2sec
   */
    @PostMapping(value = "/inbound/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
    public String startInboundTransfer(@RequestBody FileTransferRequest fileTransferRequest) throws Exception {
        log.info("Starting AWS Transfer Family inbound transfer");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.startInboundTransfer(transferClient, fileTransferRequest);
    }

    /*
    sample response:
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
    Response time: 1.8sec
     */
    @PostMapping("/connectors/monitor")
    public List<MonitoringTransferResultsResponse> monitorTransfer(@RequestBody TransferMonitorRequest transferMonitorRequest) {
        log.info("Monitoring Transfer");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
        return AwsTransferFamilyListDirectories.monitorTransfer(transferClient, transferMonitorRequest);
    }

    /*
    sample request:
    {
        "connectorId": "c-32561b335ad048fe8",
        "bucketName": "wps-dev-validations-v1",
        "remote-dir":"/sftpfiles",
        "sendFilePaths": ["/test-pfs/Output_logs.txt"]
    }

    * sample response:
    * returns transferId
    * response time: 2.57sec*/

    @PostMapping(value = "/outbound/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
    public String startOutboundTransfer(@RequestBody FileTransferRequest fileTransferRequest) throws Exception {
        log.info("Starting AWS Transfer Family outbound transfer");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.startOutboundTransfer(transferClient, fileTransferRequest);
    }
    /*
    Sample request:
            {
            "connectorId": "c-32561b335ad048fe8",
            "remoteDirectoryPath": "/sftpfiles",
            "maxItems": 10,
            "outputDirectoryPath": "/wps-dev-validations-v1/test"

        }
     sample response:
     returns the ListingId
     response time: 4.78sec
    */

    /*
     sample request:
     {
        "connectorId": "c-32561b335ad048fe8",
        "bucketName": "wps-dev-validations-v1",
        "remoteDirectoryPath": "/sftpfiles/outbound_test",
        "outputDirectoryPath": "/wps-dev-validations-v1/test-pfs",
            "maxItems": 1
      }

      sample response:
      {
        "outputFileName": "c-32561b335ad048fe8-67426196-d2af-44c8-83ac-6f69457dd34a.json",
        "listingId": "67426196-d2af-44c8-83ac-6f69457dd34a"
      }
     */
    @PostMapping("/connectors/startDirectoryListing")
    public DirectoryListingResponse startDirectoryListing(@RequestBody DirectoryListingRequest directoryListingRequest) {
        log.info("Starting directory listing AWS Transfer Family transfer");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
        return AwsTransferFamilyListDirectories.startDirectoryListing(transferClient,directoryListingRequest);
    }
}
