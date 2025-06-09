package com.example.sftp.controller;


import com.example.sftp.AwsTransferFamilyListDirectories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.transfer.TransferClient;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class ConnectorTesting {
    private static final String connectorId = "c-32561b335ad048fe8";
    private static final String remotePath = "/mft";

    private final AwsTransferFamilyListDirectories awsTransferFamilyListDirectories;
    @GetMapping("/connectors/list")
    public void listConnectors() {
        log.info("Listing AWS Transfer Family connectors");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
         AwsTransferFamilyListDirectories.listConnectors(transferClient);
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
