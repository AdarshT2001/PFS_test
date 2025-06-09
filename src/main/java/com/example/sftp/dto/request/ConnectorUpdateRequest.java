package com.example.sftp.dto.request;

import lombok.Data;

@Data
public class ConnectorUpdateRequest {
    private String connectorId;
    private String securityPolicyName;
    private String userSecretId;
    private Integer maxConcurrentConnections;
}
