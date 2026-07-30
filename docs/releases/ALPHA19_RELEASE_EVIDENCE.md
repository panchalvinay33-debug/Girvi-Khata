# Alpha 19 Verified Release Evidence

**Status:** VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING

- Source commit: `af0cee890e28403ae511e0188baff8a3c2b8b5d9`
- Android workflow: `30534642656`
- Security Guard: `30534642640`
- Artifact ID: `8756202821`
- Artifact digest: `sha256:dda68cb74147fc952cc72bc99c6dcdd4c1aef1906d550feed994068178be11c2`
- Package: `com.girvikhata.app.testing`
- Version code/name: `19` / `0.19.0-testing`
- APK size: `20,370,122 bytes`
- APK SHA-256: `e006e07f2d4edede4110dd6f420071b73d8022f809e7000dc38dfb473c2cbece`

## Verified scope

- Transactional SQLite relational shadow with customers, categories, girvis, items, payments and metadata tables.
- Foreign keys, unique constraints, indexes and write-ahead logging.
- Sensitive text cells encrypted with Android Keystore AES-256-GCM and field-specific associated data.
- Every committed encrypted snapshot triggers a complete transactional shadow rebuild.
- Shadow failure cannot replace or roll back the authoritative encrypted snapshot.
- Verification requires exact row counts, stored semantic fingerprint and fingerprint reconstructed from decrypted database rows.
- Legacy item IDs are deterministic for repeatable migration fingerprints.
- PIN-protected Database Migration Status screen supports rebuild, refresh, counts, fingerprints and mirror timestamp.
- Exact relational-shadow success/failure events are written to the encrypted safety journal.
- Fingerprint tests and the full existing test suite passed.
- Android/Compose compilation, stable testing signing, Security Guard, artifact upload, ZIP integrity and APK integrity passed.

## Owner test checklist

1. Install over Alpha 18 without uninstalling.
2. Confirm existing PIN, fingerprint, customers, girvis, payments, reports, masters and backups.
3. Open Tools → Database Migration Status; wrong PIN must fail and correct PIN must unlock.
4. Tap Transactional Shadow Rebuild & Verify.
5. Confirm expected and database counts match.
6. Confirm expected and database fingerprint prefixes match.
7. Create a disposable customer, multi-item girvi and payment.
8. Wait briefly, refresh migration status and confirm mirror time/fingerprint update.
9. Restart and confirm the status remains verified.
10. Confirm Data Safety Status contains relational-shadow verification events.
11. Regression-test backup, restore, settlement and reports.
12. Do not treat the shadow as source-of-truth; no database cutover is authorized.

## Known limitations

- The SQLite file is not whole-file SQLCipher encrypted. Sensitive text fields are AES-GCM encrypted individually; UUID links, amounts, timestamps and status fields remain relational inside the app sandbox.
- Mirror writes rebuild the full shadow rather than applying row-level deltas.
- Normal app reads still use the encrypted snapshot.
- The shadow is not included in `.gkb` because it can be regenerated from the authoritative snapshot.
- Stress testing, rollback simulation, row-level transaction APIs and read cutover remain pending.
- `main` remains untouched; no merge permission was given.
