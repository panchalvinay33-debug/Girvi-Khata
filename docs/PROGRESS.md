# Girvi Khata Progress Ledger

This file must be updated with every meaningful code, design, security, backup, or product change.

## Status legend

- ✅ Completed
- 🚧 In progress
- ⏳ Planned
- 🧪 Needs testing
- ⚠️ Blocked/risk

## 2026-07-28 — Project initialization

- ✅ Dedicated GitHub repository and development branch established.
- ✅ Privacy-first, single-owner, offline-first product scope locked.
- ✅ Roadmap, backup blueprint, decision log, security policy, testing ledger, and mandatory test-before-merge workflow created.
- ✅ Android Kotlin/Compose scaffold started.
- ✅ Android uncontrolled cloud backup/device transfer disabled.
- ✅ Repository policy blocks credentials, signing material, databases, backups, and production data.

## 2026-07-28 — Security and business core

- ✅ Models for customers, categories, configurable items, girvi records, multiple items, weights, interest plans, payments, releases, and reversals.
- ✅ Transparent monthly/daily/yearly/fixed/compound/manual/no-interest calculation foundations.
- ✅ Exact payment allocation and reconciliation rules.
- ✅ Deterministic girvi and payment receipt numbering.
- ✅ Salted PBKDF2 PIN verifier, weak-PIN rejection, progressive lockout, Android Keystore AES-256-GCM wrapper.
- ✅ Security Guard and Android CI foundations.
- ✅ Pure-domain automated test suite established.

## Alpha 1 and Alpha 2-fixed

- ✅ Alpha 1 PIN, navigation, dashboard, and CI APK physically tested and owner-approved.
- ✅ Alpha 2 initial APK exposed an install-signature conflict.
- ✅ Permanent testing package `com.girvikhata.app.testing` and stable cached non-production testing signing identity established.
- ✅ Alpha 2-fixed physically tested and owner-approved.
- ✅ Encrypted customer/category/girvi persistence, search, restart persistence, and lock/unlock verified.

## Alpha 3

- ✅ Existing-customer picker, multiple-item editor, detailed girvi view, interest rows, category activation safety, biometric unlock, and lifecycle auto-lock implemented.
- ✅ Encrypted schema v2 preserved older Alpha 2 records.
- ✅ Android build and Security Guard passed.
- ℹ️ Alpha 3 was functionally superseded by Alpha 4.

## 2026-07-29 — Alpha 4 payments, settlement, and release

- ✅ Encrypted schema v3 added immutable payment-ledger entries, allocations, notes, payment modes, reversals, release metadata, and manual-interest adjustment storage.
- ✅ Interest-first, principal-first, and custom allocation UI.
- ✅ Overpayment protection and exact reconciliation.
- ✅ Automatic receipt numbers and payment history.
- ✅ Linked payment reversals instead of deletion.
- ✅ Settlement summary, outstanding release block, owner override, and release note.
- ✅ Dashboard payment and released totals.
- ✅ Unit tests, Compose compilation, signing, artifact verification, and Security Guard passed.
- ✅ Alpha 4 physically tested and owner-approved. This remains the latest owner-approved functional baseline.

## 2026-07-29 — Alpha 5 reports and exports

- ✅ Customer-wise khata and portfolio-summary engines.
- ✅ Effective collections exclude reversed original payments while preserving immutable history.
- ✅ Active/released/all filtering, customer outstanding ranking, date-range collections.
- ✅ Receipt and customer-statement text builders.
- ✅ CSV export with safe escaping and app-private FileProvider sharing.
- ✅ PIN-protected visible Reports entry.
- ✅ Android build and Security Guard passed.

## 2026-07-29 — Alpha 6 customer and portable-backup foundations

- ✅ Customer update normalization, duplicate-mobile protection, linked display-name propagation, and deletion safety.
- ✅ Exact local-time report ranges.
- ✅ Portable backup crypto: AES-256-GCM, PBKDF2-HMAC-SHA256 310,000 iterations, random salt/nonce, authenticated envelope, payload limits, wrong-passphrase and tamper rejection.
- ✅ Customer and backup test suites passed.
- ✅ Visible backup UI remained pending at this stage.

## 2026-07-29 — Alpha 7 visible tools and backup

- ✅ `Girvi Tools Test` hub added.
- ✅ Visible Reports and PIN-protected encrypted backup creation.
- ✅ Complete snapshot serializer includes customers, categories, girvi items, payments, reversals, release metadata, and adjustments.
- ✅ `.gkb` binary backup sharing through app-private cache and temporary URI permission.
- ✅ Android build, signing, artifact verification, and Security Guard passed.
- ⚠️ Owner reported that the previously configured PIN was not accepted after installing Alpha 7.
- ⚠️ Alpha 7 is superseded and must not be treated as approved.

## 2026-07-29 — Alpha 8 verified restore and PIN recovery

- ✅ Strict portable snapshot decoder added.
- ✅ `.gkb` restore flow: PIN gate, Android file picker, recovery phrase, authenticated decrypt, schema/data validation, preview, explicit destructive confirmation, encrypted local save, and read-back count verification.
- ✅ Restore validates duplicate IDs/numbers, customer links, payment reconciliation, timestamps, status values, item quantities, and supported schema.
- ✅ Current records remain untouched on wrong passphrase, tamper, malformed payload, or failed validation.
- ✅ Automatic app-private pre-restore safety backup added; latest three retained.
- ✅ Data-preserving PIN recovery added through strong biometric or device credential authentication.
- ✅ PIN recovery replaces only the PIN verifier and lockout state; business records are not intentionally modified.
- ✅ PIN verifier parsing hardened with hash/salt/iteration validation and synchronous security-state commits.
- ✅ Android Build `30477959563` passed.
- ✅ Security Guard `30477959615` passed.
- ✅ Artifact `8734522129` verified.
- ✅ Alpha 8 APK SHA-256: `74965918d7a97c33f28faaad8df81486a0342788009d603c4de5057f984a3d96`.
- 🧪 Alpha 8 physical-device testing is pending, especially direct upgrade, PIN recovery, backup creation, and dummy-data restore.
- ⚠️ `main` remains untouched until explicit owner approval.

## Current risks and incomplete work

- ⚠️ Local persistence remains an encrypted snapshot file rather than a final transactional encrypted relational database.
- ⚠️ Corrupt local-store loading still needs explicit recovery instead of silent fallback to defaults.
- ⏳ Google Drive OAuth, appDataFolder upload, read-back verification, retention, scheduling, and cloud restore discovery.
- ⏳ Single in-app navigation; main and Tools currently remain two launcher entries.
- ⏳ Customer profile/edit/delete-safe visible UI, custom date picker, PDF/thermal receipts, media vault, and production signing.
- ⚠️ Repository must become Private before real OAuth credentials, production signing, or real business data are introduced.

## Update rule

For every future change, record completed behavior, security/backup impact, tests, owner-test status, known risks, exact build evidence, and the next concrete task. `docs/CURRENT_STATE.md` must remain consistent with this ledger.
