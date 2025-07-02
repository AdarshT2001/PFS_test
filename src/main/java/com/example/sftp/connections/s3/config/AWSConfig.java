package com.example.sftp.connections.s3.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AWSConfig {
    @Value("${aws.region}")
    private String region;

    @Value("${aws.uri:http://localhost:4566}")
    private String uri;

    @Value("${aws.accessKey:test}")
    private String accessKey;

    @Value("${aws.secretKey:test}")
    private String secretKey;
}
