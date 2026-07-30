# Alpha 10 — Single-App Navigation and Privacy Milestone

Status: DEVELOPMENT BUILD — AUTOMATED VALIDATION PENDING

## Owner approval carried forward

The owner reported Alpha 9 testing complete and instructed development to continue. Alpha 9 is therefore the latest owner-approved testing milestone. Alpha 10 remains outside `main` until its own physical-device test and explicit approval.

## Visible changes

- Only `Girvi Khata Test` remains as a launcher entry.
- The former `Girvi Tools Test` activity remains internal and is opened from the main app through a floating Tools button.
- Tools continues to provide Reports, encrypted backup creation, verified restore/import, and authenticated PIN recovery.
- The Tools entry is intentionally available from the lock screen so an owner whose PIN verifier is unusable can still reach biometric/device-credential PIN recovery.
- Reports, backup and restore retain their existing PIN/authentication gates.

## Privacy hardening

A central `GirviKhataApplication` lifecycle guard applies Android `FLAG_SECURE` to every activity window. On supported Android behavior this blocks:

- screenshots,
- ordinary screen recording/capture,
- readable recent-app thumbnails.

The policy covers the main app, PIN screens, Tools, Reports, Backup, Restore and PIN Recovery without relying on every activity to remember a separate flag.

## Android component boundary

- `MainActivity` is the only exported launcher activity.
- `ToolsActivity`, `ReportsActivity`, `BackupActivity`, `RestoreActivity` and `PinRecoveryActivity` are internal (`exported=false`).
- FileProvider remains internal and grants temporary read permission only for owner-initiated sharing.

## Upgrade requirements

- Package remains `com.girvikhata.app.testing`.
- Stable testing signature remains unchanged.
- Version code/name: `10` / `0.10.0-testing`.
- Install directly over Alpha 9; uninstalling deletes app-private test data.

## Physical test checklist

1. Install over Alpha 9 without uninstalling.
2. Confirm existing PIN, customers, girvi, payments, reports and backup data remain.
3. Confirm the launcher shows only `Girvi Khata Test`; the old Tools icon should disappear.
4. Open the main app and tap the floating Settings/Tools button.
5. Confirm Reports, Backup, Restore and PIN Recovery open from the Tools hub.
6. Verify Reports and Backup still require the correct PIN.
7. From the main app lock screen, confirm Tools can open and PIN Recovery still requires fingerprint/device credential.
8. Attempt an Android screenshot on lock, customer, girvi, report, backup and restore screens; capture should be blocked/blank according to device behavior.
9. Open Android recent apps and confirm the app preview does not expose readable business data.
10. Test back navigation and 30-second auto-lock after returning from Tools.
11. Report any duplicate icon, missing Tools button, screenshot exposure, overlay obstruction, crash or lost data.

## Known limitations

- Tools opens as an internal activity rather than being rendered as a native bottom-navigation page; this avoids a risky giant UI rewrite while removing the second launcher icon.
- Some OEM or accessibility capture behavior may differ; physical-device verification is mandatory.
- Google Drive automation and the final encrypted transactional database remain pending.
