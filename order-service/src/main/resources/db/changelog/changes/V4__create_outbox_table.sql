CREATE TABLE if not exists outbox_event (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,      -- 'Order'
    aggregate_id VARCHAR(255) NOT NULL,        -- orderId
    event_type VARCHAR(255) NOT NULL,          -- 'OrderCreatedEvent'
    payload JSON NOT NULL,                    -- тело события
    status VARCHAR(50) NOT NULL DEFAULT 'NEW', -- NEW, PUBLISHED
    trace_id VARCHAR(32) NOT NULL,
    span_id VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

drop index if exists idx_outbox_event_status_created_at;
CREATE INDEX idx_outbox_event_status_created_at ON outbox_event (status, created_at);