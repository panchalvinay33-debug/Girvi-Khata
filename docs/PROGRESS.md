# Girvi Khata Progress Ledger

This file must be updated with every meaningful code, design, security, backup, or product change.

## Status legend

- ✅ Completed
- 🚧 In progress
- ⏳ Planned
- 🧪 Needs testing
- ⚠️ Blocked/risk

## 2026-07-28 — Project initialization

### Completed

- ✅ Dedicated GitHub repository verified with admin/write access.
- ✅ Privacy-first single-owner scope locked.
- ✅ Master roadmap, encrypted backup blueprint, decision log, and security policy created.
- ✅ Android Kotlin/Compose scaffold and navy-purple lock/dashboard prototype started.
- ✅ Android uncontrolled cloud backup and device transfer disabled.
- ✅ Repository ignore policy blocks keys, credentials, databases, backups, and production data.

## 2026-07-28 — Security and business-core milestone

### Completed

- ✅ Core domain models added for shop, custom categories, item masters, customers, weights, multiple girvi items, interest plans, accounts, and payments.
- ✅ Item entry supports either a list item or a manual item name.
- ✅ Weight model calculates gross and net grams and prevents negative values.
- ✅ Payment model enforces exact principal + interest + charges reconciliation.
- ✅ Reversal payments require a link to the original transaction.
- ✅ Transparent interest engine implemented for monthly, daily, yearly, fixed, compound, manual, and no-interest plans.
- ✅ Partial-month rules, grace days, compound intervals, and rounding rules implemented.
- ✅ Manual adjustments remain separate from automatic interest.
- ✅ Payment allocation engine supports interest-first, principal-first, and validated custom allocation.
- ✅ Deterministic date-and-sequence girvi number generator added.
- ✅ Customer and girvi validation layer added.
- ✅ PIN hashing uses salted PBKDF2-HMAC-SHA256 and rejects common/repeated PINs.
- ✅ Progressive PIN lockout policy added.
- ✅ Android Keystore AES-256-GCM device-key wrapper added.
- ✅ Offline-first repository contracts and safe audit-event contract added.
- ✅ Backup manifest, verified-state model, trigger model, and restore result contract added.
- ✅ GitHub Security Guard workflow rejects obvious secrets and forbidden production files.

### Automated tests added

- ✅ Simple monthly interest.
- ✅ Extra-day full-month calculation.
- ✅ Half-month slab calculation.
- ✅ Six-month compound calculation.
- ✅ Manual discount separation.
- ✅ Invalid date rejection.
- ✅ Interest-first, principal-first, and invalid custom payment allocation.
- ✅ PIN hash verification, weak PIN rejection, and fifth-attempt lockout.

## 2026-07-28 — Alpha 1 testing milestone

- ✅ Real PIN enrollment and verification wired into Compose.
- ✅ Dashboard, Customers, Girvi, Masters, and More tabs working.
- ✅ Android CI unit tests passed.
- ✅ Debug APK built and artifact verified.
- ✅ Alpha 1 APK shared for physical-device testing.
- ✅ Owner confirmed the Alpha 1 flow works properly.

## 2026-07-28 — Encrypted local persistence and girvi-entry milestone

### Completed

- ✅ Added app-private encrypted snapshot storage.
- ✅ Business records are serialized in memory and encrypted before disk write.
- ✅ AES-256-GCM key is generated and protected by Android Keystore.
- ✅ Store uses authenticated associated data and a versioned binary envelope.
- ✅ Writes use a temporary file followed by replacement to reduce partial-write risk.
- ✅ Customer records now persist across app restarts.
- ✅ Categories now persist and can be manually added.
- ✅ Girvi records now persist across app restarts.
- ✅ New Girvi screen collects customer, mobile, address, category, item, weight, principal, and monthly interest rate.
- ✅ New customers are created during girvi entry; matching customers are reused.
- ✅ Dashboard totals are calculated from saved records.
- ✅ Customer search and girvi lists use saved encrypted data.
- ✅ One-month and six-month simple-interest preview added to the entry screen.

### Security/backup impact

- ✅ Plain customer and girvi JSON is never written to disk.
- ✅ The encrypted store remains inside Android app-private storage.
- ✅ Android automatic cloud backup remains disabled.
- ⚠️ This snapshot store is an interim persistence layer. It will be migrated to a transaction-safe encrypted relational store before production use.
- ⚠️ Google Drive backup/restore is not yet active.

### Validation status

- ✅ Android build and Security Guard passed for the first Alpha 2 artifact.
- ⚠️ Physical install exposed a package-signature conflict with Alpha 1 because CI used a new temporary debug key.
- 🧪 Replacement Alpha 2 testing APK required after signing fix.

### Next concrete work

- ⏳ Verify stable testing package/signing build in CI.
- ⏳ Produce replacement Alpha 2 APK.
- ⏳ Test create girvi, close/reopen app, unlock, and verify customer/girvi remains.
- ⏳ Add edit/deactivate flows for categories.
- ⏳ Add customer selection instead of name-only matching.
- ⏳ Add multiple items per girvi and detailed calculation screen.
- ⏳ Add lifecycle auto-lock and biometric unlock.
- ⏳ Add transaction-safe encrypted relational persistence and migrations.

## 2026-07-28 — Mandatory test-before-merge process

- ✅ Owner confirmed that every milestone will be tested separately before merging.
- ✅ `main` is now defined as the latest owner-approved testing baseline.
- ✅ New work must remain on a feature/milestone branch until its APK is tested and approved.
- ✅ Mandatory workflow documented in `docs/DEVELOPMENT_WORKFLOW.md`.
- ✅ Roadmap updated with the branch → APK → owner test → fix → approval → merge gate.
- ✅ Every milestone must update roadmap, progress, decisions, testing releases, and the backup blueprint when data/security/backup behavior changes.
- ✅ Large new ideas will be recorded as future milestones instead of silently expanding the current test scope.
- ⏳ Alpha 1 approved baseline must be merged into `main` before the next independent milestone branch is finalized.
- 🧪 Encrypted-persistence work will receive a separate Alpha 2 APK and physical-device test before merge.

## 2026-07-28 — Stable testing installation fix

- ✅ Root cause confirmed: Alpha 1 and Alpha 2 were signed by different ephemeral CI debug keys.
- ✅ Testing APK now uses separate package ID `com.girvikhata.app.testing`, so it does not conflict with the old prototype package.
- ✅ Testing app label changed to `Girvi Khata Test` so both icons are clearly distinguishable.
- ✅ CI now creates one random non-production testing key and retains it in a fixed GitHub Actions cache.
- ✅ Future testing APKs reuse the same package and signing identity, allowing direct upgrades while preserving PIN and encrypted local data.
- ✅ Production package and production release signing remain separate and are not stored in source control.
- 🧪 First stable-signing testing APK must be installed once; all later testing APKs should upgrade it directly.

## Update rule

For every future change, append the date, completed behavior, security/backup impact, tests, decisions, and next concrete task.
