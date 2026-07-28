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

## 2026-07-28 — Mandatory test-before-merge process

- ✅ Owner confirmed that every milestone will be tested separately before merging.
- ✅ `main` is now defined as the latest owner-approved testing baseline.
- ✅ New work must remain on a feature/milestone branch until its APK is tested and approved.
- ✅ Mandatory workflow documented in `docs/DEVELOPMENT_WORKFLOW.md`.
- ✅ Roadmap updated with the branch → APK → owner test → fix → approval → merge gate.
- ✅ Every milestone must update roadmap, progress, decisions, testing releases, and the backup blueprint when data/security/backup behavior changes.
- ✅ Large new ideas will be recorded as future milestones instead of silently expanding the current test scope.

## 2026-07-28 — Stable testing installation fix

- ✅ Root cause confirmed: Alpha 1 and Alpha 2 were signed by different ephemeral CI debug keys.
- ✅ Testing APK now uses separate package ID `com.girvikhata.app.testing`, so it does not conflict with the old prototype package.
- ✅ Testing app label changed to `Girvi Khata Test` so both icons are clearly distinguishable.
- ✅ CI now creates one random non-production testing key and retains it in a fixed GitHub Actions cache.
- ✅ Future testing APKs reuse the same package and signing identity, allowing direct upgrades while preserving PIN and encrypted local data.
- ✅ Production package and production release signing remain separate and are not stored in source control.

## 2026-07-28 — Alpha 2 owner approval and next milestone foundation

- ✅ Owner installed the corrected permanent testing package successfully.
- ✅ PIN, category creation, girvi creation, dashboard totals, search, listing, lock/unlock, app restart, and encrypted record persistence were physically tested and approved.
- ✅ Alpha 2 fixed is now the owner-approved testing baseline.
- ✅ Encrypted snapshot schema upgraded from v1 to v2 while keeping Alpha 2 records readable.
- ✅ Multiple-item record structure added with legacy single-item migration fallback.
- ✅ Item validation now covers quantity, gross weight, deduction, and net weight.
- ✅ Customer matching now prefers normalized exact mobile and falls back to normalized name.
- ✅ Customer search now supports name, mobile, and address.
- ✅ Girvi numbering now scans the highest sequence for the current date instead of relying on total record count.
- ✅ Transparent month-wise simple-interest breakup added.
- ✅ Category deactivate safety rule blocks deactivation while an active girvi uses that category.
- ✅ Session auto-lock timeout policy added with clock-rollback protection.
- ✅ Biometric hardware/enrollment capability layer added.
- ✅ New automated tests added for customer matching, searching, multiple-item validation, numbering, calculation breakup, category safety, and session locking.

## 2026-07-28 — Alpha 3 visible workflow implementation

- ✅ Existing-customer search and picker wired into the new-girvi screen.
- ✅ Customer selection fills saved mobile/address and reuses the existing customer ID.
- ✅ Multiple-item editor wired with add/remove, category, item name, quantity, gross weight, deduction, net-weight preview, and description.
- ✅ Girvi save now persists all item rows in encrypted schema v2.
- ✅ Girvi list and dashboard now display real multi-item totals.
- ✅ Girvi detail screen added with every saved item and gross/deduction/net weight.
- ✅ Month-selectable detailed simple-interest screen added with month-wise rows and total payable.
- ✅ Category activate/deactivate controls wired; categories used by active girvi remain protected.
- ✅ Real strong-biometric prompt wired to the lock screen when enrolled hardware is available.
- ✅ Activity lifecycle wired to the 30-second background auto-lock policy.
- ✅ Testing version advanced to `0.3.0-testing`, version code 3, retaining the permanent testing package/signature.
- ✅ Alpha 2 encrypted records remain readable through the v1-to-v2 migration fallback.
- 🚧 Android unit tests, Compose compilation, signing, APK packaging, and Security Guard are running.
- 🧪 Next physical test must verify direct upgrade over Alpha 2, old record retention, fingerprint unlock, background lock, multiple-item save, category safety, and calculation detail.

## Update rule

For every future change, append the date, completed behavior, security/backup impact, tests, decisions, and next concrete task.
