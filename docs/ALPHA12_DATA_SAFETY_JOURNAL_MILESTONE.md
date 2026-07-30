# Alpha 12 — Data Safety Journal and Backup Readiness

Status: development branch implementation; owner physical test pending.

## Visible scope

- PIN-protected `Data Safety Status` screen inside the internal Tools hub.
- Business encrypted-store health: verified, recovered from a local safety copy, or recovery required.
- Separate AES-256-GCM app-private safety journal protected by Android Keystore.
- SHA-256 hash chaining across journal entries.
- Last locally verified encrypted backup-package timestamp and package SHA-256.
- Committed changes since the last verified package.
- Backup due when no package has been verified, five committed changes occurred, or seven days elapsed.
- Latest 100 journal events visible; up to 500 retained.

## Commit observation

A process-lifetime Android `FileObserver` watches only the committed primary business file `business_records_v1.bin`. Temporary files, rotating safety copies, quarantine files, and the journal file itself are ignored. After a committed replacement it:

1. waits briefly for the file operation to settle;
2. hashes the encrypted primary file;
3. deduplicates repeated file-system callbacks;
4. loads the strict verified business snapshot;
5. appends an encrypted hash-chained aggregate commit event.

Alpha 12 records an aggregate committed-state event with customer/girvi/ledger counts. Exact field-level transaction labels require the future transactional database layer and are not claimed here.

## Backup verification

Before opening Android share:

- the portable `.gkb` package is encrypted;
- the package is decrypted in-memory with the supplied recovery phrase;
- payload bytes and schema are compared;
- package SHA-256 and counts are recorded in the safety journal;
- changes-since-backup resets to zero.

This verifies the generated encrypted package. It does not prove that the user completed an external Files/Drive save after the Android share chooser opened.

## PIN recovery

A successful authenticated PIN recovery appends a separate `PIN_RECOVERED` journal event. Journal failure does not roll back a successfully saved PIN or business snapshot; the Safety screen exposes journal verification failure instead of treating business data as missing.

## Security boundaries

- Safety activity is internal (`exported=false`).
- Viewing status requires the existing app PIN.
- Raw PIN, recovery phrase, customer record contents, and plaintext backup payload are not written to the journal.
- Business records and the journal remain separate encrypted files, avoiding a shared corruption domain.

## Automated checks

- SHA-256 determinism and content sensitivity.
- Backup due when never created.
- Backup due at five committed changes.
- Recent verified backup with fewer changes remains current.
- Backup due after seven days.
- Existing accounting, reporting, backup, restore, recovery, and storage tests remain mandatory.

## Physical test checklist

1. Install over Alpha 11 without uninstalling.
2. Confirm PIN, customers, girvi, payments, reports, backup, restore and single launcher remain.
3. Open Tools → Data Safety Status and verify PIN protection.
4. Confirm business store and journal show healthy.
5. Make one dummy customer/girvi/payment change, wait briefly, refresh Safety Status and confirm changes increment.
6. Create a portable backup, save it externally, return and refresh; changes should reset to zero and SHA/timestamp appear.
7. Make five additional dummy committed changes and confirm Backup Due appears.
8. Test authenticated PIN recovery and confirm a PIN recovery journal entry appears.
9. Confirm screenshots/recent-app previews remain blocked.
10. Report missing events, duplicate floods, wrong counts, backup status errors, journal failure, crashes, or data loss.
