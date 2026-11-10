package com.bakban9.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bakban")
public record AppProperties(
        String baseUrl,
        boolean cookieSecure,
        Owner owner,
        Seed seed,
        Mail mail,
        Storage storage,
        Security security,
        RateLimit rateLimit,
        Retention retention) {

    public record Owner(String email, String password, String name) {}

    public record Seed(boolean enabled) {}

    public record Mail(String from, String notifyTo) {}

    public record Storage(String type, String localDir, String publicBaseUrl, S3 s3) {}

    public record S3(String bucket, String region, String endpoint, String accessKey, String secretKey, boolean pathStyle) {}

    public record Security(int maxFailedLogins, int lockMinutes) {}

    public record RateLimit(int formMax, int formWindowMinutes, int loginMax, int loginWindowMinutes) {}

    public record Retention(int softDeleteDays) {}
}
