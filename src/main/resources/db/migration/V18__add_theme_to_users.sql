-- The theme the user explicitly chose in Settings, Appearance. NULL means they never chose one.
ALTER TABLE users ADD COLUMN theme VARCHAR(10);
