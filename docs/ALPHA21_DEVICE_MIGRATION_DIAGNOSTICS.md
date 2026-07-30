# Alpha 21 — Device Migration Diagnostics

Status: implementation complete; automated build verification required before sharing.

## Scope

- PIN-protected Database Migration Status now runs a real device-local rollback simulation.
- Diagnostic starts from a fully verified relational rebuild, injects a failure immediately before SQLite commit, and proves the original fingerprint remains healthy afterward.
- Diagnostic measures full rebuild and no-change dual-read verification time on the owner's actual dataset.
- A storage-headroom policy blocks diagnostics below 64 MiB free or below three times estimated relational size.
- Diagnostic result persists locally without customer data: pass/fail, timings, free bytes, timestamp and fingerprint prefix.
- Cutover blocker list explicitly includes missing device rollback and benchmark evidence.
- Schema-neutral master-link resolver extracts Unit and Locker labels plus Item Master identity from legacy description metadata. It is foundation only; dedicated database columns remain pending.

## Safety

- Encrypted snapshot remains authoritative.
- Rollback probe changes only the relational shadow transaction and deliberately fails before commit.
- No customer/business record is written to GitHub, logs or diagnostic preferences.
- Database cutover cannot be enabled from the screen.

## Tests

- Absolute free-space minimum.
- Estimated database multiplier headroom.
- Case-insensitive item/unit/locker master resolution.
- Unknown metadata safely remains unresolved.
- All existing accounting, backup, restore, relational planner and security tests remain mandatory.

## Owner test order

1. Install over Alpha 20 without uninstalling.
2. Open Tools → Database Migration Status and verify PIN protection.
3. Run Full Rebuild & Verify first.
4. Run Device Rollback & Benchmark Proof.
5. Confirm rollback, benchmark and storage headroom show verified/safe.
6. Confirm full rebuild and no-change timings are displayed.
7. Refresh and restart; diagnostic result should persist.
8. Create a disposable customer/girvi/payment and verify incremental dual-read remains healthy.
9. Keep relational cutover blocked; no owner approval is implied.
