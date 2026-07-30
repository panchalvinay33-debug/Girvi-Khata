# Alpha 20 Release Evidence

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Version code/name: `20` / `0.20.0-testing`
- Package: `com.girvikhata.app.testing`
- Exact source commit: `f1c18a93078da52e0501e4005cf4cf2f0e7c2ad0`
- Android workflow: `30537397575`
- Security Guard: `30537397646`
- Artifact ID: `8757314648`
- Artifact digest: `sha256:8f0b77752b7565c70bf00eb92da9c59c32b0f9af3d0c1d1b7295e5e857d8c3eb`
- APK size: `20,386,506 bytes`
- APK SHA-256: `bad004270e7134cddad687085e0ccf30231252db06937b38e4d170eca3a06828`

## Verified scope

- Row-level relational delta planner for customers, categories and girvi subtrees.
- Incremental SQLite transaction sync after committed encrypted snapshot saves.
- Changed girvi replacement includes only that girvi plus its item/payment children.
- Removed records use foreign-key-safe deletion order.
- Full decrypted semantic fingerprint and row-count verification remains mandatory after every delta.
- Independent dual-read comparison reconstructs relational records and compares them with the encrypted snapshot.
- Sync mode, changed-row count and consecutive healthy-sync evidence are stored.
- Explicit cutover policy blocks migration until 25 healthy syncs, stress proof, rollback proof and owner approval.
- PIN-protected migration dashboard displays dual-read status and all blockers.
- Pure large-data stress tests cover 200 customers, 1,000 girvis, 2,000 items and 1,000 payments.
- Full unit tests, Android/Compose compilation, stable testing signing, artifact upload and Security Guard passed.
- Artifact ZIP and APK archive integrity passed.

## Owner physical test checklist

1. Install over Alpha 19 without uninstalling.
2. Verify existing PIN, fingerprint, customers, girvi, payments, masters, reports and backups.
3. Open Tools → Database Migration Status and verify PIN protection.
4. Run `Full Rebuild & Verify` once; dual-read should verify.
5. Note consecutive healthy-sync count and fingerprints.
6. Add one disposable customer and refresh after save.
7. Add one disposable multi-item girvi and refresh.
8. Post one payment and refresh.
9. Confirm sync mode becomes `INCREMENTAL` and changed rows are limited rather than the whole database.
10. Confirm snapshot and relational fingerprints match after each operation.
11. Restart the app and confirm status remains readable.
12. Check Data Safety Status for `RELATIONAL_DELTA_VERIFIED` events.
13. Create and verify an external `.gkb` backup.
14. Do not authorize relational read/write cutover yet.

## Known limitations

- Encrypted snapshot remains authoritative.
- Failure-injection points exist in code, but device-executed rollback proof is not yet marked complete.
- SQLite is not whole-file SQLCipher encrypted; sensitive text cells are individually AES-GCM encrypted.
- A changed girvi replaces its complete relational subtree rather than each child row independently.
- Large-data tests currently validate fingerprint/delta planning; isolated on-device database benchmark remains pending.
- Unit, locker, plan and payment-mode dedicated relational IDs remain pending.
- No merge to `main` is authorized.
