package com.example.sftp.controller;


import com.example.sftp.AwsTransferFamilyListDirectories;
import com.example.sftp.dto.request.ConnectorUpdateRequest;
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
}
