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
- Backup restore preview passed: `YES / NO`
- Customers verified: `YES / NO`
- Active girvis verified: `YES / NO`
- Closed girvis verified: `YES / NO`
- Payments verified: `YES / NO`
- Masters verified: `YES / NO`
- Reports verified: `YES / NO`

### Business-flow test

- PIN login: `PASS / FAIL`
- Biometric unlock: `PASS / FAIL / NOT AVAILABLE`
- Create customer: `PASS / FAIL`
- Classic girvi entry: `PASS / FAIL`
- Multi-item girvi entry: `PASS / FAIL`
- Partial payment: `PASS / FAIL`
- Exact custom split: `PASS / FAIL`
- Payment reversal: `PASS / FAIL`
- Final settlement and release: `PASS / FAIL`
- Backup export: `PASS / FAIL`
- Restore into test edition: `PASS / FAIL`
- Force-close and reopen recovery: `PASS / FAIL`
- Low-storage protection: `PASS / FAIL / NOT TESTED`
- Migration diagnostics screen: `PASS / FAIL`

## Verified-write observation gate

- Successful coordinated writes observed:
- Required minimum: `25`
- Fingerprint mismatches: `0 required`
- Unexplained recovery-required states: `0 required`
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
