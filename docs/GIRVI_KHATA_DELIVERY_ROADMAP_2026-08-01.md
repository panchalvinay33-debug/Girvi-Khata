# Girvi Khata — Updated Delivery Roadmap

Date: 2026-08-01
Current active milestone: Alpha 25A

## Governance rules

- `main` remains owner-approved Alpha 21 until explicit promotion approval.
- `baseline/alpha21-owner-approved` remains permanent rollback source.
- `agent/alpha24-production-hardening` remains the signed Alpha 24 base.
- Alpha 25 work stays on feature branches and draft pull requests.
- No PR is marked ready, merged or promoted without owner approval.
- Every testing APK must use the pinned permanent testing certificate.

## Completed foundation

- Customer, girvi, payment and settlement core
- PIN, biometric and local encryption
- Verified business-write coordination
- Backup/restore foundation
- Permanent testing signing identity and fail-closed CI
- Signed Alpha 24 testing APK

## Alpha 25A — Practical bilingual entry

### Implemented
- Dedicated practical entry activity
- Owner-PIN gate for shortcut
- Hindi/English/mixed text entry
- Android Contacts import
- Editable imported name/mobile
- Back-date calendar and future-date blocking
- Multiple items
- Simple and advanced weight
- Customer/item live photo capture
- App-private media directory
- Monthly interest preview
- Bilingual review before save
- Save through encrypted verified-write path

### Remaining before Alpha 25A testing APK
- Compile and unit-test green
- Remove temporary Compose import compatibility shim cleanly
- Add model/helper tests for mobile normalization, dates and weight validation
- Verify contact with zero, one and multiple phone numbers
- Verify camera cancel/failure cleanup
- Verify existing-customer updates do not overwrite unrelated data
- Verify back-dated entry ordering and reports
- Verify private photos survive normal app restart
- Decide whether Alpha 25A backup must include photos before phone testing; stable promotion requires yes
- Produce signed APK and owner test checklist

### Alpha 25A acceptance gates
- Manual entry works when contact/camera actions are cancelled
- Hindi and English names/items persist and search correctly
- Imported values remain editable
- Future date cannot be saved
- Old Alpha 24 snapshot loads without loss
- New entry appears in existing customer/girvi screens
- PIN gate cannot be bypassed
- Security Guard, tests, compile, signing and APK verification pass

## Alpha 25B — Interest engine

Deliverables:
- Percentage-per-month mode
- Flat monthly charge mode
- Per-day accurate calculation
- Full/partial month rules
- Compound interest with configurable interval
- Immutable rule snapshot per principal/advance
- Live preview and comparison
- Golden calculation test matrix

Acceptance:
- Reproducible calculations across leap years and month lengths
- No floating-point money storage
- Date/timezone boundary tests
- Old monthly-percentage entries remain unchanged

## Alpha 25C — Additional advances and two-column ledger

Deliverables:
- Add More Amount inside active girvi
- Separate effective date per advance
- Reuse or override interest rule
- Shopkeeper-gave/customer-paid ledger
- Totals and outstanding balances
- Clear confirmation preventing advance/payment reversal confusion

Acceptance:
- Multiple advances calculate independently
- Payment allocations reconcile exactly
- Reversal returns account to prior state
- Reports agree with girvi detail totals

## Alpha 25D — Settlement redesign

Deliverables:
- Settlement date picker
- Full, interest-only, principal-only and partial payment
- Interest-first, principal-first and custom split
- Discounts/adjustments with reason
- Detailed principal/interest breakdown
- Close/release confirmation
- Side calculator

Acceptance:
- Closure blocked with outstanding unless explicit owner override
- Settlement summary matches ledger
- Reopen/reversal policy documented and tested
- Receipt-ready immutable snapshot generated

## Alpha 25E — Backup, recovery and production hardening

Deliverables:
- Encrypted media-inclusive `.gkb` backup
- Backup manifest with counts, hashes and schema
- Restore preview and dry-run verification
- Atomic restore with rollback
- Orphan-media cleanup
- Low-storage checks
- Process-kill/interrupted-write tests
- Real-device soak test

Acceptance:
- Data plus photos restore to a clean testing install
- Wrong/corrupt backup fails closed
- Restore never silently opens an empty khata
- Before/after counts and fingerprints match

## Alpha 26 — Business outputs

- PDF receipt
- Share/print receipt
- Bluetooth/thermal printer integration
- Improved reports and exports
- Customer account statement

## Alpha 27 — Optional cloud convenience

Only after stable offline release:
- User-controlled Google Drive backup
- Scheduled encrypted backup
- No dependency on WhatsApp
- Core app remains fully usable offline

## Work order from current point

1. Make Alpha 25A CI green.
2. Add Alpha 25A validation tests.
3. Complete media backup decision/implementation.
4. Produce signed testing APK.
5. Run owner-phone checklist without uninstalling Alpha 21.
6. Fix phone findings on same draft PR.
7. Owner approves or rejects Alpha 25A.
8. Start Alpha 25B on a new branch from the approved testing base.

## Definition of maximum useful work per development pass

Each pass should aim to deliver a coherent vertical slice:
- implementation
- validation/tests
- documentation/evidence
- CI result
- explicit remaining blockers

Avoid plan-only commits unless they lock a significant safety or product decision.
