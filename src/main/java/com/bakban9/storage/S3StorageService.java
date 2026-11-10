package com.bakban9.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Object storage (AWS S3, Cloudflare R2, MinIO). The bucket is served from {@code publicBaseUrl}. */
public class S3StorageService implements StorageService {

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(S3Client client, String bucket, String publicBaseUrl) {
        this.client = client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @Override
    public String put(String key, byte[] data, String contentType) {
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable");
        if ("image/svg+xml".equals(contentType)) {
            req.contentDisposition("inline");
        }
        client.putObject(req.build(), RequestBody.fromBytes(data));
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
