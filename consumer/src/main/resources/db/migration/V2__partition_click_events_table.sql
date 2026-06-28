-- V2: Click Events Table Partitioned | Analytics Schema | Consumer Service
DROP TABLE IF EXISTS analytics.click_events;
CREATE TABLE analytics.click_events (
    id BIGSERIAL,
    link_code VARCHAR(15) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    country VARCHAR(100),
    device VARCHAR(50),
    os VARCHAR(100),
    browser VARCHAR(100),
    user_agent TEXT NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id, clicked_at)
) PARTITION BY RANGE (clicked_at);

CREATE TABLE analytics.click_events_2026_06
    PARTITION OF analytics.click_events
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE analytics.click_events_2026_07
    PARTITION OF analytics.click_events
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');