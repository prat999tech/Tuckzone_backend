-- Lets an admin upload a food photo instead of pasting an external image URL.
--
-- image_url (from V2) is kept exactly as-is: existing catalog rows that already have an
-- external URL there keep working unchanged, and a new upload never touches that column —
-- the application layer prefers image_data when present and falls back to image_url
-- otherwise, so nothing needs to be migrated or backfilled here.
alter table menu_items add column image_data bytea;
alter table menu_items add column image_content_type varchar(100);
