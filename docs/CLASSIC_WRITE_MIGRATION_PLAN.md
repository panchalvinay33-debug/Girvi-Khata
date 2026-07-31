# Classic AppRoot verified-write migration plan

Issue: #9

## Goal

Remove the final `recordStore.save(next)` gateway from `AppRoot.kt` without weakening stale-screen detection, transaction intent, relational verification, interrupted-write recovery, or audit clarity.

## Required typed mutations

| Classic flow | Required mutation | Required checks |
|---|---|---|
| Customer create/edit | `UpsertCustomer` | Stable customer ID, nonblank name, stale fingerprint rejection |
| Girvi create with new/existing customer | `CreateGirviWithCustomer` | Customer identity, unique girvi ID/number, positive principal |
| Girvi edit | `UpsertGirvi` | Existing customer link, stable girvi ID, valid status |
| Payment receive | `AppendPayment` | Active girvi, globally unique payment ID and receipt number |
| Payment reversal | dedicated `ReversePayment` | Original payment exists, not already reversed, exact negating allocation, unique reversal receipt |
| Girvi release/reopen | dedicated `ChangeGirviStatus` | Valid transition, release timestamp/note policy, no accidental payment mutation |
| Category create/edit/reorder | dedicated category mutations or verified target delta | Unique IDs/names, linked girvi category rename consistency |
| Delete operations | dedicated delete mutations | Explicit dependency checks and deterministic refusal rules |

## Migration order

1. Add missing domain mutations and deterministic reducer tests.
2. Replace `MainShell(snapshot, ::persist)` with explicit callbacks grouped by business action.
3. Every callback must use the screen snapshot fingerprint as `expectedFingerprint`.
4. Reload the authoritative encrypted snapshot after every successful coordinator result.
5. Remove the legacy `persist(next: AppSnapshot)` function.
6. Remove the temporary Security Guard exception and require zero direct business-store UI writes.
7. Run full unit tests and Android compilation before signing.

## Failure rules

- Never use `ReplaceSnapshotForRestore` for ordinary UI writes.
- Never mark a post-authoritative-write failure as safely failed unless the snapshot fingerprint proves no commit occurred.
- Never silently retry a stale UI mutation.
- Never allow a generic snapshot replacement to hide which business action occurred.

## Test matrix

- Customer create, edit, duplicate prevention and stale conflict.
- Girvi create, edit, duplicate number and missing customer.
- Payment receive, duplicate receipt, released-girvi block.
- Reversal once only, exact allocation reversal and restart recovery.
- Release/reopen transition policy and settlement totals.
- Category rename/reorder with linked girvi consistency.
- Process death before snapshot save, after snapshot save, and during relational verification.

## Release gate

Issue #9 is complete only when `AppRoot.kt` has no direct `EncryptedRecordStore.save` call, Security Guard allows no classic exception, tests and compilation pass, and owner phone validation covers every migrated classic flow.
