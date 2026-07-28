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

- 🧪 Simple monthly interest.
- 🧪 Extra-day full-month calculation.
- 🧪 Half-month slab calculation.
- 🧪 Six-month compound calculation.
- 🧪 Manual discount separation.
- 🧪 Invalid date rejection.
- 🧪 Interest-first, principal-first, and invalid custom payment allocation.
- 🧪 PIN hash verification, weak PIN rejection, and fifth-attempt lockout.

### Security/backup impact

- ✅ No business data or encryption material was added to GitHub.
- ✅ Backup is represented as a verified state machine; an upload alone is not considered successful.
- ✅ Device encryption uses authenticated AES-GCM with unique system-generated IVs.
- ⚠️ Repository is still Public. Change it to Private before OAuth/signing configuration.

### Validation status

- 🧪 Source and invariants reviewed.
- ⚠️ Full Gradle compilation is still pending because a Gradle wrapper binary/build runner is not yet present in this environment.

### Next concrete work

- ⏳ Wire PIN enrollment/verification into Compose UI and encrypted preferences.
- ⏳ Add biometric prompt and background auto-lock lifecycle controller.
- ⏳ Implement Room entities/DAOs and encrypted database-key opening flow.
- ⏳ Build customer/category/item screens and new-girvi wizard.
- ⏳ Add CI Android build after wrapper/bootstrap is available.

## Update rule

For every future change, append the date, completed behavior, security/backup impact, tests, decisions, and next concrete task.
