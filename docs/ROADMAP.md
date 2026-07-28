# Girvi Khata Master Roadmap

Last updated: 2026-07-28

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records stay on the owner's device and only encrypted backup packages are stored in the owner's Google Drive.

## Mandatory delivery process

Every milestone follows this gate:

`feature branch → automated checks → separate testing APK → owner testing → fixes → owner approval → merge into main → next branch`

`main` always represents the latest owner-approved testing baseline. No untested milestone is merged. The complete mandatory process is documented in `docs/DEVELOPMENT_WORKFLOW.md`.

Every meaningful milestone must also update the progress ledger, decision log, testing-release ledger, and—whenever data, encryption, storage, recovery, migration, or backup behavior changes—the backup blueprint.

## Phase 0 — Governance and foundation

- [x] Dedicated repository created
- [x] Product privacy boundary documented
- [x] Progress ledger, backup blueprint, decision log, and security policy
- [x] Repository secret/production-file security guard
- [x] Mandatory branch → APK test → approval → merge workflow documented
- [x] First owner-tested APK checkpoint completed (`v0.1.0-alpha.1`)
- [ ] Merge approved Alpha 1 baseline into `main`
- [ ] Repository visibility changed to Private
- [ ] Branch protection and required-check rules enabled

## Phase 1 — App shell and security entry

- [x] Kotlin/Compose scaffold, theme, navigation, lock/dashboard prototypes
- [x] Secure PIN hashing and progressive attempt throttling domain
- [x] PIN enrollment UI and local verifier persistence
- [x] Android Keystore AES-GCM key manager
- [ ] Biometric unlock wiring
- [ ] App background privacy screen

## Phase 2 — Shop customization

- [x] Shop, custom category, item-master, unit, custom-field, payment-mode, and interest-plan models
- [x] List item plus manual-item path
- [x] Initial encrypted local category persistence
- [ ] Full category/item/unit/interest-plan management screens
- [ ] Edit, reorder, deactivate, and validation behavior

## Phase 3 — Customer and girvi records

- [x] Customer domain and validation
- [x] Multiple items, weight, quantity, condition, description, and media-vault IDs
- [x] Loan metadata and deterministic girvi numbering
- [x] Offline repository contracts
- [x] Initial encrypted local customer and girvi persistence
- [x] Initial customer search and new-girvi entry flow
- [ ] Production structured database and migrations
- [ ] Full customer profile and multi-item girvi wizard
- [ ] Photo/document vault

## Phase 4 — Calculation engine

- [x] Simple monthly, daily, yearly, fixed, and compound interest
- [x] Partial-month rules, grace periods, rounding, and manual adjustments
- [x] Period-wise transparent breakup
- [x] Automated calculation tests and successful CI execution
- [ ] Connect full selectable interest plans to girvi UI
- [ ] Expanded edge-case and long-period calculation suite

## Phase 5 — Payments and release

- [x] Interest-first, principal-first, and validated custom allocation
- [x] Payment and reversal domain invariants
- [ ] Persistent posting transaction
- [ ] Settlement, secure release checklist, and final receipt
- [ ] Critical backup trigger implementation

## Phase 6 — Search, reports, and receipts

- [x] Local search repository contracts
- [x] Initial saved customer/girvi listing and dashboard totals
- [ ] Customer ledger and active/due/closed screens
- [ ] Reports, PDF receipts, and protected exports

## Phase 7 — Encrypted Google Drive backup

- [x] Backup trigger/state/manifest and restore-result contracts
- [x] Verified-backup success definition
- [ ] Google authorization and minimum Drive scope
- [ ] Encrypted package, retention, upload/read-back/hash verification
- [ ] WorkManager scheduling and recovery UI

## Phase 8 — Hardening and testing

- [x] Logging/secrets policy, forbidden-file checks, and Android backup exclusion
- [x] Automated Android unit-test and debug-APK build pipeline
- [x] Versioned testing-release ledger and owner test gate
- [ ] Database migration, uninstall/restore, offline, low-storage, and large-data tests
- [ ] Root warning, security checklist, and pilot testing

## Out of scope for first stable release

- Multi-user/staff accounts
- Multi-device real-time sync
- Web dashboard
- Central customer database
- Ads or behavioral analytics
- iOS app
- Automated online lending
