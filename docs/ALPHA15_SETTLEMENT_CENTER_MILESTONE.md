# Alpha 15 — Settlement and Release Center

Status: development milestone; owner physical test required before approval or merge.

## Scope

- Internal PIN-protected Settlement & Release Center.
- Active and released girvi selection with settlement-month preview.
- Positive or negative manual interest adjustment.
- Non-zero amount, active-girvi and mandatory-reason validation.
- Cumulative adjustment remains in the encrypted business snapshot.
- Reason is written as a separate encrypted SHA-256 hash-chained audit event.
- Explicit reason event does not double-increment the backup-due counter; the committed snapshot observer remains the counter source.
- Shareable final settlement/release text receipt with items, principal, calculated interest, manual adjustment, effective payments, outstanding and release metadata.
- Visible critical-backup warning after adjustment and in the Tools hub.

## Compatibility

- Existing local schema and portable `.gkb` schema remain unchanged.
- Alpha 14 records install in place.
- No raw PIN, recovery phrase or plaintext complete snapshot is written to the audit event.

## Required test

- Install over Alpha 14 without uninstalling.
- Verify existing records and Alpha 14 settings.
- Open Settlement Center with wrong and correct PIN.
- Test positive and negative adjustment on disposable data.
- Confirm blank/short reason, zero amount and released-girvi adjustment reject.
- Confirm Data Safety Status shows the adjustment reason event and one committed snapshot change.
- Share active settlement preview and released receipt.
- Confirm backup-due warning and create an externally verified `.gkb` after critical changes.
