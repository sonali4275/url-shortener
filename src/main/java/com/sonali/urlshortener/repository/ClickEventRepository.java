package com.sonali.urlshortener.repository;

import com.sonali.urlshortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByShortCodeOrderByClickedAtDesc(String shortCode);
}
