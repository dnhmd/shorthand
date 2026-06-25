-- V1: Initial Click Events Table Structure | Analytics Schema | Consumer Service
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE TABLE analytics.click_events (
    id BIGSERIAL PRIMARY KEY,
    link_code VARCHAR(15) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    country VARCHAR(100),
    device VARCHAR(50),
    os VARCHAR(100),
    browser VARCHAR(100),
    user_agent TEXT NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE
);