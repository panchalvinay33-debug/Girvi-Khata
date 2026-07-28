# Backup, Encryption, and Restore Blueprint

Last updated: 2026-07-28

## Privacy boundary

- Business data is never stored in a Girvi Khata company database.
- The primary database and media remain in app-private storage on the owner's Android device.
- Google Drive receives only client-side encrypted packages.
- There is no developer master key, universal PIN, hidden admin login, or remote data viewer.

## Storage layers

1. **Local database** — structured customer, girvi, item, payment, calculation, settings, and audit records.
2. **Encrypted media vault** — item photos, scale photos, optional customer photos, documents, signatures, and receipts.
3. **Owner's Google Drive** — encrypted backup packages, encrypted recovery-key envelope, manifests, and retention metadata.

## Key hierarchy

- `MasterDataKey`: random 256-bit key used to protect database/media backup material.
- `DeviceWrappingKey`: non-exportable Android Keystore key used to wrap the local master key.
- `RecoveryWrappingKey`: derived from the owner's strong recovery passphrase using a memory-hard KDF.
- No raw key or passphrase is written to logs, preferences, analytics, source code, or Drive.

## Backup package pipeline

1. Freeze a consistent database snapshot.
2. Build an encrypted manifest with schema version and record counts.
3. Stream database and media into chunks.
4. Compress before encryption where appropriate.
5. Encrypt each chunk with authenticated encryption and a unique nonce.
6. Calculate integrity hashes.
7. Upload to the owner's Drive app-data space.
8. Read metadata back and verify size/hash/manifest decryptability.
9. Mark the backup verified only after all checks succeed.

## Backup triggers

Critical queued backup after:

- New girvi saved
- Payment recorded or reversed
- Girvi released
- Security or recovery settings changed

Scheduled backup:

- Daily with WorkManager
- Prefer Wi-Fi and sufficient battery
- Retry with bounded exponential backoff

Manual backup:

- Always available from Backup & Security

## Retention proposal

- Last 10 critical transaction backups
- Last 7 daily backups
- Last 4 weekly backups
- Last 6 monthly backups

A previous verified backup is never deleted until a newer backup passes full verification.

## Restore flow

1. Install app and select the same Google account.
2. Authorize minimum Drive access.
3. Discover compatible backups.
4. Show shop name, date, app/schema version, counts, and size without exposing record content.
5. Request recovery passphrase.
6. Verify package integrity before writing local data.
7. Decrypt into a temporary private location.
8. Run database migrations.
9. Verify record/media counts.
10. Atomically replace local data.
11. Generate a new device wrapping key.
12. Run a post-restore test backup.

## Failure handling

- No internet: continue offline and show pending backup count.
- Drive full: show actionable error without deleting local data.
- Wrong account: show no-backup message; never search other accounts.
- Wrong passphrase: rate limit attempts; do not reveal partial information.
- Corrupt package: keep current local data untouched and offer older verified versions.
- Interrupted restore: use temporary files and atomic swap; never leave half-restored state.

## Secret-management rules

Never commit:

- `google-services.json`
- OAuth client secrets
- Signing keystores or passwords
- Recovery phrases
- Encryption keys
- Real `.db`, `.enc`, `.backup`, `.zip`, image, PDF, or export files
- Local configuration containing credentials

## Verification definition

A cloud upload is not a successful backup. A backup is successful only when upload, metadata read-back, integrity verification, and manifest decryption test all pass.
