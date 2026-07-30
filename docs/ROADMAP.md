# Girvi Khata Master Roadmap

Last updated: 2026-07-30 after Alpha 14.

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records remain on the owner's Android device. Only client-side encrypted backup packages may enter owner-selected storage or the owner's Google Drive.

## Mandatory delivery gate

`development branch → automated checks → separate versioned APK → owner physical test → fixes/retest → explicit owner approval → merge into main`

## Current status

- Latest owner-approved functional baseline: `v0.9.0-alpha.9`
- Latest CI-verified testing build: `v0.14.0-alpha.14`
- Alpha 10–14 owner physical tests: pending
- Development branch: `agent/initial-foundation`
- `main`: untouched
- Repository must become Private before OAuth credentials, production signing or real data are introduced.
- Whole-product estimate toward a safe first stable release: about 65%; see `PROJECT_COMPLETION_AUDIT.md`.

## Phase 0 — Governance and delivery

- [x] Privacy boundary, security policy, decisions/progress/testing/current-state ledgers
- [x] Android CI, Security Guard and stable in-place testing signature
- [x] Mandatory owner-test-before-merge workflow
- [ ] Owner-test Alpha 10–14 and fix/retest regressions
- [ ] Advance an explicitly approved baseline into `main`
- [ ] Change repository visibility to Private
- [ ] Enable branch protection and required checks

## Phase 1 — Security entry and recovery

- [x] PIN enrollment, PBKDF2 verifier, weak-PIN rejection and progressive lockout
- [x] Strong biometric unlock and authenticated data-preserving PIN recovery
- [x] Configurable biometric toggle
- [x] Configurable auto-lock: immediate, 30 sec, 1 min, 5 min
- [x] App-wide secure-window policy and single launcher
- [x] PIN verifier structure diagnostic
- [ ] Owner-device timing/biometric/privacy verification
- [ ] Root/device-integrity warning and final production security checklist

## Phase 2 — Shop customization

- [x] Category create/activate/deactivate with active-girvi protection
- [x] Category rename with linked girvi/item propagation
- [x] Category reorder
- [ ] Item and unit management
- [ ] Saved interest-plan management and girvi-wizard selection
- [ ] Payment-mode, locker, status and custom-field management

## Phase 3 — Customers and girvi

- [x] Encrypted customer/category/girvi persistence
- [x] Existing-customer picker, search and profile editing
- [x] Duplicate-mobile protection and history-safe customer deletion
- [x] Multiple items, quantity, weights and description
- [x] Deterministic date-wise girvi numbers and status/details
- [x] Customer-wise khata
- [ ] Photo/document media vault
- [ ] Transaction-safe encrypted relational database and migration

## Phase 4 — Calculation engine

- [x] Monthly/daily/yearly/fixed/compound/manual/no-interest foundations
- [x] Grace, partial month, rounding, compounding, period breakup and adjustments
- [x] Settlement UI and tests
- [ ] Saved plan selection in girvi wizard
- [ ] Historical-date and long-period edge-case expansion

## Phase 5 — Payments and release

- [x] Interest-first, principal-first and custom allocation
- [x] Payment posting, modes, notes and receipt numbers
- [x] Immutable history and linked reversals
- [x] Settlement totals, release block, override and release metadata
- [ ] Manual interest-adjustment UI with mandatory reason
- [ ] Final release checklist and production receipt
- [ ] Exact database-transaction audit events

## Phase 6 — Search, reports and exports

- [x] Search by customer/mobile/address/girvi/item/category
- [x] Active/released/all filters
- [x] Customer ledger and portfolio summaries
- [x] Effective collections excluding reversals
- [x] Today/7-day/30-day/all-time/custom date ranges
- [x] CSV, text receipt and customer-statement sharing
- [ ] Production PDF receipt and statement templates
- [ ] Thermal printer support

## Phase 7 — Backup, recovery and safety

- [x] Portable versioned snapshot serializer and AES-256-GCM package
- [x] PBKDF2 recovery phrase, random salt/nonce and tamper validation
- [x] Strict restore preview/confirmation and read-back verification
- [x] Rotating local safety copies, quarantine and recovery-required screen
- [x] Data Safety dashboard and encrypted hash-chained journal
- [x] Direct Files/Drive document write and same-URI read-back verification
- [ ] Owner physical test of backup/restore/provider compatibility
- [ ] User-visible local safety-copy rollback management
- [ ] Google Drive API authorization, upload/read-back verification and retention
- [ ] WorkManager scheduling, retries and cloud restore discovery

## Phase 8 — Navigation and UX

- [x] One launcher and internal Tools hub
- [x] Owner Settings, Safety, Reports, Backup, Restore and PIN Recovery reachable internally
- [ ] Replace floating Tools activity hop with native main-app navigation
- [ ] Accessibility, font scaling, keyboard behavior and broader screen-size testing
- [ ] Final onboarding/help and wording review

## Phase 9 — Production hardening

- [x] Secret/file guard, Android backup exclusion, stable testing builds and unit suite
- [x] Explicit corrupt-store recovery; no silent empty fallback
- [ ] Encrypted relational database with transaction/migration tests
- [ ] Low-storage, large-data, interrupted-write, clock-change, offline and device-loss tests
- [ ] Private production signing and release verification
- [ ] Pilot rollout, feedback, crash diagnosis and rollback plan

## Out of scope for first stable release

Multi-user staff accounts, live multi-device sync, web dashboard, central customer database, ads, analytics, iOS and automated online lending.
