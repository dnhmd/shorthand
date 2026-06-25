package com.shorthand.consumer.infrastructure.adapter.outbound.database;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "click_events", schema = "analytics")
public class ClickEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "link_code")
    private String linkCode;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "country")
    private String country;
    @Column(name = "device")
    private String device;
    @Column(name = "os")
    private String os;
    @Column(name = "browser")
    private String browser;
    @Column(name = "user_agent")
    private String userAgent;
    @Column(name = "clicked_at")
    private Instant clickedAt;

    public ClickEventEntity() {
    }

    public ClickEventEntity(Long id, String linkCode, String ipAddress, String country, String device, String os, String browser, String userAgent, Instant clickedAt) {
        this.id = id;
        this.linkCode = linkCode;
        this.ipAddress = ipAddress;
        this.country = country;
        this.device = device;
        this.os = os;
        this.browser = browser;
        this.userAgent = userAgent;
        this.clickedAt = clickedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLinkCode() {
        return linkCode;
    }

    public void setLinkCode(String linkCode) {
        this.linkCode = linkCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(Instant clickedAt) {
        this.clickedAt = clickedAt;
    }
}
