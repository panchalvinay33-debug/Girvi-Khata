# Alpha 4 — Payment, Settlement and Release Milestone

Date: 2026-07-29

Status: VERIFIED TESTING BUILD — OWNER TEST PENDING

This file is a permanent combined progress, roadmap, backup/schema-impact, decision, and testing-release record for Alpha 4. It supplements `PROGRESS.md`, `ROADMAP.md`, `BACKUP_BLUEPRINT.md`, `DECISIONS.md`, and `TESTING_RELEASES.md` until this milestone receives owner approval.

## Build identity

- Version: `0.4.0-testing`
- Version code: `4`
- Testing package: `com.girvikhata.app.testing`
- Source commit: `6d39fbd6dec11204e340e40284659993faa612d5`
- Android workflow run: `30412018790`
- Security Guard run: `30412018797`
- Artifact ID: `8708824816`
- APK size: `19,844,926 bytes`
- APK SHA-256: `6bb19e1eefd2192fac811616db891f55cd8ac889e18937c1cbb9ac4012eb2306`

## Completed product behavior

- Receive-payment action is available from an active girvi detail screen.
- Settlement months can be selected before calculating the due balance.
- Principal due, interest due, and total outstanding are shown before payment.
- Payment allocation supports interest-first, principal-first, and custom split.
- Payment modes currently include cash, UPI, and bank.
- Optional payment note is stored with the ledger entry.
- Each payment gets a deterministic date-wise receipt number.
- Payment history shows receipt number, mode, amount, principal allocation, interest allocation, notes, and reversal state.
- Wrong payments are corrected through a new linked reversal entry; original entries are not edited or deleted.
- A payment cannot be reversed twice.
- Payment above the calculated total due is rejected.
- Girvi release is blocked while dues remain unless the owner explicitly selects override and records a release note.
- Released girvi stores status, release timestamp, and release note.
- Dashboard now shows received-payment and released-girvi summaries.
- Main Compose UI was split from the launcher activity into a modular `AppRoot.kt` to reduce future coupling.

## Accounting decisions

- Financial history is append-only during this testing milestone.
- Silent payment editing or deletion is prohibited.
- Reversal is represented by a separate linked record.
- Interest calculation months are an explicit input in the current testing flow.
- Owner override does not pretend that outstanding was paid; it records that items were released despite an outstanding amount.
- Production will require stronger audit metadata, transaction boundaries, and a relational encrypted database before real business use.

## Encrypted storage and backup impact

- Snapshot schema v3 stores payments, allocations, payment mode, notes, reversal links, manual-interest adjustment, release timestamp, and release note.
- Alpha 2 and Alpha 3 records remain readable using backward-compatible defaults.
- Existing records without payments load with an empty payment ledger.
- Business data remains AES-256-GCM encrypted in Android app-private storage.
- Android automatic cloud backup remains disabled.
- Google Drive backup is still not active.
- The backup manifest must eventually include schema version, payment count, payment ledger hash, reversal-link validation, and release-state validation.
- Restore verification must reject dangling reversal links and invalid payment allocation totals.

## Automated verification

- Unit tests passed.
- Compose compilation passed.
- Stable testing signing passed.
- APK packaging and artifact upload passed.
- Security Guard passed.
- Downloaded artifact contained exactly one APK.
- Artifact ZIP integrity passed.
- APK archive integrity passed.
- File identified as a valid Android package.

## Owner physical-test checklist

1. Install Alpha 4 directly over the installed `Girvi Khata Test` app without uninstalling.
2. Confirm the existing PIN and old customers, categories, girvi records, and multiple items remain.
3. Open an active girvi and change settlement months.
4. Record a small interest-first cash payment.
5. Confirm the receipt number and principal/interest allocation in payment history.
6. Close and reopen the app and confirm the payment persists.
7. Record a principal-first payment.
8. Test a valid custom split whose components exactly equal the entered amount.
9. Test an invalid custom split and confirm it is rejected.
10. Try a payment above total outstanding and confirm it is rejected.
11. Reverse one payment with a reason and confirm outstanding increases correctly.
12. Try reversing the same payment again and confirm it is blocked.
13. Attempt release while outstanding remains without override and confirm it is blocked.
14. Test owner-override release with a meaningful release note on test-only data.
15. Confirm a released girvi no longer offers payment entry.
16. Verify dashboard payment and released counts update.
17. Check scrolling, keyboard overlap, dialog layout, fingerprint unlock, and 30-second background lock.
18. Report every issue with screenshot and exact steps.

## Known limitations

- Alpha 3 itself still awaits explicit physical-device approval; Alpha 4 includes and supersedes its code for testing but does not count as approval.
- Interest currently uses the selected whole-month testing value rather than production date-to-date settlement rules.
- Charges entry is only exposed through custom split and there is no charges master yet.
- Receipt PDF, WhatsApp sharing, printing, and customer signature are pending.
- Payment date editing and backdated payment policy are pending.
- Partial item release is pending.
- Owner override requires a stronger adjustment/audit workflow before production.
- Storage remains an encrypted snapshot, not the final transaction-safe encrypted relational database.
- Google Drive encrypted backup and restore are pending.

## Roadmap after Alpha 4 owner test

1. Fix all Alpha 4 physical-test issues on the same branch and issue another APK if needed.
2. Record explicit owner approval before merging any milestone to `main`.
3. Add date-based settlement using actual girvi date and payment dates.
4. Add manual interest adjustment UI with mandatory reason and audit event.
5. Add receipt preview and PDF generation.
6. Add payment/release audit timeline.
7. Begin transaction-safe encrypted relational persistence and migration tests.
8. Implement encrypted Google Drive backup only after repository privacy and OAuth/signing prerequisites are safe.
