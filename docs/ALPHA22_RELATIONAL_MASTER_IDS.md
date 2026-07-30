# Alpha 22 — Relational Master-ID Schema and Backfill

Date: 2026-07-30
Branch: `agent/post-alpha21-development`
Base: owner-approved Alpha 21 merged `main`
Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING

## Goal

Normalize Business Master references into dedicated relational IDs without changing the authoritative encrypted snapshot or risking the owner-approved Alpha 21 base.

## Implemented

- Encrypted relational `masters` table contract.
- Dedicated nullable foreign-key columns:
  - `items.item_master_id`
  - `items.unit_id`
  - `items.locker_id`
  - `girvis.interest_plan_id`
  - `payments.payment_mode_id`
- Idempotent non-destructive on-device schema installer.
- No table drops and no business-row deletion in the migration contract.
- Android-Keystore AES-GCM encryption for relational master names and category labels.
- Stable persistence of first-load default master UUIDs in the encrypted catalog store.
- Canonical master ordering by stable ID.
- Deterministic transactional backfill for item/category, unit, locker, interest-rate and payment-mode links.
- Ambiguous matches remain null instead of being guessed.
- Automatic master synchronization after every successful relational business sync.
- Exact link-coverage verification and journal events.
- PIN-protected Database Migration Status card and manual install/verify action.
- App version `22` / `0.22.0-testing`.

## Verified build

- Exact source: `5ee98f358159c74820230da6b3f29a23577345c7`
- Android workflow: `30548491236` — SUCCESS
- Security Guard: `30548495430` — SUCCESS
- Artifact ID: `8761882280`
- APK size: `20,419,274 bytes`
- APK SHA-256: `4b083897615869af974a176804579fc39ed841725aaaca8d9eb774cf07eb27fa`

Detailed evidence: `docs/releases/ALPHA22_RELEASE_EVIDENCE.md`.

## Safety decision

The owner-approved Alpha 21 encrypted snapshot remains the sole source-of-truth. Alpha 22 changes only the rebuildable relational shadow. Missing, manual or ambiguous master links remain nullable and never delete or rewrite business records.

Relational cutover remains blocked.

## Rollback contract

- `main` remains the owner-approved Alpha 21 base.
- Permanent tested-source branch remains `baseline/alpha21-owner-approved`.
- Draft PR #2 must not merge until Alpha 22 is physically tested and explicitly approved.
- Recovery continues to follow `docs/ROLLBACK_RUNBOOK.md`.
