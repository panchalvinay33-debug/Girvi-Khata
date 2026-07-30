# Alpha 22 — Relational Master-ID Schema Foundation

Date: 2026-07-30
Branch: `agent/post-alpha21-development`
Base: owner-approved Alpha 21 merged `main`

## Goal

Normalize Business Master references into dedicated relational IDs without changing the authoritative encrypted snapshot or risking the owner-approved Alpha 21 base.

## Implemented in this milestone

- SQLite schema-v2 contract for an encrypted `masters` table.
- Dedicated nullable foreign-key columns:
  - `items.item_master_id`
  - `items.unit_id`
  - `items.locker_id`
  - `girvis.interest_plan_id`
  - `payments.payment_mode_id`
- Non-destructive v1→v2 SQL migration contract. It contains no `DROP TABLE` or business-row deletion.
- Foreign-key indexes for all new link columns.
- Deterministic master-catalog normalization by kind and stable ID.
- Duplicate-ID, invalid-name, invalid-rate and non-interest-rate rejection.
- Link-coverage model for later dashboard and cutover blockers.
- Unit tests proving schema completeness and non-destructive migration rules.
- App version started at `22` / `0.22.0-testing`.

## Safety decision

The existing Alpha 21 SQLite helper is not switched to schema v2 in this first patch. The migration SQL is isolated and testable first. This prevents an unreviewed schema change from opening the owner-approved database on a physical device before compile/test evidence exists.

The next patch will:

1. Wire schema v2 into `EncryptedRelationalShadowStore.onCreate/onUpgrade`.
2. Load the encrypted master catalog during shadow sync.
3. Insert encrypted master rows.
4. Resolve and backfill item/unit/locker/plan/payment-mode links transactionally.
5. Verify unresolved/manual values remain null.
6. Add link coverage and schema version to Database Migration Status.
7. Run complete Android CI and issue the Alpha 22 APK only after the real upgrade path is green.

## Rollback contract

- `main` remains the owner-approved Alpha 21 base.
- Permanent tested-source branch remains `baseline/alpha21-owner-approved`.
- This branch must not merge until Alpha 22 is built, physically tested and explicitly approved.
- Snapshot remains source-of-truth; relational schema is still rebuildable shadow data.
