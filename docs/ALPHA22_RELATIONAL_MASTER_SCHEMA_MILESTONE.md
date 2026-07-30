# Alpha 22 — Relational Master-ID Schema and Backfill

Date: 2026-07-30
Branch: `agent/post-alpha21-development`
Base: owner-approved Alpha 21 merged `main`

## Goal

Normalize Business Master references into dedicated relational IDs without changing the authoritative encrypted snapshot or risking the owner-approved Alpha 21 base.

## Implemented

- SQLite schema-v2 contract for an encrypted `masters` table.
- Dedicated nullable foreign-key columns:
  - `items.item_master_id`
  - `items.unit_id`
  - `items.locker_id`
  - `girvis.interest_plan_id`
  - `payments.payment_mode_id`
- Non-destructive migration SQL contract with no `DROP TABLE` or business-row deletion.
- Idempotent on-device schema installer that checks tables/columns before applying only missing changes.
- Encrypted master names/category labels using Android Keystore AES-256-GCM with field-specific associated data.
- Deterministic master-catalog normalization by kind and stable ID.
- First-load persistence of default master catalog so default UUIDs remain stable across app restarts.
- Transactional master-row replacement and link backfill.
- Item master links resolve by item name + category.
- Unit and locker links resolve from schema-compatible item metadata.
- Interest-plan links resolve only when the monthly basis-point rate has exactly one matching plan.
- Payment-mode links resolve only when the normalized mode name has exactly one matching master.
- Ambiguous or manual values remain null instead of being guessed.
- Exact post-write coverage verification for Item, Unit, Locker, Interest Plan and Payment Mode links.
- Automatic master-link synchronization after every verified relational business sync.
- Encrypted journal events for master-link success/failure.
- PIN-protected Database Migration Status card with row count, coverage and last verification time.
- Manual `Install / Verify Master-ID Links` action.
- App version `22` / `0.22.0-testing`.

## Safety behavior

- The Alpha 21 encrypted snapshot remains the only source-of-truth.
- Schema installation and backfill operate only on the rebuildable relational shadow.
- A master-link failure cannot roll back, replace or delete the authoritative snapshot.
- All schema changes and backfill writes occur in SQLite transactions.
- Unresolved links are expected and preserved as null; incomplete automatic coverage is not treated as business-data corruption.
- Relational read/write cutover remains blocked.

## Tests

- Schema includes every required foreign-key column and index.
- Migration contract contains no destructive table drop or business-row deletion.
- Catalog normalization is deterministic.
- Duplicate master IDs are rejected.
- Rates on non-interest masters are rejected.
- Link coverage completeness is calculated correctly.
- Item/Unit/Locker/Plan/Payment Mode links resolve case-insensitively.
- Duplicate same-rate plans remain unresolved.
- Duplicate normalized payment modes remain unresolved.
- Backfill output remains deterministic when source collection order changes.

## Owner physical test

1. Install Alpha 22 over the owner-approved Alpha 21 without uninstalling.
2. Confirm PIN, fingerprint and all business records.
3. Open Tools → Database Migration Status.
4. Run Full Rebuild & Verify.
5. Run Install / Verify Master-ID Links.
6. Confirm encrypted master row count is shown.
7. Confirm Item/Unit/Locker/Plan/Payment Mode coverage counts are shown.
8. Manual or ambiguous values may remain unresolved; no record should disappear.
9. Create a new girvi and payment using saved masters.
10. Refresh status and confirm link verification timestamp updates.
11. Check Data Safety Status for `RELATIONAL_MASTER_LINKS_VERIFIED`.
12. Create a verified `.gkb` backup before any destructive restore test.

## Rollback contract

- `main` remains the owner-approved Alpha 21 base.
- Permanent tested-source branch remains `baseline/alpha21-owner-approved`.
- PR #2 stays draft and must not merge until Alpha 22 is physically tested and explicitly approved.
- A recovery APK should use Alpha 21 code, a higher Android version code and the same stable testing signature.
