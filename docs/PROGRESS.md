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

- 🚧 Android build and Security Guard are running for this milestone.
- 🧪 Physical-device tests required: create girvi, close/reopen app, unlock, verify customer/girvi remains.

### Next concrete work

- ⏳ Fix any CI compile/test issues from the encrypted-persistence milestone.
- ⏳ Produce Alpha 2 APK after all checks pass.
- ⏳ Add edit/deactivate flows for categories.
- ⏳ Add customer selection instead of name-only matching.
- ⏳ Add multiple items per girvi and detailed calculation screen.
- ⏳ Add lifecycle auto-lock and biometric unlock.
- ⏳ Add transaction-safe encrypted relational persistence and migrations.

## Update rule

For every future change, append the date, completed behavior, security/backup impact, tests, decisions, and next concrete task.
