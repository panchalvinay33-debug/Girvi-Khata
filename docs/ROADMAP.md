# Girvi Khata Master Roadmap

Last updated: 2026-07-28

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records stay on the owner's device and only encrypted backup packages are stored in the owner's Google Drive.

## Phase 0 — Governance and foundation

- [x] Dedicated repository created
- [x] Product privacy boundary documented
- [x] Progress ledger created
- [x] Backup blueprint created
- [x] Decision log created
- [ ] Repository visibility changed to Private
- [ ] Branch protection and review rules enabled
- [ ] Android project builds in CI

## Phase 1 — App shell and security entry

- [x] Kotlin/Compose project scaffold
- [x] Material theme foundation
- [x] Initial navigation shell
- [x] App lock UI prototype
- [ ] Secure PIN enrollment
- [ ] PIN verification with attempt throttling
- [ ] Biometric unlock
- [ ] App background privacy screen
- [ ] Android Keystore key manager

## Phase 2 — Shop customization

- [ ] Shop profile
- [ ] Custom category master
- [ ] Custom item master
- [ ] Manual item add and optional permanent save
- [ ] Custom units
- [ ] Category-specific custom fields
- [ ] Custom payment modes
- [ ] Interest-plan master

## Phase 3 — Customer and girvi records

- [ ] Customer create/edit/search
- [ ] Customer photo and optional documents
- [ ] Multiple items in one girvi
- [ ] Weight, quantity, condition, description
- [ ] Item and weighing-scale photographs
- [ ] Loan amount and transaction metadata
- [ ] Review and save flow
- [ ] Girvi number generation

## Phase 4 — Calculation engine

- [ ] Simple monthly interest
- [ ] Daily and yearly interest
- [ ] Fixed monthly/period interest
- [ ] Compound interest with custom interval
- [ ] Partial-month rules
- [ ] Grace periods
- [ ] Rounding rules
- [ ] Manual adjustment without overwriting original calculation
- [ ] Period-wise transparent breakup
- [ ] Automated calculation tests

## Phase 5 — Payments and release

- [ ] Interest-only payment
- [ ] Principal-only payment
- [ ] Mixed and partial payment
- [ ] Configurable allocation priority
- [ ] Reversal transactions
- [ ] Full settlement
- [ ] Secure item-release checklist
- [ ] Final receipt
- [ ] Immediate critical backup trigger

## Phase 6 — Search, reports, and receipts

- [ ] Fast local search
- [ ] Customer profile and ledger
- [ ] Active, due, and closed girvi views
- [ ] Daily/monthly reports
- [ ] Principal and interest reports
- [ ] PDF receipts
- [ ] Protected exports with cleanup

## Phase 7 — Encrypted Google Drive backup

- [ ] Google account authorization
- [ ] Minimum Drive scope
- [ ] Client-side encrypted backup package
- [ ] Versioned backup retention
- [ ] Upload and read-back verification
- [ ] Integrity hashes and manifest
- [ ] Automatic WorkManager backup
- [ ] Manual backup
- [ ] Same-account restore
- [ ] Recovery-passphrase flow
- [ ] Corruption and Drive-full handling

## Phase 8 — Hardening and real-world testing

- [ ] No sensitive data in logs
- [ ] No secrets in repository or APK resources
- [ ] Database migration tests
- [ ] App uninstall/new-device restore test
- [ ] Offline and low-storage tests
- [ ] Large dummy dataset tests
- [ ] Root/compromised-device warning
- [ ] Security checklist completed
- [ ] Pilot shop testing

## Out of scope for first stable release

- Multi-user/staff accounts
- Multi-device real-time sync
- Web dashboard
- Central customer database
- Ads or behavioral analytics
- iOS app
- Automated online lending
