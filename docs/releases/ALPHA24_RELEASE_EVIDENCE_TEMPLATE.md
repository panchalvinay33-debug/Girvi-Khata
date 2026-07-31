# Alpha 24 Release Evidence

> Complete this file only from one consolidated signed workflow run and one physical owner test. Do not copy evidence from an older commit or APK.

## Source identity

- Branch: `agent/alpha24-production-hardening`
- Commit SHA:
- Build date/time (UTC):
- Workflow run ID:
- Workflow run URL:

## Automated verification

- Security Guard: `PASS / FAIL`
- `testDebugUnitTest`: `PASS / FAIL`
- `compileDebugKotlin`: `PASS / FAIL`
- Signing identity restore: `PASS / FAIL`
- `assembleDebug`: `PASS / FAIL`
- APK certificate verification: `PASS / FAIL`
- Artifact upload: `PASS / FAIL`

## APK identity

- Artifact name: `girvi-khata-testing-apk`
- APK filename:
- Application ID: `com.girvikhata.app.testing`
- Version code: `24`
- Version name: `0.24.0-testing`
- APK size (bytes):
- APK SHA-256:
- Signing certificate SHA-256:

## Archive verification

- Downloaded artifact ZIP SHA-256:
- ZIP integrity test: `PASS / FAIL`
- Extracted APK SHA-256 matches recorded value: `YES / NO`

## Migration and physical owner test

Before installing Alpha 24, export and retain a verified encrypted `.gkb` backup from the owner-approved Alpha 21 app.

- Alpha 21 backup exported: `YES / NO`
- Additional offline backup retained: `YES / NO`
- Backup restore preview passed: `YES / NO`
- Customers verified: `YES / NO`
- Active girvis verified: `YES / NO`
- Closed girvis verified: `YES / NO`
- Payments verified: `YES / NO`
- Masters verified: `YES / NO`
- Reports verified: `YES / NO`

### Business-flow test

- PIN setup and unlock: `PASS / FAIL`
- Wrong-PIN and lock handling: `PASS / FAIL`
- Biometric unlock: `PASS / FAIL / NOT AVAILABLE`
- Create and edit customer: `PASS / FAIL`
- Customer search: `PASS / FAIL`
- Classic girvi entry: `PASS / FAIL`
- Multi-item girvi entry: `PASS / FAIL`
- Partial payment: `PASS / FAIL`
- Exact custom split: `PASS / FAIL`
- Payment reversal: `PASS / FAIL`
- Final settlement and release: `PASS / FAIL`
- Category add, rename, activate and reorder: `PASS / FAIL`
- Backup export: `PASS / FAIL`
- Restore into test edition: `PASS / FAIL`
- Force-close and reopen recovery: `PASS / FAIL`
- Device restart recovery: `PASS / FAIL`
- Low-storage protection: `PASS / FAIL / NOT TESTED`
- Migration diagnostics screen: `PASS / FAIL`

## Restore-generation interruption matrix

Use test-only data. Keep the pre-restore `.gkb` safety backup until all cases pass.

### Portable backup: uninterrupted

- Business snapshot restored: `PASS / FAIL`
- Master catalog restored: `PASS / FAIL`
- Business fingerprint matches target: `YES / NO`
- Master fingerprint matches target: `YES / NO`
- No pending restore generation after completion: `YES / NO`

### Legacy backup

- Business snapshot restored: `PASS / FAIL`
- Existing master catalog preserved: `YES / NO`
- No pending restore generation after completion: `YES / NO`

### Process interruption

- Kill before business activation; relaunch completes safely: `PASS / FAIL`
- Kill after business activation but before master activation; relaunch completes relational proof and masters: `PASS / FAIL`
- Kill after both targets activate but before cleanup; relaunch verifies and cleans generation: `PASS / FAIL`
- Normal business writes remain blocked while generation is unresolved: `PASS / FAIL`
- No duplicate customer, girvi or payment after reconciliation: `YES / NO`

### Failure handling

- Damaged or mismatched staged target blocks recovery: `PASS / FAIL / NOT TESTED`
- Unknown business fingerprint blocks recovery: `PASS / FAIL / NOT TESTED`
- Unknown master fingerprint blocks recovery: `PASS / FAIL / NOT TESTED`
- Recovery message is clear and no silent mixed generation is shown: `PASS / FAIL`
- Pre-restore safety backup remains readable: `PASS / FAIL`

## Verified-write observation gate

Perform at least 25 successful coordinated writes across classic, advanced, settlement, category and restore flows.

- Successful coordinated writes observed:
- Required minimum: `25`
- Fingerprint mismatches: `0 required`
- Unresolved pending business intents: `0 required`
- Unresolved restore generations: `0 required`
- Unexplained recovery-required states: `0 required`
- Authoritative snapshot and relational shadow match: `YES / NO`
- Relational cutover remains blocked: `YES`

## Owner decision

- Owner tested APK: `YES / NO`
- Owner approval date:
- Decision: `APPROVED / REJECTED / MORE TESTING REQUIRED`
- Notes:

## Safety confirmation

- `main` still points to owner-approved Alpha 21 until explicit approval: `YES`
- `baseline/alpha21-owner-approved` preserved: `YES`
- Private keystore/passwords are absent from repository and artifacts: `YES`
- No real customer backup is attached to GitHub: `YES`
- PR #5 remains draft until explicit approval: `YES`
