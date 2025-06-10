package com.example.sftp.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DirectoryListingRequest {
    private String connectorId;
    private String remoteDirectoryPath;
    private String outputDirectoryPath;
    private Integer maxItems;
}
