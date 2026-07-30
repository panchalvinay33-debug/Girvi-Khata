# Alpha 20 — Incremental Relational Sync and Dual-Read Proof

Status: implementation complete; automated Android verification pending.

## Implemented

- Added a pure deterministic delta planner for customers, categories and girvi subtrees.
- Unchanged snapshots produce no row rewrite.
- Customer/category changes use row-level update or insert.
- Changed girvi records replace only that girvi plus its item/payment children in one SQLite transaction.
- Removed girvi/customer/category rows are deleted in foreign-key-safe order.
- Every delta still ends with full row-count and decrypted semantic-fingerprint verification.
- Added dual-read comparison that independently reconstructs the relational snapshot and compares it with the encrypted snapshot.
- Added sync metadata: mode, changed-row count, consecutive healthy syncs, last mirror and failure timestamps.
- Added explicit failure-injection points for rollback testing; production calls always use NONE.
- Expanded the PIN-protected Database Migration Status screen with dual-read result, sync mode, changed rows, healthy-sync count and explicit cutover blockers.
- Cutover cannot be enabled from the screen.

## Cutover policy

Cutover remains blocked until all are true:

1. At least 25 consecutive healthy syncs.
2. Latest relational attempt is healthy.
3. Large-dataset stress verification passes.
4. Transaction rollback simulation passes.
5. Owner explicitly approves physical-device behavior.

## Automated tests

- Empty-delta behavior.
- One-payment change selects only one girvi subtree.
- Deleted customer/girvi planning.
- Cutover blocker policy.
- 200 customers, 1,000 girvis, 2,000 items and 1,000 payments deterministic fingerprint stress case.
- Large-dataset one-payment delta remains limited to one girvi subtree.

## Safety boundary

- Encrypted snapshot remains authoritative.
- Relational sync failure cannot modify the encrypted source records.
- No read/write cutover is authorized.
- SQLite sensitive text remains individually AES-GCM encrypted.
- `.gkb` continues to back up authoritative business snapshot and masters, not the rebuildable shadow database.

## Remaining

- Device-executed rollback simulation and evidence flag.
- Temporary isolated-database stress benchmark.
- Row-level write path from business use cases.
- Dedicated relational IDs for unit, locker, plan and payment-mode masters.
- Owner-approved dual-read observation period and rollback window.
