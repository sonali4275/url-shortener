package com.sonali.urlshortener.service;

import com.sonali.urlshortener.entity.ShortUrl;
import com.sonali.urlshortener.exception.UrlNotFoundException;
import com.sonali.urlshortener.repository.ShortUrlRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlCacheService {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlCacheService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Cacheable(value = "shortUrlLookup", key = "#shortCode")
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .map(ShortUrl::getOriginalUrl)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }
}