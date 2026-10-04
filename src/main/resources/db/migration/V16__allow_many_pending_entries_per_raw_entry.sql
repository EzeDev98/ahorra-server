-- One note can describe several separate events, so a raw entry may now have several pending entries.
ALTER TABLE pending_entries DROP CONSTRAINT uk_pending_entries_raw_entry;
CREATE INDEX ix_pending_entries_raw_entry_id ON pending_entries (raw_entry_id);
