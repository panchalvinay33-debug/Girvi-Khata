# Girvi Khata Master Roadmap

Last updated: 2026-07-29

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records remain on the owner's Android device; only client-side encrypted backup packages may enter the owner's Google Drive.

## Mandatory delivery gate

`feature branch → automated checks → separate testing APK → owner test → fixes → owner approval → merge into main → next branch`

No untested milestone enters `main`. Every milestone updates progress, decisions, testing releases, roadmap, and the backup blueprint when storage/security/recovery behavior changes.

## Current approved baseline

- Owner-approved testing baseline: `v0.4.0-alpha.4`
- Permanent testing package/signature supports in-place upgrades and test-data retention.
- Current development remains on `agent/initial-foundation`; `main` has not yet been advanced.
- Repository must be changed to Private before OAuth, production signing, or real backup credentials are introduced.

## Phase 0 — Governance and foundation

- [x] Repository, privacy boundary, security policy, decision log, progress ledger, testing ledger
- [x] Security Guard and Android CI
- [x] Mandatory test-before-merge workflow
- [x] Alpha 1, Alpha 2-fixed, and Alpha 4 owner-approved checkpoints
- [ ] Advance approved baseline into `main` after branch-history cleanup
- [ ] Change repository visibility to Private
- [ ] Enable branch protection and required checks

## Phase 1 — Security entry

- [x] PIN enrollment, salted PBKDF2 verifier, weak-PIN rejection, progressive lockout
- [x] Android Keystore AES-GCM device protection
- [x] Strong biometric unlock
- [x] Thirty-second background auto-lock
- [ ] Privacy screen for recent-app previews
- [ ] Configurable timeout and biometric toggle

## Phase 2 — Shop customization

- [x] Configurable domain models
- [x] Persistent category create/activate/deactivate with active-girvi protection
- [ ] Category rename/reorder
- [ ] Item, unit, interest-plan, payment-mode, locker, status, and custom-field management screens

## Phase 3 — Customers and girvi

- [x] Encrypted local customer/category/girvi persistence
- [x] Existing-customer picker and search
- [x] Multiple items, quantity, gross/deduction/net weight, description
- [x] Deterministic date-wise girvi numbers
- [x] Girvi details and status
- [ ] Full customer profile and customer-wise khata screen
- [ ] Photo/document vault
- [ ] Final transactional encrypted database and migrations

## Phase 4 — Calculation engine

- [x] Monthly/daily/yearly/fixed/compound/manual/no-interest domain engine
- [x] Grace, partial-month, rounding, compounding, period breakup, adjustments
- [x] Month-wise simple-interest UI and tests
- [ ] Selectable saved interest plans in the girvi wizard
- [ ] Long-period and historical-date edge-case suite

## Phase 5 — Payments and release

- [x] Interest-first, principal-first, custom allocation
- [x] Persistent encrypted payment posting
- [x] Receipt numbering and payment modes/notes
- [x] Immutable history and linked reversals
- [x] Settlement totals, release block, owner override, release metadata
- [x] Alpha 4 physical-device owner approval
- [ ] Manual interest-adjustment UI with audit reason
- [ ] Release checklist and final settlement receipt
- [ ] Critical backup queue trigger after payment/reversal/release

## Phase 6 — Search, reports, receipts, exports

- [x] Search by customer/mobile/address/girvi/item/category
- [x] Active/released/all report filter engine
- [x] Customer-ledger and portfolio-summary engine
- [x] Effective collection reports excluding reversed payments
- [x] Date-range collection rows
- [x] Plain-text payment receipt and customer statement builders
- [x] Escaped CSV collection export builder
- [ ] Visible reports/customer khata screens
- [ ] Android secure share sheet and app-private export files
- [ ] Production PDF/thermal receipt templates and printing

## Phase 7 — Encrypted backup and recovery

- [x] Backup trigger/state/manifest contracts
- [x] Verified-backup definition
- [x] Export manifest counts and encrypted-payload SHA-256 descriptor
- [ ] Consistent encrypted snapshot package
- [ ] Recovery-passphrase key envelope
- [ ] Private Google Drive authorization and appDataFolder upload
- [ ] Upload/read-back/hash/manifest verification
- [ ] Retention, WorkManager scheduling, restore UI, migration and atomic swap

## Phase 8 — Hardening

- [x] Secret/file guard, Android backup exclusion, unit tests and signed testing builds
- [ ] Corrupt-store recovery without silently replacing records with defaults
- [ ] Transactional database, low-storage, large-data, uninstall/restore, offline and clock-change tests
- [ ] Root warning, security checklist and pilot testing

## Out of scope for first stable release

Multi-user staff accounts, live multi-device sync, web dashboard, central customer database, ads, behavioral analytics, iOS, and automated online lending.
