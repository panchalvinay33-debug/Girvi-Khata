# Alpha 18 — Multi-Item and Custom Payment Transaction Core

Status: implementation and CI verification in progress.

## Implemented core

- Validates one to fifty item drafts for a single girvi transaction.
- Normalizes category/item whitespace while preserving item order.
- Rejects blank category/item, invalid quantity, invalid/negative/oversized weight and deduction greater than gross weight.
- Rejects duplicate category + item pairs case-insensitively inside the same transaction.
- Converts validated drafts into business `GirviItemRecord` values.
- Validates custom principal, interest and charges payment splits.
- Requires non-negative components, exact match with entered payment amount and component values not exceeding current due balances.
- Returns the existing `PaymentSplit` model consumed by `GirviSettlementUseCase.postPayment`.

## Automated tests

- Multiple-item normalization and order.
- Duplicate item rejection.
- Deduction-over-gross rejection.
- Valid custom split.
- Custom split total mismatch rejection.
- Component due-limit rejection.

## Integration plan

The verified core will be wired into the PIN-protected Master-Assisted workflow. Existing settlement and immutable payment posting logic remains authoritative; Alpha 18 does not introduce a parallel accounting engine.

## Remaining before owner APK

- Add/remove/edit multiple item rows in the Compose workflow.
- Add Custom allocation choice and principal/interest/charges inputs.
- Show live remaining due after the custom split.
- Add exact journal labels after successful girvi/payment commit.
- Complete Android CI, signed APK artifact verification and physical-test checklist.

## Governance

- Development remains on `agent/initial-foundation`.
- `main` is untouched.
- No merge is authorized without explicit owner approval.
