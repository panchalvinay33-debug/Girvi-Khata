# Owner Approval — Alpha 21 Stable Baseline

Approval date: 2026-07-30
Owner: Vinay Panchal
Repository: `panchalvinay33-debug/Girvi-Khata`

## Approval decision

The owner physically tested `v0.21.0-alpha.21` and explicitly approved it as the new project base. The complete reviewed development work through Alpha 21 is authorized for merge to `main`.

All future development must begin from this approved baseline. If a later milestone introduces a crash, data regression, install failure or unusable workflow, the project must be recoverable to this baseline without rewriting history.

## Exact tested APK source

- Source commit: `eec78e0aba6a8d168baeb09959fe93e2fd85733f`
- Protected rollback branch: `baseline/alpha21-owner-approved`
- Package: `com.girvikhata.app.testing`
- Version code/name: `21` / `0.21.0-testing`
- APK size: `20,402,894 bytes`
- APK SHA-256: `65a5f56771c120b9f11e102e0eeaea7c086544c7a381a781879cf9c43ebead12`
- Android workflow: `30543127987`
- Security Guard: `30543128181`
- Artifact ID: `8759651865`
- Artifact digest: `584432831011b866850bc6db62c65a1d2f884907a7cabeca16a2ccf0890c7157`

## Approved functional scope

- PIN, biometric unlock, configurable auto-lock and authenticated PIN recovery.
- Customers, categories, duplicate-mobile protection and customer khata.
- Classic and advanced multi-item girvi entry.
- Item, Unit, Interest Plan, Payment Mode and Locker masters.
- Interest calculation, manual interest adjustment, settlement and release.
- Immutable payment ledger, linked reversals and exact custom split.
- Reports, custom date filters, CSV/text sharing and settlement receipts.
- Encrypted `.gkb` backup/restore with master catalog and same-URI verification.
- Corruption recovery, rotating safety copies and encrypted hash-chained journal.
- Incremental relational shadow database, dual-read verification and device rollback/benchmark diagnostics.

## Storage authority at this baseline

The Android-Keystore encrypted snapshot remains the authoritative business store. The relational SQLite database remains a verified shadow and is not approved as the source-of-truth. Future database cutover requires a separate explicit owner approval.

## Merge authorization

This approval authorizes PR #1 to be marked ready and merged into `main`. It does not authorize future milestones to merge automatically. Every later milestone must continue using:

`new development branch → CI → versioned APK → owner physical test → explicit approval → merge`

## Permanent rollback identity

Never force-move or delete `baseline/alpha21-owner-approved`. It identifies the exact source used to build the owner-tested Alpha 21 APK. Documentation commits and the final merge commit may be newer, but code rollback must always be able to target this branch or exact commit.
