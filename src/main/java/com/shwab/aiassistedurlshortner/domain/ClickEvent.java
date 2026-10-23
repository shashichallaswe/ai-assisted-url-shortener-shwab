package com.shwab.aiassistedurlshortner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt = Instant.now();

    @Column(name = "ip_hash")
    private byte[] ipHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "referer")
    private String referer;

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(Instant clickedAt) {
        this.clickedAt = clickedAt;
    }

    public void setIpHash(byte[] ipHash) {
        this.ipHash = ipHash;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }
}
