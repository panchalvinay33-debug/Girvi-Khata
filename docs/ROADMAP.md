# Girvi Khata Master Roadmap

Last updated: 2026-07-29

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records remain on the owner's Android device. Only client-side encrypted backup packages may enter owner-selected storage or the owner's Google Drive.

## Mandatory delivery gate

`development branch → automated checks → separate versioned APK → owner physical test → fixes/retest → explicit owner approval → merge into main`

No untested milestone enters `main`. Every milestone updates current state, progress, decisions, testing releases, roadmap, and backup documentation.

## Current status

- Latest owner-approved functional baseline: `v0.4.0-alpha.4`
- Latest CI-verified testing build: `v0.8.0-alpha.8`
- Alpha 8 owner physical test: pending
- Development branch: `agent/initial-foundation`
- `main`: untouched
- Repository must become Private before OAuth credentials, production signing, or real data are introduced.

## Phase 0 — Governance and delivery

- [x] Repository, privacy boundary, security policy, current-state document, decision/progress/testing ledgers
- [x] Android CI and Security Guard
- [x] Stable in-place testing package and signing identity
- [x] Mandatory owner-test-before-merge workflow
- [ ] Owner-test Alpha 8 and fix/retest any regressions
- [ ] Advance an explicitly approved baseline into `main`
- [ ] Change repository visibility to Private
- [ ] Enable branch protection and required checks

## Phase 1 — Security entry and recovery

- [x] PIN enrollment, salted PBKDF2 verifier, weak-PIN rejection, progressive lockout
- [x] Android Keystore AES-GCM local-data protection
- [x] Strong biometric unlock
- [x] Thirty-second background auto-lock
- [x] Data-preserving PIN recovery through biometric/device credential
- [x] PIN verifier structure validation and synchronous security-state writes
- [ ] Physical-device verification of Alpha 8 PIN recovery
- [ ] Privacy screen for recent-app previews
- [ ] Configurable lock timeout and biometric toggle
- [ ] Explicit corrupt-security-state diagnostics and recovery telemetry that contains no secrets

## Phase 2 — Shop customization

- [x] Configurable domain models
- [x] Persistent category create/activate/deactivate with active-girvi protection
- [ ] Category rename and reorder
- [ ] Item, unit, interest-plan, payment-mode, locker, status, and custom-field management screens

## Phase 3 — Customers and girvi

- [x] Encrypted customer/category/girvi persistence
- [x] Existing-customer picker and search
- [x] Multiple items, quantity, gross/deduction/net weight, and description
- [x] Deterministic date-wise girvi numbers
- [x] Girvi details and status
- [x] Customer-account domain safety: duplicate mobile, rename propagation, deletion protection
- [x] Customer-wise khata visible in Reports
- [ ] Customer profile/edit/delete-safe visible workflow in main app
- [ ] Photo/document vault
- [ ] Final transactional encrypted database and migrations

## Phase 4 — Calculation engine

- [x] Monthly/daily/yearly/fixed/compound/manual/no-interest foundations
- [x] Grace, partial-month, rounding, compounding, period breakup, adjustments
- [x] Month-wise settlement UI and tests
- [ ] Selectable saved interest plans in girvi wizard
- [ ] Historical-date and long-period edge-case suite

## Phase 5 — Payments and release

- [x] Interest-first, principal-first, and custom allocation
- [x] Encrypted payment posting, modes, notes, receipt numbers
- [x] Immutable history and linked reversals
- [x] Settlement totals, release block, owner override, and release metadata
- [x] Alpha 4 owner approval
- [ ] Manual interest-adjustment UI with mandatory audit reason
- [ ] Final settlement/release receipt and checklist
- [ ] Critical verified-backup trigger after payment/reversal/release

## Phase 6 — Search, reports, receipts, and exports

- [x] Search by customer/mobile/address/girvi/item/category
- [x] Active/released/all filters
- [x] Customer-ledger and portfolio summaries
- [x] Effective collections excluding reversed payments
- [x] Today/7-day/30-day/all-time ranges and exact date-range engine
- [x] Visible reports and customer khata
- [x] Plain-text receipt and statement sharing
- [x] App-private CSV generation and FileProvider sharing
- [ ] Visible custom date picker
- [ ] Production PDF and thermal receipt templates/printing

## Phase 7 — Portable encrypted backup and recovery

- [x] Portable versioned snapshot serializer
- [x] AES-256-GCM package with PBKDF2-HMAC-SHA256 recovery-passphrase key derivation
- [x] Random salt/nonce, tamper detection, package size and structure validation
- [x] PIN-protected backup creation and `.gkb` sharing
- [x] Strict decode with customer/girvi/payment/reversal/release validation
- [x] Restore preview, explicit confirmation, pre-restore safety backup, encrypted local replacement, read-back count verification
- [ ] Alpha 8 owner physical test of backup and dummy-data restore
- [ ] User-visible safety-backup management and rollback
- [ ] Google Drive authorization with minimum app-data scope
- [ ] Upload/read-back/hash/manifest verification
- [ ] Retention and WorkManager scheduling
- [ ] Cloud backup discovery and compatible-version migration

## Phase 8 — Navigation and UX consolidation

- [x] Modular main UI and separate Tools hub
- [ ] Move Reports, Backup, Restore, and PIN Recovery into one main-app navigation
- [ ] Remove second launcher entry after owner verification
- [ ] Improve empty states, validation messages, keyboard behavior, and accessibility

## Phase 9 — Production hardening

- [x] Secret/file guard, backup exclusion, unit tests, stable signed testing builds
- [ ] Replace encrypted snapshot store with transaction-safe encrypted relational storage
- [ ] Explicit corrupt-store recovery; never silently replace unreadable records with defaults
- [ ] Low-storage, large-data, interrupted-write, clock-change, uninstall/restore, and offline testing
- [ ] Root/device-integrity warning and production security checklist
- [ ] Private production signing and pilot rollout

## Out of scope for first stable release

Multi-user staff accounts, live multi-device sync, web dashboard, central customer database, ads, behavioral analytics, iOS, and automated online lending.
