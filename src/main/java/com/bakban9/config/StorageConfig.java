package com.bakban9.config;

import com.bakban9.storage.LocalStorageService;
import com.bakban9.storage.S3StorageService;
import com.bakban9.storage.StorageService;
import java.net.URI;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class StorageConfig {

    @Bean
    StorageService storageService(AppProperties props) {
        AppProperties.Storage s = props.storage();
        if ("s3".equalsIgnoreCase(s.type())) {
            AppProperties.S3 c = s.s3();
            S3ClientBuilder b = S3Client.builder()
                    .region(Region.of(c.region()))
                    .forcePathStyle(c.pathStyle())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(c.accessKey(), c.secretKey())));
            if (c.endpoint() != null && !c.endpoint().isBlank()) {
                b.endpointOverride(URI.create(c.endpoint()));
            }
            return new S3StorageService(b.build(), c.bucket(), s.publicBaseUrl());
        }
        return new LocalStorageService(Path.of(s.localDir()), s.publicBaseUrl());
    }
}
