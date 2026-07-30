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

- Build: `v0.13.0-alpha.13`
- Version code/name: `13` / `0.13.0-testing`
- Package: `com.girvikhata.app.testing`
- Source commit: `e3a745c1e376f08f5d929fc8f42502dc96db70e9`
- Android workflow: `30514393001`
- Security Guard: `30514393005`
- Artifact ID: `8748366094`
- APK size: `20,091,418 bytes`
- APK SHA-256: `7f6d42226dfe973f068f4027749b4a112fec823b8d6a31131ee613b13dda92ca`
- CI status: unit tests, Compose/Android compilation, stable testing signing, artifact upload, Security Guard, ZIP integrity and APK integrity passed.
- Owner status: Alpha 10 navigation/privacy, Alpha 11 local-store recovery, Alpha 12 safety journal and Alpha 13 external document verification require physical-device confirmation.

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
- Encrypted hash-chained activity journal, backup-due state and latest externally verified package metadata
- Reports and customer khata
- Active/released/all filters
- Today, 7-day, 30-day, all-time and exact custom From/To collection ranges
- CSV, receipt and customer-statement sharing
- Portable encrypted `.gkb` backup creation with direct Files/Drive document save and same-URI read-back verification
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
- Backup becomes due after no verified external package, five committed changes, or seven elapsed days.
- Portable backups use AES-256-GCM with PBKDF2-HMAC-SHA256 at 310,000 iterations, random salt/nonce and authenticated tamper detection.
- Alpha 13 writes the package through Android `CreateDocument`, reads the same URI back, requires exact byte equality, decrypts the read-back package and verifies schema plus complete payload before recording SHA/count metadata.
- Picker cancellation or any provider/write/read/decrypt/schema/payload failure leaves backup-due state unchanged.
- Recovery phrase characters are not persisted and are overwritten after the pending operation ends.
- Restore validates schema, IDs, customer/girvi links, payment reconciliation, status values, timestamps, counts and package integrity before replacement.
- Android automatic backup/device transfer remains disabled.
- A central lifecycle guard applies Android `FLAG_SECURE` to every activity window where supported.
- Only `MainActivity` is exported; Tools, Safety, Reports, Backup, Restore and PIN Recovery are internal activities.
- No developer master key, admin backdoor, central business database, ads or analytics exists.

## Important limitations

- Alpha 10 privacy behavior, Alpha 11 fault recovery, Alpha 12 observer/journal behavior and Alpha 13 provider compatibility require owner physical-device confirmation.
- Committed business events are aggregate state-change records rather than exact field-level transaction labels.
- Same-URI read-back proves the Android document provider returned the exact bytes at verification time, but cannot prove later remote-cloud synchronization or cross-device retention.
- Process death while the document picker is open cancels the in-memory pending backup operation; the phrase is deliberately not persisted.
- Tools currently opens as an internal activity through a floating button rather than a native bottom-navigation page.
- Persistence is still an encrypted snapshot file, not the final transactional encrypted relational database.
- Local safety/quarantine/journal files are device-bound and disappear on uninstall or device loss; an external `.gkb` remains mandatory.
- Google Drive API authorization, automatic upload/read-back verification, retention, scheduling and cloud restore discovery are not implemented.
- PDF/thermal printing, media vault, final receipt templates and production signing are pending.

## Next priority order

1. Owner test Alpha 13 as an in-place update, including Alpha 10/11/12 regression checks.
2. Confirm cancellation does not reset backup status and phone Files plus Google Drive document providers pass same-URI read-back verification.
3. Fix any physical-device/provider regression before advancing the approved baseline.
4. Replace the snapshot store with a transaction-safe encrypted relational database and exact transaction audit events.
5. Make the repository private, then implement owner-authorized Google Drive app-data backup with API upload/read-back verification, retention and restore discovery.
6. Add production receipt/PDF/thermal printing and media-vault milestones after storage migration.

## Permanent delivery rule

`development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
