# Girvi Khata — Current Project State

Last updated: 2026-07-29

This file is the quickest authoritative summary of the repository. Detailed history remains in the other ledgers.

## Branch and merge status

- Development branch: `agent/initial-foundation`
- Pull request: draft PR #1
- `main` has not been updated or merged.
- No milestone may enter `main` without explicit owner physical-device approval.
- Repository is currently public; do not add OAuth credentials, production signing material, real customer records, or real backup files until it is private.

## Owner-approved baseline

- Latest owner-approved functional baseline: `v0.4.0-alpha.4`
- Alpha 4 includes encrypted schema v3, payments, immutable reversals, settlement, release protection, and owner override.

## Latest testing build

- Build: `v0.8.0-alpha.8`
- Version code/name: `8` / `0.8.0-testing`
- Package: `com.girvikhata.app.testing`
- Source commit: `c289c31b94ed260d6a686c93ee45489f359ac628`
- Android workflow: `30477959563`
- Security Guard: `30477959615`
- Artifact ID: `8734522129`
- APK size: `19,993,086 bytes`
- APK SHA-256: `74965918d7a97c33f28faaad8df81486a0342788009d603c4de5057f984a3d96`
- CI status: unit tests, Android compilation, stable testing signing, artifact upload, and Security Guard passed.
- Owner status: physical-device testing pending.

## Alpha 7 regression and Alpha 8 correction

- Owner reported that the previously configured PIN was not accepted after installing Alpha 7.
- No package-ID or preference-key migration change was found between the earlier testing package and Alpha 7.
- Alpha 8 adds a data-preserving PIN recovery path through strong biometric or device credential authentication.
- PIN recovery replaces only the PIN verifier and lockout state; customer, girvi, category, payment, reversal, release, report, and backup data are not intentionally modified.
- Alpha 7 is superseded for further testing.

## Current visible application scope

### Main app

- PIN enrollment/unlock and biometric unlock
- 30-second background auto-lock
- Customers, categories, girvi records, multiple items, weights, search
- Interest preview and settlement calculation
- Payment receive with interest-first, principal-first, or custom allocation
- Cash, UPI, and bank modes
- Immutable payment history and linked reversal
- Release blocking, owner override, and release metadata

### Girvi Tools Test

- Reports and customer khata
- Active/released/all filters
- Collection ranges and CSV sharing
- Receipt and customer-statement sharing
- Portable encrypted `.gkb` backup creation
- Verified `.gkb` restore/import
- Data-preserving PIN recovery

## Current storage and security

- Local business records are stored in an app-private AES-256-GCM encrypted snapshot protected by Android Keystore.
- Snapshot schema is v3.
- Portable backups use AES-256-GCM with a recovery-passphrase key derived by PBKDF2-HMAC-SHA256 at 310,000 iterations.
- Portable packages use random salt and nonce and authenticated tamper detection.
- Restore validates schema, IDs, customer/girvi links, payment reconciliation, status values, timestamps, counts, and package integrity before replacement.
- Restore creates an app-private pre-restore safety backup and performs post-save read-back count verification.
- Android automatic backup/device transfer remains disabled.
- No developer master key, admin backdoor, central business database, ads, or analytics exists.

## Important limitations

- Persistence is still an interim encrypted snapshot file, not the final transactional encrypted relational database.
- Corrupt local-store loading can still fall back to defaults; production must instead expose explicit recovery without silent replacement.
- Google Drive automatic upload, account authorization, read-back verification, retention, scheduling, and cloud restore discovery are not implemented.
- Reports/Tools and the main app still use two launcher entries inside one package.
- PDF/thermal printing, media vault, final receipt templates, and production signing are pending.
- PIN recovery and destructive restore require owner physical-device testing before approval.

## Next priority order

1. Owner test Alpha 8 upgrade, PIN recovery, backup creation, and restore on dummy data.
2. Fix any physical-device regression before new scope is approved.
3. Merge Reports/Backup/Restore into one in-app navigation structure and remove the second launcher entry.
4. Add customer profile/edit/delete-safe UI and custom date picker.
5. Replace the interim snapshot store with a transaction-safe encrypted database and explicit corrupt-store recovery.
6. Make the repository private, then implement owner-authorized Google Drive app-data backup with upload/read-back verification and retention.

## Permanent delivery rule

`development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
