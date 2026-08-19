package com.sonali.urlshortener.service;

import com.sonali.urlshortener.entity.ClickEvent;
import com.sonali.urlshortener.entity.ShortUrl;
import com.sonali.urlshortener.exception.UrlNotFoundException;
import com.sonali.urlshortener.repository.ClickEventRepository;
import com.sonali.urlshortener.repository.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class ShortUrlService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortUrlCacheService shortUrlCacheService;

    public ShortUrlService(ShortUrlRepository shortUrlRepository,
                            ClickEventRepository clickEventRepository,
                            ShortUrlCacheService shortUrlCacheService) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.shortUrlCacheService = shortUrlCacheService;
    }

    @Transactional
    public ShortUrl createShortUrl(String originalUrl) {
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(originalUrl, code);
        return shortUrlRepository.save(shortUrl);
    }

    @Transactional
    public String resolveAndRecordClick(String shortCode, String referrer) {
        String originalUrl = shortUrlCacheService.getOriginalUrl(shortCode);

        shortUrlRepository.incrementClickCount(shortCode);
        clickEventRepository.save(new ClickEvent(shortCode, referrer));

        return originalUrl;
    }

    @Transactional(readOnly = true)
    public ShortUrl getByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    @Transactional(readOnly = true)
    public List<ClickEvent> getClickHistory(String shortCode) {
        if (!shortUrlRepository.existsByShortCode(shortCode)) {
            throw new UrlNotFoundException(shortCode);
        }
        return clickEventRepository.findByShortCodeOrderByClickedAtDesc(shortCode);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (shortUrlRepository.existsByShortCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}