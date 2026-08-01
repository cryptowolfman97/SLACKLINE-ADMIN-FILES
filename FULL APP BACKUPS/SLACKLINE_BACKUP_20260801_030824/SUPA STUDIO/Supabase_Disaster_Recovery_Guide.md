# Supabase Disaster Recovery — Restoration Guide

Applies to backups produced by the **Full Backup** module (Supa Studio / SlackLine Admin).

---

## What you'll have from a backup

Each run folder contains:
```
Full Backup Files/<date>/backup_<ref>_<uniqueId>/
├── schema.sql        ← executable DDL: tables, keys, indexes, policies, functions, triggers
├── data/
│   └── <table>.json  ← one file per table, full row data
└── backup_<ref>_<uniqueId>.zip   ← both of the above, bundled
```

**Important:** `schema.sql` does *not* include `auth`, `storage`, `realtime`, `vault`, or `extensions` schema tables — those are Supabase-managed and get recreated fresh automatically. It *does* include RLS policies on those schemas (e.g. `storage.objects` bucket rules), since those are yours.

---

## Step 1 — Create the new Supabase project

1. Go to [supabase.com/dashboard](https://supabase.com/dashboard) and create a new project.
2. Choose the same region as before if possible (not required, but keeps latency consistent).
3. Wait for provisioning to finish — the project needs its own fresh `auth`, `storage`, `realtime` schemas before you touch anything.

## Step 2 — Restore the schema

1. Open **SQL Editor** in the new project's dashboard (or use your own app's SQL Editor tab, pointed at the new project ref).
2. Open `schema.sql` from your backup.
3. Run it **top to bottom, once**, against the empty database.
   - It runs in dependency order already: extensions → enums → tables → primary keys → foreign keys → indexes → RLS enable + policies → functions → triggers.
   - If anything errors partway (e.g. a function referencing a table that hasn't been created yet due to ordering), don't panic — re-run the file again. `CREATE TABLE IF NOT EXISTS` and `CREATE OR REPLACE FUNCTION` are safe to re-run; only `ADD PRIMARY KEY` / `ADD FOREIGN KEY` / `CREATE POLICY` could throw a "already exists" error on a second pass. If that happens, just run the remaining statements manually.
4. Confirm the tables show up under **Database → Tables** in the dashboard.

## Step 3 — Restore the data

You have two options, depending on table sizes:

**Option A — Manual, via SQL Editor (small/medium tables)**
1. For each `data/<table>.json` file, convert it to INSERT statements, or
2. Use Supabase's dashboard **Table Editor → Insert → Import data from CSV/JSON** if it supports your table's shape.

**Option B — Scripted (recommended, especially for larger tables)**
1. Write a small script (Python, Node, or even a Kotlin routine in your own app) that:
   - Reads each `data/<table>.json` file
   - POSTs the rows to the new project's REST API (`https://<new-ref>.supabase.co/rest/v1/<table>`) using the **service_role** key, in batches (e.g. 500 rows at a time)
2. Restore tables in dependency order — parent tables (referenced by foreign keys) before child tables — or temporarily disable FK constraints, load everything, then re-enable.

> This is the one part of your current backup tooling that isn't fully automated end-to-end. If you want, next session I can build a "Restore" companion feature into Supa Studio/SlackLine that reads a backup folder and replays the data automatically — happy to build that whenever you're ready.

## Step 4 — Restore Auth users (if applicable)

Your backup does **not** capture full `auth.users` data (only a summary, by design — full auth restore has its own considerations around password hashes and provider tokens).

- If you need users restored, this typically means either:
  - Re-inviting users to sign up again, or
  - Using Supabase's Auth migration tools if you have a raw `auth.users`/`auth.identities` export (not part of this backup — flag if you want this added).

## Step 5 — Restore Storage buckets

1. Recreate buckets manually (Storage → New bucket) — names and public/private settings aren't captured in `schema.sql` in detail beyond the bucket list, so check your bucket config notes if you have them.
2. Re-upload files if you have them backed up separately (this backup captures **metadata and policies**, not the actual file blobs in storage — that's a separate concern from database backup).

## Step 6 — Point your apps at the new project

1. In each app (SlackLine, SHV Store, Supa Studio itself, etc.), update the Supabase project URL and anon/service keys to the new project's credentials.
2. Test each app against the new project before considering the migration complete.

## Step 7 — Verify

- Row counts per table match what you expect (cross-check against the `data/*.json` file sizes/row counts).
- RLS policies are active (`Database → Policies` in dashboard) and behave as expected — test a query as a non-admin user.
- Functions and triggers fire correctly (test at least one write that should trigger something, if you have any).

---

## Quick reference — order of operations

```
1. New Supabase project created
2. Run schema.sql (schema + policies + functions + triggers)
3. Load data/*.json into tables (parents before children)
4. Re-invite/restore auth users if needed
5. Recreate storage buckets + re-upload files
6. Update app configs to new project ref/keys
7. Verify row counts, RLS, functions
```
