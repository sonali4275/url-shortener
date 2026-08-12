package com.sonali.urlshortener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    // Nullable: browser/client may not always send this header
    @Column(length = 512)
    private String referrer;

    public ClickEvent(String shortCode, String referrer) {
        this.shortCode = shortCode;
        this.clickedAt = LocalDateTime.now();
        this.referrer = referrer;
    }
}
