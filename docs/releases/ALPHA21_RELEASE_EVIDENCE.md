# Alpha 21 Verified Release Evidence

Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING

- Version code/name: `21` / `0.21.0-testing`
- Package: `com.girvikhata.app.testing`
- Exact source commit: `eec78e0aba6a8d168baeb09959fe93e2fd85733f`
- Android workflow: `30543127987`
- Security Guard: `30543128181`
- Artifact ID: `8759651865`
- Artifact digest: `sha256:584432831011b866850bc6db62c65a1d2f884907a7cabeca16a2ccf0890c7157`
- APK size: `20,402,894 bytes`
- APK SHA-256: `65a5f56771c120b9f11e102e0eeaea7c086544c7a381a781879cf9c43ebead12`

## Verified scope

- Device-local rollback simulation deliberately fails the relational transaction immediately before commit.
- Post-failure relational counts and semantic fingerprint must remain equal to the verified baseline.
- Device-local benchmark records full rebuild and no-change dual-read verification timings.
- Diagnostics are blocked below 64 MiB free or below three times estimated relational database size.
- Diagnostic proof persists locally without customer data.
- Database Migration Status shows rollback, benchmark, free space, timings and additional cutover blockers.
- Schema-neutral resolver maps legacy Item/Unit/Locker description metadata to stable master IDs when a matching master exists.
- Unknown or manual metadata safely remains unresolved.
- Snapshot remains authoritative and database cutover remains unavailable.

## Automated evidence

- Space-policy tests passed.
- Master-link resolver tests passed.
- All previous accounting, multi-item, custom split, backup, restore, corruption recovery, safety journal, relational fingerprint, delta planner and stress tests passed.
- Android/Compose compilation passed.
- Stable testing signing and artifact upload passed.
- Security Guard passed.
- Artifact ZIP and APK archive integrity passed.

## Owner test order

1. Install over Alpha 20 without uninstalling.
2. Confirm PIN, fingerprint, customers, girvi, payments, masters and reports.
3. Open Tools → Database Migration Status and verify PIN protection.
4. Run Full Rebuild & Verify.
5. Run Device Rollback & Benchmark Proof.
6. Confirm Rollback Simulation = VERIFIED.
7. Confirm Benchmark = VERIFIED and Storage Headroom = SAFE.
8. Note full rebuild and no-change timings.
9. Refresh, close and restart; diagnostic result should persist.
10. Create a disposable customer, multi-item girvi and payment; verify incremental dual-read remains healthy.
11. Create a verified external `.gkb` backup.
12. Do not approve relational cutover yet.

## Known limitations

- Diagnostic benchmark uses the owner's current dataset, not a destructive synthetic production-scale database.
- Low-storage failure is prevented by a preflight gate; true device-full behavior still requires controlled test hardware.
- Master IDs are resolved in memory only; dedicated SQLite columns and foreign keys remain pending.
- SQLite is not whole-file SQLCipher encrypted; sensitive text cells remain individually AES-GCM encrypted.
- Snapshot remains the only source-of-truth and fallback.
