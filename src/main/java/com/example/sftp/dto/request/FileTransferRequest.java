package com.example.sftp.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FileTransferRequest {
    private String connectorId;
    private String bucketName;
    private String localDir;
    private String remoteDir;
    private List<String> fileNames;
}
