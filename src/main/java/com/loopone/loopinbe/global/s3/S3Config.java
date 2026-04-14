package com.loopone.loopinbe.global.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Profile("!test")
public class S3Config {
    @Value("${spring.aws.credentials.access-key}")
    private String accessKeyId;

    @Value("${spring.aws.credentials.secret-key}")
    private String secretAccessKey;

    @Value("${spring.aws.credentials.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
        var builder = S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(credentials);

        if (!endpoint.isBlank()) { // OCI: endpoint 있으면 override
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)
                           .build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
        var builder = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(credentials);

        if (!endpoint.isBlank()) { // OCI: endpoint 있으면 override
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)
                           .build());
        }
        return builder.build();
    }
}
