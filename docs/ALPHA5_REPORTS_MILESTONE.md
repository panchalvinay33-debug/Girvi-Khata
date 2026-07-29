# Alpha 5 — Reports, Statements, Receipts, and Secure Export

Status: IMPLEMENTED — ANDROID CI VALIDATION IN PROGRESS

## Owner-approved baseline

- Alpha 4 payment/settlement/release milestone is owner approved.
- Alpha 5 remains on the testing branch and is not merged into `main`.

## Visible scope

- Secure reports entry protected by the existing Girvi Khata PIN verifier.
- Portfolio overview with customer, girvi, active/released, principal, received, and outstanding totals.
- Settlement-month selector for transparent report calculations.
- Outstanding-customer ranking.
- Customer search and customer-wise khata summary.
- Shareable customer statement text.
- Girvi search with All, Active, and Released filters.
- Today, 7-day, 30-day, and all-time effective collection reports.
- Reversed payments excluded from collection totals while retained in the immutable audit ledger.
- Shareable payment receipt text.
- CSV collection export through an app-private temporary cache file.
- Read-only Android FileProvider URI grants for CSV sharing.

## Security and privacy behavior

- Reports read the same app-private encrypted snapshot as the main app.
- Reports do not expose data before PIN verification.
- CSV files are created only inside the app cache export directory.
- Share recipients receive temporary read permission; no public-storage write is performed.
- Export cache files older than 24 hours are cleaned opportunistically.
- Android automatic cloud backup remains disabled.
- Google Drive backup is still not active.

## Current navigation decision

For this testing milestone, Android exposes a second launcher entry named `Girvi Reports Test` inside the same installed testing package. It shares the same encrypted data and PIN verifier. This avoids a risky rewrite of the current large navigation file during the accounting/reporting milestone. After owner testing, reports should be folded into the main app navigation in a dedicated refactor milestone.

## Version

- Version code: 5
- Version name: `0.5.0-testing`
- Package: `com.girvikhata.app.testing`
- Stable testing signature must remain unchanged.

## Owner physical-test checklist

1. Install directly over Alpha 4 without uninstalling.
2. Confirm the main app PIN and records remain.
3. Confirm two launcher entries appear: `Girvi Khata Test` and `Girvi Reports Test`.
4. Open reports and verify no data appears before PIN unlock.
5. Test correct and incorrect PIN behavior.
6. Compare overview totals against saved girvi/payment records.
7. Change settlement months and confirm interest/outstanding changes.
8. Open customer khata and share a statement.
9. Test girvi All/Active/Released filters and search.
10. Test Today, 7 Days, 30 Days, and All collection ranges.
11. Reverse a payment in the main app and confirm it disappears from effective collections.
12. Share a payment receipt through WhatsApp/email or another available app.
13. Share collection CSV and confirm it opens correctly.
14. Confirm no crash, clipped dialog, scrolling issue, or accidental data exposure.

## Known limitations

- Reports currently use a separate launcher entry rather than the main bottom navigation.
- Customer statements and receipts are text-first; PDF/thermal-print layouts are pending.
- Custom date picker is pending; preset ranges are available.
- Final transaction-safe encrypted relational database is pending.
- Encrypted Google Drive backup/restore is pending.
