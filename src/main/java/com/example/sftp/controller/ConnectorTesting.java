package com.example.sftp.controller;


import com.example.sftp.AwsTransferFamilyListDirectories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.transfer.TransferClient;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class ConnectorTesting {

    private final AwsTransferFamilyListDirectories awsTransferFamilyListDirectories;
    @GetMapping("/connectors/list")
    public void listConnectors() {
        log.info("Listing AWS Transfer Family connectors");
        TransferClient transferClient = AwsTransferFamilyListDirectories.createTransferClient();
         AwsTransferFamilyListDirectories.listConnectors(transferClient);
    }



}
