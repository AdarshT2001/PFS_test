package com.example.sftp.connections.s3;

import com.example.sftp.connections.s3.config.AWSConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3ObjClient {

    private final AWSConfig awsConfig;
    private final Environment environment;
    private volatile S3Client s3Client;

    @Bean
    @Profile("dev")
    public synchronized S3Client getLocalS3Client() {
        if (s3Client == null) {
            StsClient stsClient = StsClient.builder()
                    .region(Region.of(awsConfig.getRegion()))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create(awsConfig.getAccessKey(), awsConfig.getSecretKey()))
                    )
                    .build();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn("arn:aws:iam::192868178773:role/WPS-dev-role") // Replace with your IAM Role ARN
                    .roleSessionName("WPS-dev-role")
                    .build();


            Supplier<AssumeRoleRequest> assumeRoleRequestSupplier = () -> assumeRoleRequest;


            val stsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequestSupplier)// Use the STS client
                    .build();


            s3Client = S3Client.builder()
                    .region(Region.of(awsConfig.getRegion()))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create(awsConfig.getAccessKey(), awsConfig.getSecretKey()))
//                            stsAssumeRoleCredentialsProvider
                    )
                    .endpointOverride(URI.create(awsConfig.getUri()))
                    .serviceConfiguration(
                            S3Configuration.builder()
                                    .pathStyleAccessEnabled(true) // critical for LocalStack
                                    .build()
                    )
                    .httpClient(
                            ApacheHttpClient.builder()
                                    .connectionTimeout(Duration.ofMinutes(30))
                                    .socketTimeout(Duration.ofMinutes(30))
                                    .build()
                    )
                    .build();
        }
        return s3Client;
    }

    @Bean
    @Profile("!dev")
    @Primary
    public synchronized S3Client getS3RemoteClient() {
        if (s3Client == null) {

            s3Client = S3Client.builder()
                    .region(Region.of(awsConfig.getRegion()))
                    .serviceConfiguration(
                            S3Configuration.builder()
                                    .pathStyleAccessEnabled(true) // critical for LocalStack
                                    .build()
                    )
                    .httpClient(
                            ApacheHttpClient.builder()
                                    .connectionTimeout(Duration.ofMinutes(30))
                                    .socketTimeout(Duration.ofMinutes(30))
                                    .build()
                    )
                    .build();
        }
        return s3Client;
    }

    public synchronized S3Client getS3Client() {
        if (s3Client == null) {
            String activeProfile = environment.getActiveProfiles()[0];

            if ("dev".equalsIgnoreCase(activeProfile)) {
                s3Client = getLocalS3Client();
                log.info("Initialized S3 client with LOCAL configuration for profile: {}", activeProfile);
            } else {
                s3Client = getS3RemoteClient();
                log.info("Initialized S3 client with REMOTE configuration for profile: {}", activeProfile);
            }
        }
        return s3Client;
    }

}
