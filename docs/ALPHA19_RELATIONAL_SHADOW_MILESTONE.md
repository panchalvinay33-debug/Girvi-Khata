# Alpha 19 — Encrypted Relational Shadow Database

## Goal

Build and prove a transaction-safe relational representation before replacing the existing encrypted snapshot source-of-truth.

## Implemented

- Android SQLite relational shadow with foreign keys, unique constraints, indexes and WAL.
- Tables for customers, categories, girvis, items, payments and metadata.
- Sensitive text fields are individually AES-256-GCM encrypted through Android Keystore with field-specific associated data.
- UUID identifiers, foreign-key identifiers, status flags, amounts and timestamps remain relational columns.
- Every committed encrypted snapshot is mirrored by `BusinessCommitObserver` in a single SQLite transaction.
- A failed mirror never replaces or rolls back the authoritative encrypted snapshot.
- Deterministic semantic SHA-256 fingerprint covers customers, categories, girvis, items, payments, releases and accounting allocation.
- Legacy top-level item rows use deterministic `legacy-item-<girvi-id>` IDs.
- Shadow verification requires exact row counts, stored fingerprint and fingerprint reconstructed by decrypting database rows.
- PIN-protected Database Migration Status dashboard supports rebuild, refresh, counts, fingerprints and last mirror time.
- Exact journal events record relational shadow verification success or failure without recording raw PINs or backup passphrases.

## Safety boundary

Alpha 19 does not switch normal reads or writes to SQLite. The encrypted snapshot remains the only source-of-truth. Database cutover is blocked until owner-device tests, repeated consistency checks, migration rollback tests and large-data tests pass.

## Physical test order

1. Install over Alpha 18 without uninstalling.
2. Confirm existing PIN, customers, girvis, payments, reports and backups.
3. Open Tools → Database Migration Status and verify PIN protection.
4. Tap Transactional Shadow Rebuild & Verify.
5. Confirm expected/database counts match and both fingerprints match.
6. Add a disposable customer/girvi/payment, wait briefly, refresh status and verify a new mirror time and matching fingerprint.
7. Restart the app and recheck status.
8. Confirm Data Safety Status contains relational shadow success events.
9. Continue using the encrypted snapshot workflows; no database cutover is authorized.

## Known limitations

- SQLite database file itself is not whole-file SQLCipher encrypted; sensitive text cells are AES-GCM encrypted individually. Relational numeric fields and UUID links remain visible to the app sandbox.
- Automatic mirror rebuild currently rewrites the complete shadow transactionally rather than applying row-level deltas.
- The shadow is app-private and device-bound; uninstall/app-data clear removes it.
- Shadow database is not yet included in portable `.gkb` because it is reproducible from the authoritative snapshot.
- Read cutover, row-level update transactions, rollback simulation and stress testing remain pending.
- `main` remains untouched and no merge is authorized.
