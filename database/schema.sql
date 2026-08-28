-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop table if exists
DROP TABLE IF EXISTS schemes;

-- Create schemes table
CREATE TABLE schemes (
    id UUID PRIMARY KEY,
    slug VARCHAR(255) UNIQUE NOT NULL,
    name TEXT NOT NULL,
    short_title TEXT,
    state VARCHAR(100) NOT NULL,
    department TEXT,
    level VARCHAR(50),
    category JSONB,             -- List of categories stored as JSON array
    subcategory JSONB,          -- List of subcategories stored as JSON array
    beneficiary TEXT,
    description TEXT,
    benefits TEXT,
    eligibility TEXT,
    documents JSONB,            -- List of documents stored as JSON array
    application_mode TEXT,
    application_link TEXT,
    faq JSONB,                  -- List of FAQs stored as JSON array of objects
    embedding vector(384)       -- 384-dimensional vector embedding
);

-- Create a vector index (Cosine Distance Index)
-- Using HNSW index for fast search queries
CREATE INDEX IF NOT EXISTS schemes_embedding_hnsw_idx 
ON schemes 
USING hnsw (embedding vector_cosine_ops);
