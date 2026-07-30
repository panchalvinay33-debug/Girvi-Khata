# Alpha 22 Release Evidence — Relational Master IDs

Date: 2026-07-30
Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING

## Exact build identity

- Version code/name: `22` / `0.22.0-testing`
- Package: `com.girvikhata.app.testing`
- Exact APK source commit: `5ee98f358159c74820230da6b3f29a23577345c7`
- Android workflow: `30548491236` — SUCCESS
- Security Guard: `30548495430` — SUCCESS
- Artifact ID: `8761882280`
- Artifact digest: `sha256:0f02342b06c0828d308ee06eec75b99cd66f67f18f6e01e55eacec1fd0d0b2d4`
- APK size: `20,419,274 bytes`
- APK SHA-256: `4b083897615869af974a176804579fc39ed841725aaaca8d9eb774cf07eb27fa`
- Artifact ZIP integrity: passed
- APK ZIP/archive integrity: passed
- File classification: valid Android APK

## Implemented scope

- Dedicated relational master-ID schema contract for Item, Unit, Locker, Interest Plan and Payment Mode.
- Idempotent non-destructive schema installer for the rebuildable relational shadow.
- Encrypted `masters` rows using Android-Keystore AES-GCM for master name/category values.
- Nullable relational link columns so manual, missing or ambiguous values remain safe and unresolved.
- Deterministic master catalog validation and stable-ID canonical ordering.
- First-load default master catalog is immediately persisted in the encrypted catalog store so UUIDs remain stable.
- Deterministic backfill resolves item/category, unit, locker, interest rate and payment mode links.
- Ambiguous same-rate interest plans or duplicate normalized payment modes remain null instead of being guessed.
- Automatic master synchronization and link verification after successful relational business sync.
- Exact journal events for master-link success or failure.
- PIN-protected Database Migration Status card showing schema version, encrypted master count and link coverage.
- Manual `Install / Verify Master-ID Links` dashboard action.

## Automated evidence

- Existing accounting, interest, payment, reversal, settlement, report, backup, restore, corruption-recovery and journal tests passed.
- Relational schema non-destructive SQL tests passed.
- Dedicated column/index completeness tests passed.
- Duplicate master-ID and invalid-rate tests passed.
- Deterministic backfill tests passed.
- Ambiguous plan/mode safe-null tests passed.
- Android and Compose compilation passed.
- Stable testing signing and artifact upload passed.
- Security Guard passed.

## Owner physical test order

1. Install over Alpha 21 without uninstalling.
2. Verify existing PIN, fingerprint, customers, girvi, payments, masters and reports.
3. Open Tools → Database Migration Status with correct PIN.
4. Confirm existing dual-read status remains readable.
5. Tap `Install / Verify Master-ID Links`.
6. Confirm schema shows `v2` and encrypted master row count is non-zero.
7. Confirm Item, Unit, Locker, Interest Plan and Payment Mode coverage values are displayed.
8. Existing/manual records may show unresolved links; this is allowed and must not delete or alter records.
9. Create a disposable girvi using saved Item, Unit, Locker and Interest Plan masters.
10. Post a payment using a saved Payment Mode.
11. Refresh migration status and verify link counts increase appropriately.
12. Restart the app and verify schema/coverage remains readable.
13. Check Data Safety Status for relational master-link verification events.
14. Create a verified external `.gkb` backup.
15. Regression-test settlement, reports, restore preview and auto-lock.

## Safety and rollback boundary

- Owner-approved Alpha 21 remains `main` and the stable rollback base.
- Permanent rollback branch remains `baseline/alpha21-owner-approved`.
- Alpha 22 is isolated in draft PR #2 and is not merged.
- The Android-Keystore encrypted snapshot remains the sole source-of-truth.
- The relational database remains rebuildable shadow data.
- No relational read/write cutover is authorized.
- Alpha 22 must not merge until owner physical testing and explicit approval.

## Known limitations

- Existing manual metadata may remain unresolved.
- Link coverage is diagnostic and does not change accounting behavior.
- SQLite is not whole-file SQLCipher encrypted; sensitive text cells are encrypted individually.
- The schema installer is idempotent but still requires owner-device upgrade testing.
- Relational cutover remains blocked.
