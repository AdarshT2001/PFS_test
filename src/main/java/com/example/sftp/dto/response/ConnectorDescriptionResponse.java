package com.example.sftp.dto.response;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.services.transfer.model.SftpConnectorConfig;
import software.amazon.awssdk.services.transfer.model.Tag;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConnectorDescriptionResponse {

    private String arn;
    private String connectorId;
    private String url;
    private String accessRole;
    private String loggingRole;
    private Map<String, String> tags;
    private Map<String,Object> sftpConfig;
    private String serviceManagedEgressIpAddresses;
    private String securityPolicyName;
}
