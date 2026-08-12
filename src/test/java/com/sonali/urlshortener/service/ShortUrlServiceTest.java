package com.sonali.urlshortener.service;

import com.sonali.urlshortener.entity.ShortUrl;
import com.sonali.urlshortener.exception.UrlNotFoundException;
import com.sonali.urlshortener.repository.ClickEventRepository;
import com.sonali.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        service = new ShortUrlService(shortUrlRepository, clickEventRepository);
    }

    @Test
    void createShortUrl_savesUrlWithGeneratedCode() {
        when(shortUrlRepository.existsByShortCode(any())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = service.createShortUrl("https://example.com/some-long-page");

        assertNotNull(result.getShortCode());
        assertEquals(7, result.getShortCode().length());
        assertEquals("https://example.com/some-long-page", result.getOriginalUrl());
        verify(shortUrlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_retriesOnCollision() {
        // First generated code collides, second does not
        when(shortUrlRepository.existsByShortCode(any()))
                .thenReturn(true)
                .thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createShortUrl("https://example.com");

        verify(shortUrlRepository, times(2)).existsByShortCode(any());
    }

    @Test
    void resolveAndRecordClick_incrementsClickCountAndLogsEvent() {
        ShortUrl existing = new ShortUrl("https://example.com", "abc1234");
        when(shortUrlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(existing));
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = service.resolveAndRecordClick("abc1234", "https://google.com");

        assertEquals(1, result.getClickCount());

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getClickCount());
        verify(clickEventRepository, times(1)).save(any());
    }

    @Test
    void resolveAndRecordClick_throwsWhenCodeNotFound() {
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class,
                () -> service.resolveAndRecordClick("missing", null));

        verify(clickEventRepository, never()).save(any());
    }

    @Test
    void getClickHistory_throwsWhenCodeDoesNotExist() {
        when(shortUrlRepository.existsByShortCode("missing")).thenReturn(false);

        assertThrows(UrlNotFoundException.class, () -> service.getClickHistory("missing"));
    }
}
