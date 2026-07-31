# Alpha 24 — Production Hardening Milestone

Date: 2026-07-31
Status: Near-final core code; signed APK intentionally blocked until the permanent testing identity is provisioned.

## Stable references

- Owner-approved `main`: Alpha 21
- Permanent rollback: `baseline/alpha21-owner-approved`
- Frozen Alpha 23 feature candidate: `candidate/alpha23-testing`
- Active branch: `agent/alpha24-production-hardening`
- Draft PR: #5

## Completed in Alpha 24

### Release signing governance

- Removed random and cache-generated signing identities.
- Unit tests execute before the signing gate.
- Required keystore, passwords, alias and certificate SHA-256 are explicit repository secrets.
- Keystore certificate is checked before APK assembly.
- Final APK certificate is checked again before artifact upload.
- Missing or mismatched signing identity prevents APK creation and upload.

### Persistent verified-write evidence

- Added device-local non-customer observation storage.
- Successful coordinated-write count survives app restarts.
- Last transaction ID, commit time and snapshot/relational fingerprints persist.
- Failures preserve the last successful proof while recording failure time/reason.
- Cutover observation policy consumes the persistent evidence.

### Advanced workflow migration

- Removed direct `EncryptedRecordStore.save` from Advanced Multi-Item and Custom Split workflows.
- Multi-item customer + girvi creation uses one atomic `CreateGirviWithCustomer` verified transaction.
- Custom split payment uses isolated `AppendPayment` verified transaction.
- Stale screen fingerprint blocks writes until Refresh Data is used.
- Successful operations show a short transaction ID.
- Blank custom split components safely map to zero paise without invalid money parsing.

### Verified restore transaction

- Restore still creates a verified pre-restore safety backup or quarantines a damaged primary.
- Restore enforces at least 64 MB free space or three times the estimated portable bundle size, whichever is larger.
- Restore storage policy is pure and covered by boundary tests.
- Business snapshot replacement uses `ReplaceSnapshotForRestore` through `VerifiedBusinessWriteCoordinator`.
- Authoritative snapshot read-back and relational dual-read proof are mandatory.
- Portable Business Masters retain independent encrypted save/read-back verification.
- Restore success shows the coordinated transaction ID.

### Interrupted-write recovery

- Every coordinated write persists a pending transaction intent before business mutation.
- The target snapshot fingerprint is persisted before the authoritative snapshot save begins.
- Process interruption can therefore be classified as pre-write, post-snapshot, or unknown state.
- Startup reconciliation never replaces the authoritative snapshot.
- Pre-write interruption is marked safe to retry.
- Post-snapshot interruption completes relational dual-read verification and observation evidence.
- Unknown fingerprints remain blocked and create explicit recovery-required journal evidence.
- Recovery decisions and target-intent ordering are covered by pure tests.

### Migration Status dashboard

- Shows verified-write progress against the 25-write minimum.
- Shows latest transaction ID and commit time.
- Shows latest snapshot and relational fingerprint evidence.
- Shows latest coordinated-write failure evidence.
- Adds verified-write blockers to the combined cutover blocker list.
- Relational cutover remains impossible from the screen.

## Verification evidence

Alpha 24 source repeatedly completed the full unit-test task successfully. The signing step then failed closed because permanent repository secrets are not yet provisioned. APK assembly and upload were correctly skipped. Security Guard passed.

This is the intended release-safety behavior: code can be tested, but no installable artifact can be promoted under an unknown or changing certificate.

## Remaining core gates

1. Provision one permanent testing keystore and five GitHub repository secrets outside the public repository.
2. Run full signed APK assembly and verify package/version/certificate/SHA-256.
3. Run the consolidated settlement/reversal/report/backup suite on the final signed source.
4. Perform one unified owner phone-test round including migration, restore, advanced workflows and restart recovery.
5. Accumulate real-device coordinated-write observations after the consolidated build.
6. Explicit owner approval before any merge to `main`.

## Deferred modules after first safe stable release

- PDF receipts
- Bluetooth/thermal printing
- Encrypted photo/document vault
- Automatic Google Drive backup
- Relational source-of-truth cutover after observation and explicit owner approval

## Governance

- No merge to `main` without explicit owner approval.
- No signing keys, passwords, OAuth credentials, customer data or `.gkb` files in the public repository.
- Snapshot remains the sole source-of-truth.
- Alpha 21 remains the emergency rollback base.
