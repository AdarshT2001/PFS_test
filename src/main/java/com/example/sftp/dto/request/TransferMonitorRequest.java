package com.example.sftp.dto.request;

import lombok.Data;

@Data
public class TransferMonitorRequest {
    private String connectorId;
    private String transferId;
    private Integer maxRecords;
}
