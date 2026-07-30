# Alpha 23 Release Evidence — Verified Business Writes

Date: 2026-07-30
Status: CI-verified development candidate; physical owner test pending

## Stable bases

- Owner-approved `main`: Alpha 21
- Permanent rollback: `baseline/alpha21-owner-approved`
- Frozen Alpha 22 source: `candidate/alpha22-testing`
- Alpha 23 branch: `agent/alpha23-relational-write-api`
- Draft stacked PR: #3

## Exact final source evidence

- Source commit: `45c331ea09e91eb00aba099d802521f22cf04c78`
- Version code: `23`
- Version name: `0.23.0-testing`
- Package: `com.girvikhata.app.testing`
- Android workflow: `30552955962` — SUCCESS
- Security Guard: `30552955902` — SUCCESS
- APK artifact: `8763695914`
- Artifact ZIP size: `19,583,296 bytes`
- Artifact digest: `sha256:834dbf9a1a66bdd2113c56bb2af8800c9ec71e4b9a6490bdee40b85aaba790b9`

## Implemented

- Optimistic expected-snapshot fingerprint precondition.
- Immutable transaction UUIDs.
- Authoritative encrypted snapshot save/read-back verification.
- Incremental relational sync plus independent dual-read fingerprint proof.
- Verified customer, girvi, payment and restore mutation primitives.
- Classic Master-Assisted girvi and payment workflow routed through the coordinator.
- On-screen Refresh Data action for stale-screen recovery.
- Short transaction ID shown after successful classic girvi/payment writes.
- Cross-girvi duplicate receipt/payment protection.
- Released-girvi payment protection.
- Atomic `CreateGirviWithCustomer` mutation and regression tests.
- Minimum 25 successful coordinated-write observation policy for future relational cutover.

## Important exact boundary

The classic screen is coordinator-wired, but its new-customer path at this exact source still invokes customer and girvi as two consecutive verified coordinator requests. The atomic `CreateGirviWithCustomer` primitive exists and is fully tested, but the screen has not yet been switched to that single primitive. Therefore a second-stage failure could leave a newly-created customer without the girvi. This is non-destructive and recoverable, but the single-call UI switch remains required before Alpha 23 is promoted for owner testing.

## Verification

- Full unit-test suite passed.
- New atomic customer+girvi tests passed.
- Android/Compose compilation passed.
- Stable testing signing passed.
- APK build and artifact upload passed.
- Security Guard passed.

## Artifact limitation

The GitHub artifact ZIP is available and has an authoritative GitHub digest. The local execution runtime repeatedly failed before ZIP extraction, so direct APK byte size, APK archive test and APK SHA-256 were not independently recorded in this evidence. Do not treat this as the final owner-installable Alpha 23 APK evidence yet.

## Governance

- PR #3 remains draft.
- No Alpha 23 merge to `main`.
- Alpha 21 remains the approved production-development base.
- Alpha 22 remains the current phone-testing candidate.
- Snapshot remains sole source-of-truth.
- Relational cutover remains blocked.
