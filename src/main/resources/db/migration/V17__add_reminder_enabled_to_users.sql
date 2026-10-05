ALTER TABLE users ADD COLUMN reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Users who already have a device registered keep their reminder on.
UPDATE users SET reminder_enabled = TRUE WHERE id IN (SELECT DISTINCT user_id FROM push_subscriptions);
