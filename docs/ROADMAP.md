# Girvi Khata Master Roadmap

Last updated: 2026-07-28

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records stay on the owner's device and only encrypted backup packages are stored in the owner's Google Drive.

## Phase 0 — Governance and foundation

- [x] Dedicated repository created
- [x] Product privacy boundary documented
- [x] Progress ledger, backup blueprint, decision log, and security policy
- [x] Repository secret/production-file security guard
- [ ] Repository visibility changed to Private
- [ ] Branch protection and review rules enabled
- [ ] Android project builds in CI

## Phase 1 — App shell and security entry

- [x] Kotlin/Compose scaffold, theme, navigation, lock/dashboard prototypes
- [x] Secure PIN hashing and progressive attempt throttling domain
- [x] Android Keystore AES-GCM key manager
- [ ] PIN enrollment UI and encrypted persistence
- [ ] Biometric unlock wiring
- [ ] App background privacy screen

## Phase 2 — Shop customization

- [x] Shop, custom category, item-master, unit, custom-field, payment-mode, and interest-plan models
- [x] List item plus manual-item path
- [ ] Persistent repository implementation
- [ ] Compose management screens

## Phase 3 — Customer and girvi records

- [x] Customer domain and validation
- [x] Multiple items, weight, quantity, condition, description, and media-vault IDs
- [x] Loan metadata and deterministic girvi numbering
- [x] Offline repository contracts
- [ ] Room entities and DAOs
- [ ] Customer screens and new-girvi wizard

## Phase 4 — Calculation engine

- [x] Simple monthly, daily, yearly, fixed, and compound interest
- [x] Partial-month rules, grace periods, rounding, and manual adjustments
- [x] Period-wise transparent breakup
- [x] Automated calculation tests authored
- [ ] Full build execution and expanded edge-case suite

## Phase 5 — Payments and release

- [x] Interest-first, principal-first, and validated custom allocation
- [x] Payment and reversal domain invariants
- [ ] Persistent posting transaction
- [ ] Settlement, secure release checklist, and final receipt
- [ ] Critical backup trigger implementation

## Phase 6 — Search, reports, and receipts

- [x] Local search repository contracts
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
