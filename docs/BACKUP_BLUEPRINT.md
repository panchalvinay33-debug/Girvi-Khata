# Backup, Encryption, and Restore Blueprint

Last updated: 2026-07-29

## Privacy boundary

- Business data is never stored in a Girvi Khata company database.
- Primary records and media stay in app-private storage on the owner's Android phone.
- Google Drive may receive only client-side encrypted packages.
- No developer master key, universal PIN, hidden admin login, remote viewer, or remote-delete channel exists.

## Current local milestone

The testing app currently uses an Android Keystore-protected AES-256-GCM encrypted snapshot store. Schema v3 contains customers, categories, multiple girvi items, payments, allocation splits, reversals, manual-interest-adjustment foundation, and release metadata. This remains an interim store; production requires a transaction-safe encrypted relational database.

## Planned storage layers

1. Transactional encrypted local database for business/audit records.
2. Encrypted media vault for item/customer/document/signature/receipt files.
3. Owner's Google Drive app-data space for encrypted packages, recovery-key envelope, manifests, and retention metadata.

## Key hierarchy

- `MasterDataKey`: random 256-bit key protecting database/media backup material.
- `DeviceWrappingKey`: non-exportable Android Keystore key wrapping the local master key.
- `RecoveryWrappingKey`: derived from the owner's recovery passphrase with a memory-hard KDF.
- Raw keys and passphrases never enter logs, preferences, analytics, source control, or Drive.

## Export and manifest rules

- Reports use the **effective ledger**: original payments that have a linked reversal are excluded from received/collection totals.
- Reversal records remain in the immutable audit history even when excluded from effective totals.
- CSV and receipt exports are generated from in-memory decrypted records and must be written only to app-private temporary files until the owner explicitly shares them.
- Export files must never be silently uploaded or left in public Downloads storage.
- A backup manifest records schema version, creation time, customer count, girvi count, payment-entry count, and SHA-256 of the encrypted payload.
- Manifest counts include immutable reversal entries; financial collection totals use only effective payments.

## Backup package pipeline

1. Freeze a consistent database snapshot.
2. Serialize records and media references into a versioned package.
3. Build a manifest with schema/app version, counts, package ID, chunk metadata, and encrypted-payload SHA-256.
4. Compress where appropriate before encryption.
5. Encrypt every chunk with authenticated encryption and a unique nonce.
6. Store a passphrase-wrapped recovery-key envelope separately inside the package.
7. Upload to the owner's Drive app-data space.
8. Read metadata/content back and verify size, hashes, chunk authentication, manifest decryptability, and record counts.
9. Mark verified only after every check succeeds.

## Backup triggers

Critical queued backup after new girvi, payment, reversal, release, manual financial adjustment, or security/recovery-setting change.

Scheduled backup: daily WorkManager job with bounded exponential backoff, preferably on Wi-Fi and sufficient battery.

Manual backup: always available from Backup & Security.

## Retention proposal

- Last 10 critical transaction backups
- Last 7 daily
- Last 4 weekly
- Last 6 monthly

A previous verified backup is never deleted until a newer package passes complete verification.

## Restore flow

1. Install app and choose the same Google account.
2. Authorize minimum Drive scope.
3. Discover compatible verified backups.
4. Show non-sensitive metadata: shop, date, app/schema version, counts, size.
5. Request recovery passphrase and rate-limit failures.
6. Verify package/chunk integrity before touching local data.
7. Decrypt into a temporary app-private location.
8. Run schema migrations and validate counts, ledger links, reversals, receipts, and media hashes.
9. Atomically replace local data.
10. Generate a new device wrapping key.
11. Run and verify a post-restore backup.

## Failure handling

- No internet: continue offline and show pending backup state.
- Drive full: show an actionable error; never delete local data.
- Wrong account: show no-backup state without searching other accounts.
- Wrong passphrase: rate-limit attempts and reveal no partial information.
- Corrupt package: keep current data untouched and offer older verified versions.
- Interrupted restore: temporary files plus atomic swap; never expose a half-restored store.
- Hash/count mismatch: backup remains unverified and is never selected automatically for restore.

## Secret-management rules

Never commit OAuth secrets, `google-services.json`, signing keystores/passwords, recovery phrases, encryption keys, real databases/backups/exports/media, or credential-bearing configuration.

## Verification definition

A successful upload is not a successful backup. Success requires upload, read-back, authenticated decryption checks, SHA-256 verification, manifest verification, and expected record/media counts.
