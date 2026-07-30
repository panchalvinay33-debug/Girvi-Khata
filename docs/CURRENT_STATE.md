# Girvi Khata — Current Project State

Last updated: 2026-07-30

This file is the quickest authoritative summary of the repository. Detailed history remains in the other ledgers.

## Branch and merge status

- Development branch: `agent/initial-foundation`
- Pull request: draft PR #1
- `main` has not been updated or merged.
- No milestone may enter `main` without explicit owner physical-device approval.
- Repository is currently public; do not add OAuth credentials, production signing material, real customer records or real backup files until it is private.

## Owner-approved baseline

- Latest owner-approved functional baseline: `v0.9.0-alpha.9`.
- Alpha 9 includes authenticated PIN recovery/restore, customer profile editing, duplicate-mobile protection, safe unused-customer deletion and exact custom collection dates.
- Alpha 7 remains superseded because the previous PIN was not accepted on the owner's device.

## Latest testing build

- Build: `v0.12.0-alpha.12`
- Version code/name: `12` / `0.12.0-testing`
- Package: `com.girvikhata.app.testing`
- Source commit: `1c028cfd8e1be02509240584e49dcf0317498df8`
- Android workflow: `30512547105`
- Security Guard: `30512547107`
- Artifact ID: `8747719885`
- APK size: `20,075,034 bytes`
- APK SHA-256: `499349cb8aa4e681a6312e2ac82c4d15485d89799f7bd58354866c746eb0c27b`
- CI status: unit tests, Compose/Android compilation, stable testing signing, artifact upload, Security Guard, ZIP integrity and APK integrity passed.
- Owner status: Alpha 10 navigation/privacy, Alpha 11 local-store recovery and Alpha 12 safety-journal workflows require physical-device confirmation.

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
- Floating Tools entry for safety, reports, backup, restore and security
- Explicit `Data Recovery Required` screen instead of silent empty records when all encrypted local copies fail

### Internal Tools

- PIN-protected Data Safety Status dashboard
- Encrypted hash-chained activity journal, backup-due state and latest verified package metadata
- Reports and customer khata
- Active/released/all filters
- Today, 7-day, 30-day, all-time and exact custom From/To collection ranges
- CSV, receipt and customer-statement sharing
- Portable encrypted `.gkb` backup creation with local decrypt/read-back verification
- Verified `.gkb` restore/import, including replacement of a quarantined damaged primary
- Data-preserving PIN recovery with a journal event
- During local corruption, Reports and new-backup creation are blocked while Restore and PIN Recovery remain available
- Tools is no longer a separate launcher icon; `MainActivity` is the only exported launcher activity

## Current storage and security

- Local business records use an app-private AES-256-GCM encrypted snapshot protected by Android Keystore.
- Snapshot schema is v3.
- Normal saves validate relationships, create a rotating encrypted pre-save safety copy, fsync a temporary file, decrypt/read back before replacement and verify the final primary.
- Latest five local safety copies and latest two damaged-file quarantine copies are retained.
- A damaged primary tries newest safety copies automatically and promotes the first verified copy.
- If no valid local copy remains, the app blocks Dashboard and requires verified `.gkb` restore.
- A separate AES-256-GCM encrypted journal stores up to 500 SHA-256 hash-chained safety events.
- Android `FileObserver` records aggregate committed business-store changes and deduplicates repeated callbacks using encrypted-file SHA/timing.
- Backup becomes due after no verified package, five committed changes, or seven elapsed days.
- Portable backups use AES-256-GCM with PBKDF2-HMAC-SHA256 at 310,000 iterations, random salt/nonce and authenticated tamper detection.
- Generated backup packages are decrypted/read back in memory before their SHA/count metadata is recorded and Android share opens.
- Restore validates schema, IDs, customer/girvi links, payment reconciliation, status values, timestamps, counts and package integrity before replacement.
- Android automatic backup/device transfer remains disabled.
- A central lifecycle guard applies Android `FLAG_SECURE` to every activity window where supported.
- Only `MainActivity` is exported; Tools, Safety, Reports, Backup, Restore and PIN Recovery are internal activities.
- No developer master key, admin backdoor, central business database, ads or analytics exists.

## Important limitations

- Alpha 10 privacy behavior, Alpha 11 fault recovery and Alpha 12 observer/journal behavior require owner physical-device confirmation.
- Alpha 12 committed business events are aggregate state-change records rather than exact field-level transaction labels.
- Local package verification does not prove the user completed an external Files/Drive save after Android share opened.
- Tools currently opens as an internal activity through a floating button rather than a native bottom-navigation page.
- Persistence is still an encrypted snapshot file, not the final transactional encrypted relational database.
- Local safety/quarantine/journal files are device-bound and disappear on uninstall or device loss; an external `.gkb` remains mandatory.
- Google Drive automatic upload, account authorization, read-back verification, retention, scheduling and cloud restore discovery are not implemented.
- PDF/thermal printing, media vault, final receipt templates and production signing are pending.

## Next priority order

1. Owner test Alpha 12 as an in-place update, including Alpha 10/11 regression checks.
2. Confirm one committed dummy change creates one journal event and backup due/reset behavior is correct.
3. Fix any physical-device regression before advancing the approved baseline.
4. Replace the snapshot store with a transaction-safe encrypted relational database and exact transaction audit events.
5. Make the repository private, then implement owner-authorized Google Drive app-data backup with upload/read-back verification and retention.
6. Add production receipt/PDF/thermal printing and media-vault milestones after storage migration.

## Permanent delivery rule

`development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
