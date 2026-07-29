# Girvi Khata — Current Project State

Last updated: 2026-07-30

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

- Build: `v0.9.0-alpha.9`
- Version code/name: `9` / `0.9.0-testing`
- Package: `com.girvikhata.app.testing`
- Source commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- Android workflow: `30480566880`
- Security Guard: `30480566699`
- Artifact ID: `8735582885`
- APK size: `20,025,854 bytes`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`
- CI status: unit tests, Compose/Android compilation, stable testing signing, artifact upload, Security Guard, ZIP integrity, and APK integrity passed.
- Owner status: Alpha 8 PIN recovery/restore and Alpha 9 customer/date workflows remain physical-device test pending.

## PIN regression correction

- Owner reported that the previously configured PIN was not accepted after Alpha 7.
- No package-ID or preference-key migration change was found in source.
- Alpha 8 and later include data-preserving PIN recovery through strong biometric or device credential authentication.
- PIN recovery replaces only the PIN verifier and lockout state; business records are not intentionally modified.
- Alpha 7 is superseded.

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
- Customer profile with mobile/address
- Customer name/mobile/address editing with encrypted persistence
- Duplicate-mobile rejection and linked-girvi name propagation
- Deletion only for customers with no girvi history
- Active/released/all filters
- Today, 7-day, 30-day, all-time, and exact custom From/To collection ranges
- CSV, receipt, and customer-statement sharing
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
- App-wide screenshot/recent-app preview blocking is pending.
- PDF/thermal printing, media vault, final receipt templates, and production signing are pending.
- Alpha 8/9 workflows require owner physical-device testing before approval.

## Next priority order

1. Owner test Alpha 9 as an in-place update, including PIN recovery, backup/restore, customer editing and custom dates.
2. Fix any physical-device regression before advancing the approved baseline.
3. Merge Reports/Backup/Restore/PIN Recovery into one in-app navigation structure and remove the second launcher entry.
4. Add app-wide privacy-screen protection and explicit corrupt-local-store recovery.
5. Replace the interim snapshot store with a transaction-safe encrypted database.
6. Make the repository private, then implement owner-authorized Google Drive app-data backup with upload/read-back verification and retention.

## Permanent delivery rule

`development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
