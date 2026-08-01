# Girvi Khata — Backup, Recovery and Rollback Plan

Date: 2026-08-01
Scope: business records, configuration, ledger and private media

## 1. Objectives

- A shopkeeper must not lose the khata because of app update, phone failure, storage corruption or accidental uninstall.
- Recovery must fail closed; the app must never replace damaged data with a silently empty account.
- Every promoted build must have a tested rollback path.
- WhatsApp is not an authentication, backup or recovery dependency.

## 2. Current protection layers

### Local record protection
- Business records stored in an encrypted app-private snapshot.
- Temporary write is decrypted/read back before replacing primary.
- Existing primary is copied to rotating encrypted safety copies.
- Damaged primary can be recovered from newest valid safety copy.
- Unrecoverable corruption opens a recovery-required screen instead of an empty khata.

### Access protection
- Six-digit PIN
- Optional biometric unlock
- Auto-lock
- Owner authentication before practical-entry shortcut

### Release rollback
- Owner-approved Alpha 21 remains installed and protected.
- `baseline/alpha21-owner-approved` is permanent source rollback.
- Alpha 24 testing package uses a separate testing application ID and permanent testing certificate.
- Alpha 25 development cannot move protected refs automatically.

## 3. Backup package target

The canonical portable format remains an encrypted `.gkb` package.

Target package contents:
- Encrypted business snapshot
- Customer records
- Girvi records and item details
- Immutable payments/reversals
- Categories and owner settings allowed for export
- Customer and item photos
- Manifest

Manifest fields:
- Backup format version
- App package/version
- Local schema version
- Created-at timestamp
- Customer/girvi/item/payment/media counts
- Per-file size and SHA-256
- Overall manifest fingerprint
- Encryption/KDF metadata without secrets

## 4. Backup creation flow

1. Verify current encrypted primary can be read.
2. Verify snapshot references and uniqueness.
3. Enumerate referenced private media.
4. Detect missing and orphan media.
5. Check free storage for at least two package copies plus safety margin.
6. Write backup to a temporary app-private file.
7. Encrypt records and media.
8. Write manifest and hashes.
9. Reopen and verify the complete temporary backup.
10. Only then offer save/share through Android document picker.
11. Keep no unencrypted export residue.

If any step fails, no successful-backup message is shown.

## 5. Photo/media policy

Current Alpha 25A photos are app-private but are not yet guaranteed inside existing `.gkb` backups.

Therefore:
- Alpha 25A can be developer-tested before media backup completion.
- It must not be promoted as stable for real customer photo use until media-inclusive backup/restore passes.
- The UI should eventually show separate statuses: records protected, photos protected.
- Orphan media should be quarantined first, then deleted only after a safe retention period.

## 6. Restore flow

1. User chooses `.gkb` through document picker.
2. Copy source to app-private staging.
3. Validate size limits and file structure.
4. Validate manifest and all hashes.
5. Decrypt using the supported recovery credentials/key flow.
6. Decode into an isolated candidate snapshot.
7. Validate schema and all references.
8. Show preview: customers, girvis, payments, media, backup date and app version.
9. Check free storage.
10. Create a pre-restore safety generation of current records/media.
11. Restore into a temporary generation.
12. Read back and compare fingerprint/counts.
13. Atomically activate restored generation.
14. Preserve old generation until the app completes post-restore verification.
15. On any failure, reactivate the previous generation.

## 7. Recovery scenarios

### App record corruption
- Try rotating encrypted local safety copies.
- If none verify, block normal business writes and require `.gkb` restore.

### Wrong/corrupt backup
- Reject before changing current records.
- Explain validation failure without exposing secrets.

### App update failure
- Do not uninstall the owner-approved app.
- Testing package remains separate.
- Restore backup only into the intended package and after preview.

### Lost phone
- Install the correctly signed app on replacement phone.
- Restore the latest verified `.gkb`.
- PIN/biometric alone cannot recover data without a backup.
- Future optional Drive backup may reduce this risk but is not required for core operation.

### Forgotten PIN
- Use the existing approved PIN recovery mechanism.
- Recovery must not decrypt/export customer data without owner verification.

### Accidental entry/payment
- Never edit immutable payment history silently.
- Use reversal with reason.
- Business corrections remain visible in audit history.

## 8. Backup schedule recommendation

For a working shop:
- Manual backup at end of every business day
- Extra backup before app update, phone repair or restore operation
- Keep at least three generations on separate storage locations
- Periodically test restore on the separate testing package

Future optional automation:
- Encrypted scheduled Drive backup
- Only over user-selected conditions
- Retain offline manual export as the independent recovery path

## 9. Release backup checklist

Before installing any new testing build:
- Confirm Alpha 21 remains installed
- Export a fresh encrypted `.gkb`
- Record backup date, size and hash where available
- Confirm backup can be selected/read in restore preview
- Do not use real customer photos until media backup status is confirmed

Before approving a stable build:
- Restore records and photos on a clean test install
- Match all counts and fingerprints
- Open representative Hindi/English customers and items
- Verify active/released status and payment ledger
- Verify reports after restore
- Verify wrong password/corrupt package fails closed
- Verify low-storage restore does not damage current data

## 10. Rollback decision matrix

Rollback immediately when:
- Existing snapshot cannot load
- Customer/girvi/payment counts drop unexpectedly
- Calculation changes old accounts
- PIN gate can be bypassed
- Backup or restore verification fails
- Signing fingerprint differs

Continue testing but do not promote when:
- Cosmetic bilingual labels are imperfect
- Contact picker behavior differs by device but manual entry works
- Camera is unavailable but entry remains usable

## 11. Evidence required for promotion

- Source commit SHA
- Workflow run ID
- Unit/compile result
- Signing certificate SHA-256
- Artifact digest
- APK SHA-256
- Backup created before installation
- Restore test evidence
- Owner-phone checklist result
- Explicit owner approval
