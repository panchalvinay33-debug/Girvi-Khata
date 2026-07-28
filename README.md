# Girvi Khata

Privacy-first, single-owner Android application for girvi records, interest calculations, payments, receipts, and owner-controlled encrypted Google Drive backups.

> **Aapka data, aapka mobile, aapka Google Drive.**

## Project control files

- [`docs/ROADMAP.md`](docs/ROADMAP.md) — master product roadmap
- [`docs/BACKUP_BLUEPRINT.md`](docs/BACKUP_BLUEPRINT.md) — encryption, backup, and restore blueprint
- [`docs/PROGRESS.md`](docs/PROGRESS.md) — continuously updated completed/pending work ledger
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — new ideas and architecture decisions
- [`SECURITY.md`](SECURITY.md) — non-negotiable security rules

Every development change must update `docs/PROGRESS.md`. Any new feature or architecture decision must also update `docs/DECISIONS.md` and, where applicable, the roadmap or backup blueprint.

## Core constraints

- Single owner and single primary Android device
- Offline-first operation
- No central business-record database
- Local encrypted data storage
- Encrypted backup in the owner's Google Drive
- No ads, tracking SDKs, developer backdoor, or master password
- Financial records use reversal and audit history instead of silent deletion

## Technology

- Kotlin 2.3.21
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.3.1
- Android API 37 compile target
- Room-compatible persistence architecture
- Android Keystore and BiometricPrompt
- Credential Manager and Google Drive API
- WorkManager for verified background backups

## Repository safety

Never commit real customer data, signing files, OAuth secrets, backup archives, local databases, photographs, recovery phrases, or encryption keys. All examples must use dummy data.
