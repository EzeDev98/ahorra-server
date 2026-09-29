ALTER TABLE pending_entries
    ADD COLUMN ledger_entry_id BIGINT REFERENCES ledger_entries (id),
    ADD COLUMN confirmed_at    TIMESTAMP;

-- A ledger entry can come from at most one pending entry.
CREATE UNIQUE INDEX uk_pending_entries_ledger_entry
    ON pending_entries (ledger_entry_id) WHERE ledger_entry_id IS NOT NULL;

-- Lifecycle rule enforced in the schema: PENDING has no ledger link,
-- CONFIRMED/EDITED always have one.
ALTER TABLE pending_entries
    ADD CONSTRAINT ck_pending_entries_lifecycle CHECK (
        (status = 'PENDING' AND ledger_entry_id IS NULL AND confirmed_at IS NULL)
        OR (status IN ('CONFIRMED', 'EDITED') AND ledger_entry_id IS NOT NULL AND confirmed_at IS NOT NULL)
    );
