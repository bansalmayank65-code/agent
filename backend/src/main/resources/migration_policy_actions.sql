-- Migration script to add policy_actions table
-- Run this script in your PostgreSQL database

-- Create the trigger function if it doesn't exist
CREATE OR REPLACE FUNCTION update_timestamp_last_updated()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create policy_actions table
CREATE TABLE IF NOT EXISTS policy_actions (
    policy_action_id BIGSERIAL PRIMARY KEY,
    env_name VARCHAR(100) NOT NULL,
    interface_num INT NOT NULL,
    policy_cat1 VARCHAR(100) NOT NULL,
    policy_cat2 VARCHAR(100) NOT NULL,
    policy_description TEXT,
    actions_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_policy_actions_env_name ON policy_actions(env_name);
CREATE INDEX IF NOT EXISTS idx_policy_actions_interface_num ON policy_actions(interface_num);
CREATE INDEX IF NOT EXISTS idx_policy_actions_policy_cat1 ON policy_actions(policy_cat1);
CREATE INDEX IF NOT EXISTS idx_policy_actions_policy_cat2 ON policy_actions(policy_cat2);
CREATE INDEX IF NOT EXISTS idx_policy_actions_env_interface ON policy_actions(env_name, interface_num);

-- Create trigger
DROP TRIGGER IF EXISTS policy_actions_update_timestamp ON policy_actions;
CREATE TRIGGER policy_actions_update_timestamp
    BEFORE UPDATE ON policy_actions
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp_last_updated();

-- Verify table creation
SELECT 'policy_actions table created successfully' AS status;
