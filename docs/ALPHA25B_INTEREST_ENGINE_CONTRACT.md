# Alpha 25B — Interest Engine Contract

Date: 2026-08-01
Status: implementation contract for testing branch

## Purpose
One calculation engine must be used by New Girvi, Additional Advance, Payment/Interest-only and Settlement. No screen may invent its own formula.

## Money representation
- Persist and calculate money in paise (`Long`).
- Percentage rate is basis points: 2.00% = `200` basis points.
- Round a payable sub-result to nearest paise using HALF_UP.
- Overflow must fail rather than silently wrap.

## Supported modes

### Percentage per month
Monthly charge = current principal × monthly percentage.
Example: ₹10,000 at 2% => ₹200 monthly charge.

### Flat per month
Monthly charge is a fixed rupee amount independent of principal.
Example: flat ₹300/month => ₹300 each charged month.

Compound mode is intentionally not allowed with flat charges because capitalizing a fixed service charge into principal creates ambiguous semantics. If a shop later requires this, it must become a separately named rule, not an accidental combination.

## Period rules

### EXACT_DAYS
Commercial daily calculation:
`monthly charge / 30 × elapsed calendar days`.

Example: ₹200 monthly charge for 15 elapsed days => ₹100.

### FULL_MONTH_STARTED
Every started partial month is charged as a complete month.
Example: 1 day after pledge date at ₹200/month => ₹200.

### COMPLETED_MONTHS_PLUS_DAYS
Charge complete calendar months, then remaining calendar days at monthly charge / 30.
Example: 1 complete month + 15 days at ₹200/month => ₹300.

## Calendar behavior
- Start and end are treated as local calendar dates, normalized to midday to avoid DST-midnight edge cases.
- Same date means zero elapsed interest.
- End before start is invalid and must fail.
- Complete months are calendar-month increments, not fixed 30-day blocks.

## Compound percentage interest
- Optional interval in calendar months.
- Supported UI presets planned: 1, 2, 3, 6, 12, 24, 36 months plus Custom.
- At every full interval boundary, the interval's interest is capitalized into running principal.
- Remaining time after the last complete compound interval follows the selected period rule without premature capitalization.

Example: ₹10,000 at 2% monthly, compounded monthly for 2 months:
- Month 1 interest ₹200 => new principal ₹10,200
- Month 2 interest ₹204 => total ₹10,404
- Total interest ₹404

## Persistence
`InterestTermsCodec` writes version marker `GKINT1` plus mode, rate, flat charge, period rule and compound interval. Unknown or malformed versions decode to null instead of guessing.

Every advance in Alpha 25C must store its own terms snapshot. Changing shop defaults later must never rewrite historical terms.

## Required UI preview
Before save, UI should show:
- principal
- selected interest mode
- monthly percentage or flat monthly amount
- monthly charge
- period rule
- compound interval if enabled
- example amount as of a chosen preview date

## Required settlement output
Settlement must expose:
- original principal
- elapsed days
- completed months and remaining days
- compound periods applied
- calculated interest
- total payable before recorded payments/discounts

## Safety invariants
- Negative principal/rate/flat charge invalid.
- Compound interval must be positive.
- Flat + compound invalid.
- End before start invalid.
- Arithmetic overflow fails closed.
- Existing Girvi records without `GKINT1` terms continue to use legacy monthly-rate behavior until explicitly migrated; migration must never silently change historical totals.
