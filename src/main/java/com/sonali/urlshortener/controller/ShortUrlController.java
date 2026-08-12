package com.sonali.urlshortener.controller;

import com.sonali.urlshortener.dto.CreateUrlRequest;
import com.sonali.urlshortener.dto.ShortUrlResponse;
import com.sonali.urlshortener.entity.ClickEvent;
import com.sonali.urlshortener.entity.ShortUrl;
import com.sonali.urlshortener.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    // POST /api/urls  -> create a short URL
    @PostMapping("/api/urls")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        ShortUrl shortUrl = shortUrlService.createShortUrl(request.getOriginalUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(shortUrl));
    }

    // GET /api/urls/{code}  -> metadata + click count, no redirect, no click recorded
    @GetMapping("/api/urls/{code}")
    public ResponseEntity<ShortUrlResponse> getUrlInfo(@PathVariable("code") String code) {
        ShortUrl shortUrl = shortUrlService.getByShortCode(code);
        return ResponseEntity.ok(toResponse(shortUrl));
    }

    // GET /api/urls/{code}/analytics -> click history
    @GetMapping("/api/urls/{code}/analytics")
    public ResponseEntity<List<ClickEvent>> getAnalytics(@PathVariable("code") String code) {
        return ResponseEntity.ok(shortUrlService.getClickHistory(code));
    }

    // GET /{code}  -> actual redirect, records a click
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code, HttpServletRequest request) {
        String referrer = request.getHeader(HttpHeaders.REFERER);
        ShortUrl shortUrl = shortUrlService.resolveAndRecordClick(code, referrer);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortUrl.getOriginalUrl()))
                .build();
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        String shortUrlText = "http://localhost:8080/" + shortUrl.getShortCode();
        return new ShortUrlResponse(
                shortUrl.getShortCode(),
                shortUrlText,
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getClickCount()
        );
    }
}
