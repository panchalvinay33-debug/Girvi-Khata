# Alpha 23 Release Evidence — Atomic Verified Business Writes

Date: 2026-07-30
Status: CI-verified owner-installable testing candidate; physical owner test pending

## Stable bases

- Owner-approved `main`: Alpha 21
- Permanent rollback: `baseline/alpha21-owner-approved`
- Frozen Alpha 22 source: `candidate/alpha22-testing`
- Alpha 23 branch: `agent/alpha23-relational-write-api`
- Draft stacked PR: #3

## Exact final source evidence

- Source commit: `ff8c198d804fa600a66025c694cebef434dc11c3`
- Version code: `23`
- Version name: `0.23.0-testing`
- Package: `com.girvikhata.app.testing`
- Android workflow: `30561422718` — SUCCESS
- Security Guard: `30561422668` — SUCCESS
- APK artifact: `8767152080`
- Artifact ZIP size: `19,583,417 bytes`
- Artifact digest: `sha256:3e558a52ba0a8a2add0b8bfc66a3999532edc03c5866297e605063e0e363e356`
- APK size: `20,435,662 bytes`
- APK SHA-256: `2a21e6f1d6200cee6227cdc8314d3b56ca4c49e278d6f1907b308ce052c7822b`
- Artifact ZIP integrity: PASS
- APK archive integrity: PASS

## Implemented

- Optimistic expected-snapshot fingerprint precondition.
- Immutable transaction UUIDs.
- Authoritative encrypted snapshot save/read-back verification.
- Incremental relational sync plus independent dual-read fingerprint proof.
- Verified customer, girvi, payment and restore mutation primitives.
- Classic Master-Assisted girvi and payment workflow routed through the coordinator.
- Atomic single-request customer + girvi creation using `CreateGirviWithCustomer`.
- Existing matching customers are reused without creating duplicates.
- On-screen Refresh Data action for stale-screen recovery.
- Short transaction ID shown after successful classic girvi/payment writes.
- Cross-girvi duplicate receipt/payment protection.
- Released-girvi payment protection.
- Minimum 25 successful coordinated-write observation policy for future relational cutover.

## Atomic business invariant

The Classic Master-Assisted new-girvi path now submits customer and girvi in one verified coordinator request. The authoritative encrypted snapshot either receives both records together or receives neither. The previous two-stage customer-then-girvi partial-commit gap is closed.

## Verification

- Full unit-test suite passed.
- Atomic customer+girvi tests passed.
- Android/Compose compilation passed.
- Stable testing signing passed.
- APK build and artifact upload passed.
- Security Guard passed.
- Artifact ZIP was independently extracted.
- APK archive integrity test passed.
- APK SHA-256 and byte size were independently recorded.

## Phone test focus

1. Install over Alpha 22 without uninstalling.
2. Verify existing PIN, fingerprint, customers, girvis and payments.
3. Open Tools → Classic Master-Assisted Entry.
4. Create a girvi with a brand-new disposable customer.
5. Confirm both customer and girvi appear together and the success message contains a short TX ID.
6. Create a girvi for an existing customer and confirm no duplicate customer appears.
7. Receive a saved-mode payment and confirm its receipt and TX ID.
8. Open Data Safety Status and match the `VERIFIED_BUSINESS_WRITE` event to the displayed transaction ID.
9. Keep the screen open, make a business change elsewhere, return and attempt another write; stale write must be blocked until Refresh Data is used.
10. Verify reports, backup and restore regressions.

## Governance

- PR #3 remains draft.
- No Alpha 23 merge to `main` without explicit owner approval.
- Alpha 21 remains the approved base and permanent rollback reference.
- Alpha 22 remains frozen as a separate testing candidate.
- Snapshot remains sole source-of-truth.
- Relational cutover remains blocked.
