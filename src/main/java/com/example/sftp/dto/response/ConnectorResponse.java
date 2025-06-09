package com.example.sftp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorResponse {
    private String arn;
    private String connectorId;
    private String url;

}
