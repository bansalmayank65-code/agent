-- Migration to add performance indexes for policy_actions table
-- These composite indexes optimize the most common query patterns

-- Drop old basic composite index if it exists
DROP INDEX IF EXISTS idx_policy_actions_env_interface;

-- Composite indexes for query performance optimization

-- Optimizes: SELECT * WHERE env_name=? AND interface_num=? ORDER BY last_updated_at DESC
-- This is the most common query pattern for listing policy actions
CREATE INDEX IF NOT EXISTS idx_policy_actions_env_interface_updated 
    ON policy_actions(env_name, interface_num, last_updated_at DESC);

-- Optimizes: SELECT DISTINCT policy_cat1 WHERE env_name=? AND interface_num=? ORDER BY policy_cat1
-- Used by the Build Actions tab dropdown for Category 1
CREATE INDEX IF NOT EXISTS idx_policy_actions_env_interface_cat1 
    ON policy_actions(env_name, interface_num, policy_cat1);

-- Optimizes: WHERE env_name=? AND interface_num=? AND policy_cat1=? AND policy_cat2=?
-- Used by unique constraint checks and filtering
CREATE INDEX IF NOT EXISTS idx_policy_actions_env_interface_cat1_cat2 
    ON policy_actions(env_name, interface_num, policy_cat1, policy_cat2);

-- Optimizes: SELECT * WHERE env_name=? AND interface_num=? AND policy_cat1=? ORDER BY last_updated_at DESC
-- Used when filtering by Category 1 in the View tab
CREATE INDEX IF NOT EXISTS idx_policy_actions_env_interface_cat1_updated 
    ON policy_actions(env_name, interface_num, policy_cat1, last_updated_at DESC);

-- Verify indexes were created
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'policy_actions' 
  AND indexname LIKE 'idx_policy_actions_%'
ORDER BY indexname;

-- Show index sizes for monitoring
SELECT
    indexrelname AS index_name,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public' 
  AND relname = 'policy_actions'
ORDER BY indexrelname;

RAISE NOTICE 'Performance indexes added successfully';
