package com.example.sftp.controller;


import com.example.sftp.AwsTransferFamilyListDirectories;
import com.example.sftp.dto.request.ConnectorUpdateRequest;
import com.example.sftp.dto.request.FileTransferRequest;
import com.example.sftp.dto.response.ConnectorDescriptionResponse;
import com.example.sftp.dto.response.ConnectorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.transfer.TransferClient;
import software.amazon.awssdk.services.transfer.model.UpdateConnectorRequest;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class ConnectorTesting {
    private static final String connectorId = "c-32561b335ad048fe8";
    private static final String remotePath = "/mft";

    private final AwsTransferFamilyListDirectories awsTransferFamilyListDirectories;
    @GetMapping("/connectors/list")
    public List<ConnectorResponse> listConnectors() {
        log.info("Listing AWS Transfer Family connectors");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.listConnectors(transferClient);
    }

    @GetMapping(value = "/connectors/description/{connection-id}",  produces = MediaType.APPLICATION_JSON_VALUE)
    public ConnectorDescriptionResponse getConnectorDescription(@PathVariable("connection-id") String connectionId) {
        log.info("Getting AWS Transfer Family connector description");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.describeConnector(transferClient, connectionId);
    }

    @PostMapping(value = "/update/connector", produces = MediaType.APPLICATION_JSON_VALUE)
    public String updateConnector(@RequestBody ConnectorUpdateRequest connectorUpdateRequest) {
        log.info("Updating AWS Transfer Family connector");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.updateConnector(transferClient, connectorUpdateRequest);
    }

    @PostMapping(value = "/inbound/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
    public String startInboundTransfer(@RequestBody FileTransferRequest fileTransferRequest){
        log.info("Starting AWS Transfer Family inbound transfer");
        TransferClient transferClient = awsTransferFamilyListDirectories.createTransferClient();
        return awsTransferFamilyListDirectories.startInboundTransfer(transferClient, fileTransferRequest);
    }
    @GetMapping("/connectors/monitor")
    public void monitorTransfer(@RequestParam(name = "transferId") String transferId) {
        log.info("Monitoring Transfer");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
        AwsTransferFamilyListDirectories.monitorTransfer(transferClient, connectorId, transferId);
    }

    @GetMapping("/connectors/startOutboundTransfer")
    public String startOutboundTransfer() {
        log.info("Starting outbound AWS Transfer Family transfer");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
        return AwsTransferFamilyListDirectories.startOutboundTransfer(transferClient, connectorId);
    }

    @GetMapping("/connectors/startDirectoryListing")
    public String startDirectoryListing() {
        log.info("Starting directory listing AWS Transfer Family transfer");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
        return AwsTransferFamilyListDirectories.startDirectoryListing(transferClient, connectorId, remotePath);
    }
}
