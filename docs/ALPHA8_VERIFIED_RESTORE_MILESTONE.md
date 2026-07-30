# Alpha 8 — Verified Encrypted Restore

Status: IMPLEMENTED — OWNER TEST PENDING

## Baseline

- Alpha 4 remains the latest explicitly owner-approved accounting baseline.
- Alpha 7 backup creation APK remains owner physical-test pending.
- Alpha 8 stays on `agent/initial-foundation`; `main` must not be changed without owner approval.

## Visible workflow

`Girvi Tools Test` now exposes:

1. Reports & Customer Khata
2. Encrypted Backup Banaye
3. Backup Restore / Import

Restore requires:

- Existing six-digit app PIN.
- Android document picker selection of a backup package.
- Recovery passphrase.
- Successful AES-GCM authentication and portable-envelope validation.
- Strict snapshot JSON decoding and relationship checks.
- Count/hash preview before any write.
- Explicit destructive confirmation.

## Safety rules

- Wrong passphrase, damaged package, invalid JSON, unsupported schema, duplicate IDs/numbers, invalid accounting entries, or missing customer links must fail before the current store is touched.
- Before replacement, the current snapshot is encrypted with the same recovery passphrase into app-private `restore_safety` storage.
- The last three pre-restore safety packages are retained.
- Restored records are saved through the existing encrypted Android-Keystore store.
- Customer, girvi, and immutable ledger counts are read back after save; mismatch is a restore failure.
- Restore/backup/report activities are not exported Android components.

## Strict portable decode

The decoder validates:

- Supported schema range.
- Required nonblank IDs/names/numbers.
- Unique customer/category/girvi IDs.
- Unique girvi numbers.
- Positive timestamps and principal.
- Nonnegative interest rates.
- Active/released status only.
- Positive quantities.
- Payment allocation invariants through `PaymentRecord` construction.
- Every girvi customer ID exists in the customer list.

## Tests

- Full encode/decode round trip preserving items, payments, manual adjustments, release timestamp, and release note.
- Encrypt/decrypt/decode round trip.
- Missing customer relationship rejection.
- Unsupported future schema rejection.
- Duplicate girvi-number rejection.
- Existing wrong-passphrase/tamper/trailing-data crypto tests remain active.

## Version

- Version code: 8
- Version name: `0.8.0-testing`
- Package/signing baseline unchanged.

## Known limitations

- Google Drive automatic upload/read-back/retention is not yet active.
- Pre-restore safety backups are app-private and are lost if the app is uninstalled; the owner should keep external `.gkb` files.
- Restore currently replaces the complete snapshot; selective merge/import is intentionally unsupported.
- Main persistence remains the interim encrypted snapshot store rather than a transaction-safe encrypted relational database.
- Alpha 8 requires a separate owner physical test before approval or merge.