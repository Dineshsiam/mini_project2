-- ============================================================
-- Kisan Setu — Conversational AI Upgrade Migration
-- Run this on existing databases if you are NOT using ddl-auto:update
-- JPA ddl-auto:update will handle this automatically for new installs.
-- ============================================================

-- 1. Add metadata columns to existing schemes table (SAFE: no data loss)
ALTER TABLE schemes
    ADD COLUMN IF NOT EXISTS source_url TEXT,
    ADD COLUMN IF NOT EXISTS last_scraped_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS translations JSONB;

-- 2. Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    language VARCHAR(10) NOT NULL DEFAULT 'en'
);

-- 3. Chat messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,   -- USER | ASSISTANT
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_time
    ON chat_messages(conversation_id, created_at);
