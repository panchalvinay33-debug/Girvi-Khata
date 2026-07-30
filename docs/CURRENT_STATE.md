# Girvi Khata — Current Project State

Last updated: 2026-07-30

This file is the quickest authoritative summary of the repository. Detailed history remains in milestone and release-evidence files.

## Branch and merge status

- Development branch: `agent/initial-foundation`
- Pull request: draft PR #1
- `main` has not been updated or merged.
- No milestone may enter `main` without explicit owner approval after physical-device testing.
- Repository is currently public; never commit OAuth credentials, production signing material, real customer records, recovery phrases or `.gkb` files.

## Owner-approved baseline

- Latest explicitly owner-approved baseline: `v0.14.0-alpha.14`.
- Alpha 14 approved Owner Settings, configurable auto-lock, biometric toggle and category rename/reorder scope.
- Later Alpha 15–19 builds are verified testing builds but still require owner physical-device approval.

## Latest verified testing build

- Build: `v0.19.0-alpha.19`
- Version code/name: `19` / `0.19.0-testing`
- Package: `com.girvikhata.app.testing`
- Source commit: `af0cee890e28403ae511e0188baff8a3c2b8b5d9`
- Android workflow: `30534642656`
- Security Guard: `30534642640`
- Artifact ID: `8756202821`
- APK size: `20,370,122 bytes`
- APK SHA-256: `e006e07f2d4edede4110dd6f420071b73d8022f809e7000dc38dfb473c2cbece`
- CI status: unit tests, Android/Compose compilation, stable testing signing, artifact upload, Security Guard, ZIP integrity and APK integrity passed.
- Owner status: physical update and relational-shadow verification pending.

## Current visible application scope

### Main business work

- PIN enrollment/unlock, biometric unlock and authenticated PIN recovery
- Configurable immediate/30-second/1-minute/5-minute background auto-lock
- Customers, categories and duplicate-mobile safeguards
- Single-item, classic master-assisted and advanced multi-item girvi creation
- Item, Unit, Interest Plan, Payment Mode and Locker masters
- Interest preview, manual interest adjustments with mandatory reason and settlement calculation
- Payment receive with interest-first, principal-first and exact custom allocation
- Immutable payment history, linked reversal, release blocking and release metadata
- Customer khata, portfolio/collection reports, custom dates and CSV/text sharing
- Settlement/release receipts

### Internal Tools

- Data Safety Status with encrypted hash-chained journal and backup-due state
- Database Migration Status with relational counts, fingerprints, mirror time and rebuild action
- Owner Settings and Business Masters
- Advanced Multi-Item & Custom Split workflow plus classic fallback
- Settlement & Release Center
- Reports & Customer Khata
- Portable encrypted `.gkb` backup with business records and master catalog
- Android Files/Drive direct save plus same-URI read-back, byte, SHA, decrypt and payload verification
- Strict restore preview, legacy-backup compatibility and combined pre-restore safety package
- Data-preserving PIN recovery
- During local corruption, Reports and new-backup creation are blocked while Restore/PIN Recovery remain available
- `MainActivity` remains the only exported launcher activity

## Current storage and security

- Authoritative business records remain an app-private AES-256-GCM encrypted snapshot protected by Android Keystore.
- Snapshot schema is v3.
- Normal saves validate relationships, create rotating encrypted safety copies, fsync a temporary file and perform decrypt/read-back verification before and after replacement.
- Latest five safety copies and latest two damaged-file quarantine copies are retained.
- If no valid local copy remains, normal business UI is blocked until verified `.gkb` restore.
- A separate AES-256-GCM encrypted journal stores up to 500 SHA-256 hash-chained events.
- Backup becomes due after no verified external package, five committed changes or seven elapsed days.
- Portable backups use AES-256-GCM with PBKDF2-HMAC-SHA256 at 310,000 iterations and authenticated tamper detection.
- Recovery phrases, raw PINs and production secrets are never persisted in business records or journal entries.
- Android automatic backup/device transfer remains disabled.
- App-wide `FLAG_SECURE` protection is applied where supported.

## Alpha 19 relational migration foundation

- A transactional SQLite relational shadow contains customers, categories, girvis, items, payments and metadata.
- Foreign keys, unique constraints, indexes and write-ahead logging are enabled.
- Sensitive text cells are individually AES-256-GCM encrypted with field-specific associated data.
- UUID links, amounts, timestamps and status values remain relational columns inside the app sandbox.
- Every committed encrypted snapshot triggers a complete shadow rebuild inside one SQLite transaction.
- Shadow failure never replaces or rolls back the authoritative encrypted snapshot.
- Verification requires exact row counts, stored deterministic semantic fingerprint and fingerprint reconstructed by decrypting database rows.
- Legacy item rows use deterministic IDs for repeatable migration fingerprints.
- Normal app reads/writes have not switched to SQLite; cutover remains blocked.

## Important limitations

- Alpha 15–19 workflows require owner physical-device confirmation.
- SQLite is currently a verified shadow rather than the source-of-truth.
- The SQLite file is not whole-file SQLCipher encrypted; sensitive text cells are encrypted individually.
- Automatic mirror currently rebuilds the complete shadow instead of applying row-level deltas.
- Dedicated unit/locker/plan relational IDs are not yet normalized out of schema-compatible item descriptions.
- Same-URI read-back cannot prove later remote-cloud synchronization or cross-device retention.
- Tools remains an internal activity rather than a fully unified bottom-navigation page.
- Google Drive API authorization/automatic backup, PDF/thermal printing, encrypted media vault, production signing and pilot rollout remain pending.

## Next priority order

1. Owner-test Alpha 19 as an in-place update and verify Database Migration Status on real records.
2. Add row-level relational transaction APIs and rollback/failure-injection tests.
3. Add large-data, interrupted-write, low-storage and repeated consistency stress tests.
4. Normalize unit, locker, interest-plan and payment-mode IDs into dedicated relational columns.
5. Run dual-read comparisons before any owner-approved read cutover.
6. Make the repository private before Drive OAuth or production signing work.
7. Add PDF/thermal printing and encrypted media-vault milestones after storage cutover.

## Permanent delivery rule

`development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
