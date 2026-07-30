# Alpha 23 — Verified Business Write Coordinator

Date: 2026-07-30
Branch: `agent/alpha23-relational-write-api`
Base: Alpha 22 verified documentation head
Stable production base: owner-approved Alpha 21 `main`

## Goal

Introduce a transaction-safe write coordination layer without making relational SQLite authoritative.

## Implemented

- Optimistic precondition fingerprint on every coordinated mutation.
- Immutable transaction UUID.
- Pure/testable mutations for:
  - customer upsert,
  - girvi upsert,
  - payment append,
  - restore snapshot replacement.
- Duplicate customer/girvi/payment/receipt safeguards.
- Released-girvi payment block.
- Authoritative encrypted snapshot save and immediate read-back fingerprint verification.
- Relational incremental sync after verified snapshot persistence.
- Independent relational dual-read comparison.
- Exact success journal event with transaction ID, mutation label, before/after fingerprint prefixes and sync mode.
- Cutover observation policy requiring at least 25 coordinated verified writes and a matching latest relational fingerprint.
- Regression tests for mutation isolation, duplicate receipt protection, released-girvi protection, restore replacement and cutover blockers.
- Version `23` / `0.23.0-testing`.

## Safety boundary

- The encrypted snapshot remains authoritative.
- A relational failure never changes the already verified snapshot semantics.
- Normal screens are not switched wholesale to this coordinator in the first patch.
- No relational read/write cutover is enabled.
- Alpha 22 exact APK source is frozen at `candidate/alpha22-testing`.
- Owner-approved Alpha 21 remains on `main` and `baseline/alpha21-owner-approved`.

## Next wiring

After this core is CI-green, selected low-risk write paths will be migrated one at a time, beginning with customer upsert and new-girvi creation. Each path must retain current accounting validation and must record coordinated-write evidence before wider adoption.
