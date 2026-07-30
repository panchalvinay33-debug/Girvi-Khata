# Alpha 16 Release Evidence

Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING

- Source commit: `9df3577f71271ebc43daf217a448a49a8966826d`
- Android workflow: `30521817958`
- Security Guard: `30521817959`
- Artifact ID: `8751141218`
- Package: `com.girvikhata.app.testing`
- Version code/name: `16` / `0.16.0-testing`
- APK size: `20,189,814 bytes`
- APK SHA-256: `0cc000de869bfcef98d998ff7eaea0fc373310853ba9a23a31feb63c56fef294`

## Verified scope

- PIN-protected Business Masters entry inside internal Tools.
- Separate Android-Keystore AES-GCM encrypted master catalog.
- Temporary encrypted write, decrypt/read-back verification and final-file verification.
- Item, unit, monthly interest-plan, payment-mode and locker/storage masters.
- Add, rename, activate/deactivate and same-kind ordering.
- Case-insensitive duplicate rejection and normalized whitespace.
- Default operational units, payment modes, interest plan and locker.
- Domain tests passed together with all existing accounting, settlement, reporting, backup, restore, recovery and journal tests.
- Android/Compose compilation, stable testing signing, artifact upload, Security Guard, artifact ZIP and APK integrity passed.

## Owner test checklist

1. Install over Alpha 15 without uninstalling.
2. Confirm existing PIN, biometric, customers, girvi, payments, settings and settlement center.
3. Open Tools → Business Masters; wrong PIN must fail and correct PIN must open.
4. Add each master type and restart the app.
5. Blank and duplicate names must reject.
6. Rename, disable/enable and reorder entries; restart and confirm persistence.
7. Add two same item names under different categories; both should be accepted.
8. Verify existing backup/restore, reports, Data Safety and PIN recovery remain unchanged.

## Known limitation

Alpha 16 manages the encrypted catalog but does not yet wire saved selections into New Girvi and Payment dialogs. The catalog is also not yet included in portable `.gkb` backup/restore. These are explicitly the next milestone.
