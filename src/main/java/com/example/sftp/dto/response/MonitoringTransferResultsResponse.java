package com.example.sftp.dto.response;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.services.transfer.model.TransferTableStatus;

@Data
@Builder
public class MonitoringTransferResultsResponse {
    private String filePath;
    private TransferTableStatus statusCode;
    private String failureCode;
    private String failureMessage;

}
