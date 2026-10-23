-- Brownfield change: callers may now supply a human-readable alias
-- instead of only accepting a generated 8-character code.
ALTER TABLE short_links DROP CONSTRAINT short_links_code_len;

ALTER TABLE short_links
    ADD CONSTRAINT short_links_code_len CHECK (char_length(code) BETWEEN 4 AND 32);

ALTER TABLE short_links
    ADD COLUMN custom_alias BOOLEAN NOT NULL DEFAULT false;
