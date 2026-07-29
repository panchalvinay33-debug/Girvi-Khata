# Girvi Khata

Privacy-first, single-owner Android application for girvi records, interest calculations, payments, receipts, reports, and owner-controlled encrypted backups.

> **Aapka data, aapka mobile, aapka backup.**

## Current development state

- Latest owner-approved functional baseline: `v0.4.0-alpha.4`
- Latest CI-verified testing build: `v0.8.0-alpha.8`
- Alpha 8 owner physical test: pending
- Development branch: `agent/initial-foundation`
- `main`: not merged

Read [`docs/CURRENT_STATE.md`](docs/CURRENT_STATE.md) first for the exact build, checksum, completed scope, regression history, risks, and next priorities.

## Current visible capabilities

- PIN and biometric unlock with background auto-lock
- Data-preserving PIN recovery through biometric/device credential
- Encrypted local customers, categories, girvi records, and multiple items
- Interest calculations, settlement, payments, receipts, reversals, and release protection
- Customer khata, reports, collection filters, receipt/statement sharing, and CSV export
- Recovery-passphrase encrypted `.gkb` backup creation
- Strict backup restore/import with preview, validation, pre-restore safety copy, and read-back verification

## Project control files

- [`docs/CURRENT_STATE.md`](docs/CURRENT_STATE.md) — authoritative current snapshot
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — master product roadmap
- [`docs/BACKUP_BLUEPRINT.md`](docs/BACKUP_BLUEPRINT.md) — implemented and planned encryption/backup/restore behavior
- [`docs/PROGRESS.md`](docs/PROGRESS.md) — chronological completed/pending work ledger
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — architecture and product decisions
- [`docs/TESTING_RELEASES.md`](docs/TESTING_RELEASES.md) — APK evidence, checksums, known limitations, and owner-test status
- [`docs/DEVELOPMENT_WORKFLOW.md`](docs/DEVELOPMENT_WORKFLOW.md) — branch, APK, physical-test, approval, and merge rules
- [`SECURITY.md`](SECURITY.md) — non-negotiable security rules

## Core constraints

- Single owner and one primary Android device for the first stable release
- Offline-first operation
- No central business-record database
- Local app-private encrypted storage
- Only client-side encrypted portable/cloud backup packages
- No ads, tracking SDKs, developer backdoor, universal PIN, or master decryption key
- Financial corrections use immutable reversals rather than silent deletion
- No testing milestone enters `main` without explicit owner physical-device approval

## Current security/storage design

- Android Keystore-protected AES-256-GCM encrypted local snapshot, schema v3
- Portable AES-256-GCM backup with PBKDF2-HMAC-SHA256 recovery key derivation at 310,000 iterations
- Random salt/nonce, authenticated tamper detection, strict restore validation
- Android automatic cloud backup/device transfer disabled
- App-private temporary export and backup sharing through FileProvider

## Important limitations

- The local encrypted snapshot is an interim testing store, not the final transaction-safe encrypted relational database.
- Google Drive OAuth, appDataFolder upload, read-back verification, retention, and scheduling are not implemented.
- Main and Tools currently appear as two launcher entries inside one package.
- PDF/thermal printing, media vault, production signing, and pilot hardening are pending.
- Alpha 8 PIN recovery and restore require owner physical-device testing.

## Technology

- Kotlin and Jetpack Compose / Material 3
- Android Keystore and BiometricPrompt
- Pure Kotlin financial/reporting/validation modules
- GitHub Actions Android build, stable testing signing, and Security Guard
- Planned: transaction-safe encrypted relational persistence, WorkManager, and owner-authorized Google Drive app-data backup

## Repository safety

The repository is currently public. Never commit real customer data, OAuth credentials, `google-services.json`, production signing files/passwords, backup archives, local databases, photographs, recovery phrases, or encryption keys. All examples and tests must use dummy data.
