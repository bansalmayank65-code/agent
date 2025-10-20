-- Migration to add unique constraint for policy_actions table
-- This ensures that each combination of env_name, interface_num, policy_cat1, and policy_cat2 is unique

-- Check if constraint already exists before adding
DO $$
BEGIN
    -- Add unique constraint if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_policy_actions_combination'
    ) THEN
        ALTER TABLE policy_actions 
        ADD CONSTRAINT uk_policy_actions_combination 
        UNIQUE (env_name, interface_num, policy_cat1, policy_cat2);
        
        RAISE NOTICE 'Unique constraint uk_policy_actions_combination added successfully';
    ELSE
        RAISE NOTICE 'Unique constraint uk_policy_actions_combination already exists';
    END IF;
END $$;

-- Verify the constraint was added
SELECT 
    conname as constraint_name,
    contype as constraint_type,
    pg_get_constraintdef(oid) as definition
FROM pg_constraint 
WHERE conname = 'uk_policy_actions_combination';
