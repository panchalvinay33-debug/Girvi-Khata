# Backup, Encryption, and Restore Blueprint

Last updated: 2026-07-29

## Privacy boundary

- Business data is never stored in a Girvi Khata company database.
- Primary records stay in app-private storage on the owner's Android phone.
- Owner-selected storage or Google Drive may receive only client-side encrypted packages.
- No developer master key, universal PIN, hidden admin login, remote viewer, or remote-delete channel exists.

## Current implemented local storage

- App-private encrypted snapshot file.
- AES-256-GCM encryption with an Android Keystore-protected device key.
- Authenticated binary envelope and schema v3 JSON payload.
- Schema v3 contains customers, categories, multiple girvi items, payments, allocations, reversals, manual-interest adjustments, and release metadata.
- Writes use a temporary file and replacement approach.

### Current limitation

This is an interim testing store, not the final production database. Stable production requires transaction-safe encrypted relational storage, robust migrations, and explicit corrupt-store recovery. An unreadable local store must never silently become an empty working ledger in production.

## Implemented portable `.gkb` backup

- Complete business snapshot serializer.
- AES-256-GCM authenticated encryption.
- Recovery key derived from owner passphrase by PBKDF2-HMAC-SHA256 with 310,000 iterations.
- Random 16-byte salt and 12-byte nonce per package.
- Versioned package header, schema version, creation time, KDF settings, bounded ciphertext length, and trailing-data rejection.
- Minimum recovery phrase: 12 characters with letters and digits.
- Wrong passphrase and tampered/corrupt package use a safe failure path.
- Package limit: 128 MB.
- Temporary shared backup files remain in app-private cache and receive only temporary read permission through FileProvider.
- Recovery phrases are not persisted in preferences, logs, source, analytics, or package metadata.

## Implemented backup contents

- Customers, mobile numbers, addresses, and creation times
- Categories and active status
- Girvi IDs/numbers, customer links, status, principal, rate, dates
- Multiple items, quantity, category/item names, gross/deduction weight, descriptions
- Payment receipt numbers, amounts, principal/interest/charges allocations, mode, note, time
- Reversal markers and original-payment links
- Manual-interest adjustments
- Release time and release note

## Implemented restore flow

1. Require existing app PIN.
2. Select `.gkb` package through Android document picker.
3. Request recovery passphrase.
4. Verify package structure and authenticated encryption.
5. Decode the snapshot into temporary memory.
6. Reject unsupported schema, malformed payload, duplicate IDs/numbers, missing customer links, invalid quantities/timestamps/status, or unreconciled payments.
7. Show customer/category/girvi/payment counts, creation time, and payload SHA-256 preview.
8. Require explicit destructive confirmation.
9. Encrypt the current snapshot into app-private pre-restore safety storage.
10. Save the imported snapshot into the device-protected encrypted store.
11. Reload saved records and verify customer, girvi, and immutable payment-entry counts.
12. Report success only after read-back verification.

## Pre-restore safety storage

- Current snapshot is protected with the same recovery passphrase before replacement.
- Stored in app-private `restore_safety` storage.
- Latest three safety packages are retained.
- These copies are not a substitute for an external backup because uninstalling the app removes app-private files.
- A future milestone must add visible safety-copy management and rollback.

## PIN recovery and backup separation

- The six-digit app PIN never encrypts portable backups.
- PIN recovery may replace the local PIN verifier and lockout state only after strong biometric or device-credential authentication.
- PIN recovery must not intentionally rewrite customers, girvi, payments, reports, or backup packages.
- Recovery phrase and device credential remain separate security factors.

## Export and reporting rules

- Reports use the effective ledger: an original payment with a linked reversal is excluded from collection totals.
- Reversal records remain in immutable audit history and backup counts.
- CSV, receipt, statement, and backup files are generated from in-memory decrypted records and written only to app-private temporary locations until explicitly shared.
- No export is silently written to public Downloads or uploaded.

## Planned Google Drive architecture

Google Drive is backup storage, not a live database.

1. Make repository Private before adding credentials.
2. Authorize the owner's Google account with the minimum suitable app-data scope.
3. Freeze a consistent local snapshot.
4. Create a versioned encrypted package and manifest.
5. Upload to the owner's Drive app-data area.
6. Read metadata/content back.
7. Verify size, hashes, authenticated package decryptability, schema, and expected counts.
8. Mark backup verified only after all checks pass.
9. Retain older verified backup until a newer package is fully verified.

## Planned backup triggers

- Critical queued backup after new girvi, payment, reversal, release, manual financial adjustment, restore, or recovery-setting change.
- Daily WorkManager backup with bounded exponential backoff and appropriate battery/network constraints.
- Manual backup always available.

## Planned retention

- Last 10 critical transaction backups
- Last 7 daily backups
- Last 4 weekly backups
- Last 6 monthly backups

A previous verified backup is never deleted until a newer package passes complete verification.

## Failure handling

- Wrong passphrase: reveal no partial business data and do not touch current records.
- Corrupt/tampered package: reject and retain current records.
- Unsupported schema: reject until a tested migration exists.
- Validation mismatch: reject before replacement.
- Interrupted restore: current or pre-restore safety data must remain recoverable.
- Post-save count mismatch: restore is not considered successful.
- No internet or Drive failure: local app continues working; pending cloud state is shown later.
- Drive full: never delete local records.
- Wrong Google account: show no compatible backups without searching unrelated accounts.

## Secret-management rules

Never commit OAuth secrets, `google-services.json`, signing keystores/passwords, recovery phrases, encryption keys, real databases/backups/exports/media, or credential-bearing configuration.

## Verification definition

### Portable local backup

Creation succeeds only after serialization and authenticated encryption complete. Restore succeeds only after authenticated decrypt, strict decode/validation, explicit confirmation, encrypted save, and read-back count verification.

### Future cloud backup

A successful upload is not a successful backup. Success requires upload, read-back, authenticated package checks, SHA-256/manifest verification, and expected record/media counts.
