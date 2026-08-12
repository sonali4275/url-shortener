package com.sonali.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateUrlRequest {

    @NotBlank(message = "originalUrl must not be blank")
    @Pattern(regexp = "^https?://.+", message = "originalUrl must start with http:// or https://")
    private String originalUrl;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}
