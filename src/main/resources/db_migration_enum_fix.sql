-- Migration script to update workflow_steps.required_role ENUM
-- 1. Temporarily expand ENUM to include both OLD and NEW values to prevent data loss during transition
ALTER TABLE workflow_steps MODIFY COLUMN required_role ENUM('ADMIN', 'SUPER_ADMIN', 'USER', 'GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER');

-- 2. Migrate existing data from OLD roles to NEW roles
-- Mapping: ADMIN -> TENANT_ADMIN
UPDATE workflow_steps SET required_role = 'TENANT_ADMIN' WHERE required_role = 'ADMIN';

-- Mapping: SUPER_ADMIN -> GLOBAL_ADMIN
-- (Assuming GLOBAL_ADMIN is the equivalent, though effectively they might be system level)
UPDATE workflow_steps SET required_role = 'GLOBAL_ADMIN' WHERE required_role = 'SUPER_ADMIN';

-- 3. Finalize ENUM definition to only include NEW values
ALTER TABLE workflow_steps MODIFY COLUMN required_role ENUM('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'USER');

-- Note: If you have a 'users' table or other tables using this enum, you should apply similar logic there.
-- Example for users table (uncomment if needed):
-- ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN', 'SUPER_ADMIN', 'USER', 'GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER');
-- UPDATE users SET role = 'TENANT_ADMIN' WHERE role = 'ADMIN';
-- UPDATE users SET role = 'GLOBAL_ADMIN' WHERE role = 'SUPER_ADMIN';
-- ALTER TABLE users MODIFY COLUMN role ENUM('GLOBAL_ADMIN', 'TENANT_ADMIN', 'TENANT_MANAGER', 'USER');
