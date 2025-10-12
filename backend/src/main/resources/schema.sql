-- Amazon Agentic Workstation Database Schema

DROP TRIGGER IF EXISTS task_update_timestamp ON task;
DROP TRIGGER IF EXISTS login_update_timestamp ON login;
DROP FUNCTION IF EXISTS update_timestamp() CASCADE;
DROP TABLE IF EXISTS task_history CASCADE;
DROP TABLE IF EXISTS task CASCADE;
DROP TABLE IF EXISTS login_history CASCADE;
DROP TABLE IF EXISTS login CASCADE;


CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_date_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TABLE IF NOT EXISTS login (
    user_id VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS login_history (
    login_history_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    login_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    login_status VARCHAR(20) NOT NULL CHECK (login_status IN ('SUCCESS', 'FAILED', 'LOGOUT')),
    failure_reason VARCHAR(255),
    session_id VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES login(user_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS task (
    task_id VARCHAR(100) PRIMARY KEY,
    env_name VARCHAR(100) NOT NULL,
    interface_num INT NOT NULL,
    instruction TEXT NOT NULL,
    num_of_edges INT DEFAULT 0,
    task_json TEXT NOT NULL,
    result_json TEXT,
    user_id VARCHAR(50) NOT NULL,
    task_status VARCHAR(20) DEFAULT 'DRAFT' CHECK (task_status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'NEEDS_CHANGES', 'MERGED', 'DISCARDED')),
    is_active BOOLEAN DEFAULT TRUE,
    created_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES login(user_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS task_history (
    history_id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    action_type VARCHAR(20) NOT NULL CHECK (action_type IN ('CREATED', 'UPDATED', 'DELETED', 'STATUS_CHANGED')),
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(50) NOT NULL,
    change_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    change_reason VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (task_id) REFERENCES task(task_id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES login(user_id) ON DELETE CASCADE
);
CREATE OR REPLACE TRIGGER login_update_timestamp
    BEFORE UPDATE ON login
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE OR REPLACE TRIGGER task_update_timestamp
    BEFORE UPDATE ON task
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();
CREATE INDEX IF NOT EXISTS idx_login_is_active ON login(is_active);
CREATE INDEX IF NOT EXISTS idx_login_created_date ON login(created_date_time);
CREATE INDEX IF NOT EXISTS idx_login_history_user_id ON login_history(user_id);
CREATE INDEX IF NOT EXISTS idx_login_history_timestamp ON login_history(login_timestamp);
CREATE INDEX IF NOT EXISTS idx_login_history_status ON login_history(login_status);
CREATE INDEX IF NOT EXISTS idx_login_history_session ON login_history(session_id);
CREATE INDEX IF NOT EXISTS idx_login_history_ip ON login_history(ip_address);
CREATE INDEX IF NOT EXISTS idx_task_user_id ON task(user_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(task_status);
CREATE INDEX IF NOT EXISTS idx_task_env_name ON task(env_name);
CREATE INDEX IF NOT EXISTS idx_task_interface_num ON task(interface_num);
CREATE INDEX IF NOT EXISTS idx_task_active ON task(is_active);
CREATE INDEX IF NOT EXISTS idx_task_created_date ON task(created_date_time);
CREATE INDEX IF NOT EXISTS idx_task_updated_date ON task(updated_date_time);
CREATE INDEX IF NOT EXISTS idx_task_status_active ON task(task_status, is_active);
CREATE INDEX IF NOT EXISTS idx_task_user_status ON task(user_id, task_status);
CREATE INDEX IF NOT EXISTS idx_task_history_task_id ON task_history(task_id);
CREATE INDEX IF NOT EXISTS idx_task_history_changed_by ON task_history(changed_by);
CREATE INDEX IF NOT EXISTS idx_task_history_timestamp ON task_history(change_timestamp);
CREATE INDEX IF NOT EXISTS idx_task_history_action_type ON task_history(action_type);
CREATE INDEX IF NOT EXISTS idx_task_history_task_action ON task_history(task_id, action_type);

INSERT INTO login (user_id, password) VALUES 
('mayank', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');