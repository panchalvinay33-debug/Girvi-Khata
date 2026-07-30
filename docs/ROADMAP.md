# Girvi Khata Master Roadmap

Last updated: 2026-07-30 after owner approval of Alpha 21.

## Product promise

A private, offline-first, single-owner digital girvi ledger. Business records remain on the owner's Android device. Only client-side encrypted backup packages may enter owner-selected storage or the owner's Google Drive.

## Mandatory delivery gate

`approved main base → new development branch → automated checks → separate versioned APK → owner physical test → fixes/retest → explicit owner approval → merge into main`

## Current status

- Owner-approved stable base: `v0.21.0-alpha.21`
- Exact tested source: `eec78e0aba6a8d168baeb09959fe93e2fd85733f`
- Permanent rollback branch: `baseline/alpha21-owner-approved`
- PR #1: authorized for merge to `main`
- Future relational source-of-truth cutover: not approved
- Repository must become Private before OAuth credentials, production signing or real data are introduced
- Whole-product estimate toward a safe first stable release: about 83%

## Phase 0 — Governance and delivery

- [x] Privacy boundary, security policy and authoritative project ledgers
- [x] Android CI, Security Guard and stable in-place testing signature
- [x] Mandatory owner-test-before-merge workflow
- [x] Owner-approved Alpha 21 baseline
- [x] Permanent Alpha 21 rollback branch and recovery runbook
- [x] Authorization to advance approved work into `main`
- [ ] Change repository visibility to Private
- [ ] Enable branch protection and required checks after baseline merge
- [ ] Create a fresh post-Alpha-21 development branch from `main`

## Phase 1 — Security entry and recovery

- [x] PIN enrollment, PBKDF2 verifier, weak-PIN rejection and progressive lockout
- [x] Strong biometric unlock and authenticated PIN recovery
- [x] Configurable biometric toggle and auto-lock
- [x] App-wide secure-window policy and single launcher
- [x] PIN verifier diagnostic
- [x] Owner physical approval through Alpha 21
- [ ] Root/device-integrity warning and final production security checklist

## Phase 2 — Shop customization

- [x] Category create/activate/deactivate, rename and reorder
- [x] Item and unit management
- [x] Saved interest-plan management and workflow selection
- [x] Payment-mode and locker management
- [x] Portable master backup/restore
- [ ] Dedicated relational master-ID columns
- [ ] Optional custom fields/status master after database schema stabilization

## Phase 3 — Customers and girvi

- [x] Encrypted customer/category/girvi persistence
- [x] Existing-customer picker, search and profile editing
- [x] Duplicate-mobile protection and history-safe deletion
- [x] Multiple items, quantity, weights and descriptions
- [x] Master-assisted and advanced multi-item entry
- [x] Deterministic girvi numbers and customer khata
- [x] Relational shadow with dual-read fingerprint verification
- [x] Device rollback and benchmark diagnostics
- [ ] Photo/document media vault
- [ ] Owner-approved relational source-of-truth cutover

## Phase 4 — Calculation engine

- [x] Monthly/daily/yearly/fixed/compound/manual/no-interest foundations
- [x] Grace, partial month, rounding, compounding, period breakup and adjustments
- [x] Saved plan selection in entry workflows
- [x] Settlement UI and tests
- [ ] Historical-date and very-long-period edge-case expansion

## Phase 5 — Payments and release

- [x] Interest-first, principal-first and exact custom allocation
- [x] Payment posting, modes, notes and receipt numbers
- [x] Immutable history and linked reversals
- [x] Manual interest adjustment with mandatory reason
- [x] Settlement totals, release block, override and release metadata
- [x] Settlement/release receipt text
- [ ] Production PDF receipt and final release checklist template
- [ ] Exact relational write-transaction audit after cutover

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

- [x] Portable encrypted business and master bundle
- [x] PBKDF2 recovery phrase, random salt/nonce and tamper validation
- [x] Direct Files/Drive write and same-URI verification
- [x] Strict restore preview and legacy compatibility
- [x] Rotating safety copies, quarantine and recovery-required screen
- [x] Data Safety dashboard and encrypted hash-chained journal
- [x] Approved rollback branch and documented recovery process
- [ ] User-visible local safety-copy selection
- [ ] Google Drive API authorization, upload verification and retention
- [ ] WorkManager scheduling, retries and cloud restore discovery

## Phase 8 — Navigation and UX

- [x] One launcher and internal Tools hub
- [x] Owner Settings, Masters, Safety, Reports, Backup, Restore and PIN Recovery
- [x] Advanced and classic workflow fallbacks
- [ ] Replace floating Tools activity hop with native main navigation
- [ ] Accessibility, font scaling, keyboard and wider screen-size testing
- [ ] Final onboarding/help and language review

## Phase 9 — Database migration and production hardening

- [x] Transactional relational shadow schema
- [x] Sensitive text cell encryption and foreign-key constraints
- [x] Incremental delta synchronization
- [x] Independent dual-read comparison
- [x] Deterministic large-data planner/fingerprint tests
- [x] Device-executable rollback simulation and benchmark
- [x] Low-space preflight policy
- [ ] Dedicated Unit/Locker/Plan/Payment Mode columns and explicit migration
- [ ] Relational business write APIs with snapshot fallback
- [ ] Controlled completely-full-storage and process-kill testing
- [ ] Long dual-read observation period
- [ ] Separate owner approval for relational read/write cutover
- [ ] Private production signing and release verification
- [ ] Pilot rollout, crash diagnosis and recovery rehearsal

## Out of scope for first stable release

Multi-user staff accounts, live multi-device sync, web dashboard, central customer database, ads, analytics, iOS and automated online lending.
