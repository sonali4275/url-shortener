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

    // Base62 alphabet keeps short codes URL-safe with no encoding needed.
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    public ShortUrlService(ShortUrlRepository shortUrlRepository,
                            ClickEventRepository clickEventRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * Creates a new short URL. Generates a random Base62 code and retries
     * on the rare collision instead of relying on a sequential counter -
     * this keeps codes non-guessable and avoids a single point of contention
     * under concurrent writes.
     */
    @Transactional
    public ShortUrl createShortUrl(String originalUrl) {
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(originalUrl, code);
        return shortUrlRepository.save(shortUrl);
    }

    /**
     * Resolves a short code back to the original URL and records a click
     * event for analytics. Runs in one transaction so the click count and
     * the click log never drift apart.
     */
    @Transactional
    public ShortUrl resolveAndRecordClick(String shortCode, String referrer) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        shortUrl.setClickCount(shortUrl.getClickCount() + 1);
        shortUrlRepository.save(shortUrl);

        clickEventRepository.save(new ClickEvent(shortCode, referrer));

        return shortUrl;
    }

    @Transactional(readOnly = true)
    public ShortUrl getByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    @Transactional(readOnly = true)
    public List<ClickEvent> getClickHistory(String shortCode) {
        // Confirms the code exists before returning (possibly empty) history,
        // so callers get a 404 instead of a silently empty list for typos.
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
