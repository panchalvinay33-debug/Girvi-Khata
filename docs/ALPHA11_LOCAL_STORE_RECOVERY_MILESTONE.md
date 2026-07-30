# Alpha 11 — Resilient Local Store Recovery

Status: CI pending; owner physical test required.

## Purpose

Prevent a damaged encrypted local store from silently appearing as an empty khata.

## Implemented

- Silent corruption fallback removed from the local encrypted record store.
- Every normal save validates snapshot relationships and schema before encryption.
- Existing valid primary is copied to a rotating encrypted pre-save safety set.
- Latest five local encrypted safety copies are retained.
- New primary is written to a temporary file, fsynced, decrypted, decoded and compared before replacement.
- Final primary is decrypted/read back and compared after replacement.
- Envelope magic, format, IV length, ciphertext length, trailing bytes and total file size are validated before allocation/decryption.
- If primary fails, newest safety copies are tried in order.
- A valid safety copy is promoted to primary; damaged primary is quarantined.
- Latest two damaged encrypted files are retained for diagnosis/recovery work.
- If every copy fails, the app shows Data Recovery Required instead of Dashboard/default records.
- Main recovery screen links directly to verified `.gkb` restore and supports retry after restore.
- Tools disables Reports and new-backup creation while local records are corrupt; Restore and PIN Recovery remain available.
- Verified restore can quarantine a fully corrupt primary and install the validated portable snapshot.
- Valid pre-restore data still receives a portable encrypted safety backup.
- Pre-restore file bytes and restored primary counts are verified.
- Pure policy tests cover envelope bounds, retention order, duplicate girvi numbers and missing customer relationships.

## Physical test order

1. Install as an update over Alpha 10; do not uninstall.
2. Verify current PIN, customers, girvi, payments, reports and backup/restore remain.
3. Create/edit/payment operations and restart the app after each to exercise verified saves.
4. Confirm no unexpected empty khata appears.
5. Create an external `.gkb` backup before any destructive fault simulation.
6. Fault injection should only be done with disposable dummy records or a dedicated test device/file tool.
7. On a deliberately damaged primary, confirm a valid local safety copy auto-recovers.
8. If all local copies are damaged, confirm Dashboard is blocked and Data Recovery Required appears.
9. Confirm Tools disables Reports and Backup but leaves Restore and PIN Recovery enabled.
10. Restore the external `.gkb`, return to main screen and tap retry; records must return.
11. Confirm no screenshot/recent-preview regression and only one launcher icon remains.

## Honest limitations

- This remains an encrypted snapshot-file store, not the final transactional relational database.
- Local safety copies are device-bound to Android Keystore and disappear on uninstall/device loss.
- External portable `.gkb` backup remains mandatory.
- Automatic Google Drive upload/read-back verification is still pending.
- Fault recovery needs owner physical-device testing before approval.
