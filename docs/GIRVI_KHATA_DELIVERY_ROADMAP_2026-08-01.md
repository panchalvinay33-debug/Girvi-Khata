# Girvi Khata — Updated Delivery Roadmap

Date: 2026-08-01
Current active milestone: Alpha 25B

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

### Implemented and CI-verified
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
- Security Guard green
- Unit tests green
- Android compilation green
- Pinned signing identity restored and verified
- Signed Alpha 25A testing APK produced from run 517

### Remaining before Alpha 25A promotion
- Owner-phone practical workflow validation
- Verify contact with zero, one and multiple phone numbers
- Verify camera cancel/failure cleanup
- Verify existing-customer updates do not overwrite unrelated data
- Verify back-dated entry ordering and reports
- Verify private photos survive normal app restart
- Media-inclusive backup is required before stable real-photo use
- Remove temporary Compose import compatibility shim in a clean source change

## Alpha 25B — Interest engine

### Implemented on `agent/alpha25b-interest-engine`
- Reusable deterministic `InterestEngine`
- Percentage-per-month mode
- Flat monthly charge mode
- Exact per-day calculation using monthly charge / 30
- Full-month-started rule
- Completed calendar months + remaining days rule
- Compound percentage interest with configurable month interval
- Money calculated in paise using `Long` and `BigDecimal` HALF_UP rounding
- Arithmetic overflow fails closed
- Versioned `GKINT1` terms codec
- Backward-compatible `GirviInterestMetadata` attach/read/strip helper
- Reusable bilingual Compose `InterestEntrySection`
- Settlement comparison helper for Exact Days vs Full Month vs Month+Days
- Calculation contract documented
- Tests for simple monthly, flat, daily, partial month and compounding
- Tests for metadata round-trip and malformed metadata
- Tests for month-end and leap-year boundaries

### Remaining Alpha 25B integration
- Connect `InterestEntrySection` to existing Practical Entry with a small reviewed patch
- Persist `GKINT1` metadata during practical-entry save
- Read metadata when displaying/editing a girvi
- Add old-entry fallback mapping to legacy `monthlyRateBasisPoints`
- Produce signed Alpha 25B testing APK after integration
- Owner-phone calculation spot checks

### Alpha 25B acceptance
- Same engine is used by entry, future advances and settlement
- Reproducible calculations across leap years and month lengths
- No floating-point money storage
- Old monthly-percentage entries remain unchanged
- Malformed/unknown interest metadata never silently invents terms
- Flat monthly and compound percentage remain clearly separate modes

## Alpha 25C — Additional advances and two-column ledger

Deliverables:
- Add More Amount inside active girvi
- Separate effective date per advance
- Reuse or override interest rule
- Immutable interest snapshot per advance
- Shopkeeper-gave/customer-paid ledger
- Totals and outstanding balances
- Clear confirmation preventing advance/payment reversal confusion

Acceptance:
- Multiple advances calculate independently through Alpha 25B engine
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
- Calculation comparison via Alpha 25B comparison helper
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

1. Keep Alpha 25A signed APK as current phone-test baseline.
2. Finish Alpha 25B CI for engine/UI helper/tests.
3. Integrate Alpha 25B selector + metadata into existing practical entry.
4. Produce signed Alpha 25B testing APK.
5. Owner-phone spot-check percentage, flat, daily and compound examples.
6. Fix calculation/integration findings before any promotion.
7. Start Alpha 25C ledger from the verified Alpha 25B base.
8. Finish media-inclusive backup before stable real-photo promotion.

## Definition of maximum useful work per development pass

Each pass should aim to deliver a coherent vertical slice:
- implementation
- validation/tests
- documentation/evidence
- CI result
- explicit remaining blockers

Avoid plan-only commits unless they lock a significant safety or product decision.
