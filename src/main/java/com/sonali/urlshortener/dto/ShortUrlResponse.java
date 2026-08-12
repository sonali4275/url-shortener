package com.sonali.urlshortener.dto;

import java.time.LocalDateTime;

public class ShortUrlResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private LocalDateTime createdAt;
    private long clickCount;

    public ShortUrlResponse(String shortCode, String shortUrl, String originalUrl,
                             LocalDateTime createdAt, long clickCount) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.clickCount = clickCount;
    }

    public String getShortCode() { return shortCode; }
    public String getShortUrl() { return shortUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getClickCount() { return clickCount; }
}
