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

- Latest owner-approved functional baseline: `v0.9.0-alpha.9`.
- Alpha 9 includes the Alpha 8 PIN recovery/restore scope plus customer profile editing, duplicate-mobile protection, safe unused-customer deletion and exact custom collection dates.
- Alpha 7 remains superseded because the previous PIN was not accepted on the owner's device.

## Latest testing build

- Build: `v0.10.0-alpha.10`
- Version code/name: `10` / `0.10.0-testing`
- Package: `com.girvikhata.app.testing`
- Source commit: `fad678462cf801e682acf6565fccf4978248fde7`
- Android workflow: `30482752429`
- Security Guard: `30482752531`
- Artifact ID: `8736432865`
- APK size: `20,025,846 bytes`
- APK SHA-256: `a50da4c6c678723f5d9b0284d8423d97af009823266564b0dbc11378e2f9ed60`
- CI status: unit tests, Compose/Android compilation, stable testing signing, artifact upload, Security Guard, ZIP integrity and APK integrity passed.
- Owner status: Alpha 10 single-launcher/privacy physical-device test pending.

## Current visible application scope

### Main app

- PIN enrollment/unlock and biometric unlock
- Data-preserving authenticated PIN recovery through Tools
- 30-second background auto-lock
- Customers, categories, girvi records, multiple items, weights and search
- Customer profile/edit with duplicate-mobile protection and history-safe deletion
- Interest preview and settlement calculation
- Payment receive with interest-first, principal-first or custom allocation
- Cash, UPI and bank modes
- Immutable payment history and linked reversal
- Release blocking, owner override and release metadata
- Floating Tools entry for reports, backup, restore and security

### Internal Tools

- Reports and customer khata
- Active/released/all filters
- Today, 7-day, 30-day, all-time and exact custom From/To collection ranges
- CSV, receipt and customer-statement sharing
- Portable encrypted `.gkb` backup creation
- Verified `.gkb` restore/import
- Data-preserving PIN recovery
- Tools is no longer a separate launcher icon; `MainActivity` is the only exported launcher activity.

## Current storage and security

- Local business records are stored in an app-private AES-256-GCM encrypted snapshot protected by Android Keystore.
- Snapshot schema is v3.
- Portable backups use AES-256-GCM with a recovery-passphrase key derived by PBKDF2-HMAC-SHA256 at 310,000 iterations.
- Portable packages use random salt and nonce and authenticated tamper detection.
- Restore validates schema, IDs, customer/girvi links, payment reconciliation, status values, timestamps, counts and package integrity before replacement.
- Restore creates an app-private pre-restore safety backup and performs post-save read-back count verification.
- Android automatic backup/device transfer remains disabled.
- A central application lifecycle guard applies Android `FLAG_SECURE` to every activity window to block screenshots, ordinary screen recording and readable recent-app previews where supported.
- Only `MainActivity` is exported; Tools, Reports, Backup, Restore and PIN Recovery are internal activities.
- No developer master key, admin backdoor, central business database, ads or analytics exists.

## Important limitations

- Alpha 10 privacy behavior and old Tools-icon removal still require owner physical-device confirmation; OEM launcher/capture behavior can differ.
- Tools currently opens as an internal activity through a floating button rather than a native bottom-navigation page.
- Persistence is still an interim encrypted snapshot file, not the final transactional encrypted relational database.
- Corrupt local-store loading can still fall back to defaults; production must instead expose explicit recovery without silent replacement.
- Google Drive automatic upload, account authorization, read-back verification, retention, scheduling and cloud restore discovery are not implemented.
- PDF/thermal printing, media vault, final receipt templates and production signing are pending.

## Next priority order

1. Owner test Alpha 10 as an in-place update: one launcher icon, Tools access, privacy capture blocking and auto-lock regression.
2. Fix any physical-device issue before advancing the approved baseline.
3. Add explicit corrupt-local-store detection/recovery so unreadable records never silently appear as an empty ledger.
4. Replace the interim snapshot store with a transaction-safe encrypted relational database and migration tests.
5. Add critical-change backup queue/status and local verified-backup history.
6. Make the repository private, then implement owner-authorized Google Drive app-data backup with upload/read-back verification and retention.

## Permanent delivery rule

`development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
