# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations, and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records, and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling, and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

## v0.10.0-alpha.10

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

Build source:

- Commit: `fad678462cf801e682acf6565fccf4978248fde7`
- Android workflow run: `30482752429`
- Security Guard run: `30482752531`
- Artifact ID: `8736432865`
- Package: `com.girvikhata.app.testing`
- Version code/name: `10` / `0.10.0-testing`
- APK size: `20,025,846 bytes`
- APK SHA-256: `a50da4c6c678723f5d9b0284d8423d97af009823266564b0dbc11378e2f9ed60`

Verified scope:

- `MainActivity` is the only exported launcher activity.
- The previous `Girvi Tools Test` launcher entry is removed; Tools remains an internal activity.
- The main app contains a floating Settings/Tools button that opens Reports, Backup, Restore and PIN Recovery.
- Tools remains reachable from the lock screen so authenticated PIN recovery is not blocked by an unusable PIN verifier.
- Reports and Backup retain PIN verification; PIN recovery retains biometric/device-credential authentication.
- A central `GirviKhataApplication` applies Android `FLAG_SECURE` to every activity window.
- Screenshots, normal screen recording and readable recent-app previews should be blocked where the device honors `FLAG_SECURE`.
- All unit tests, Android compilation, stable testing signing, APK packaging, artifact upload and Security Guard passed.

Owner physical-test checklist:

1. Install directly over Alpha 9 without uninstalling.
2. Confirm existing PIN, customers, girvi, payments, reports, backup files and restore behavior remain.
3. Confirm the launcher now shows only `Girvi Khata Test`; the old Tools icon should disappear after launcher refresh/restart.
4. Open the main app and tap the floating Settings/Tools button.
5. Confirm Reports, Encrypted Backup, Restore and PIN Recovery open from the Tools hub.
6. Confirm Reports and Backup still reject a wrong PIN and accept the correct PIN.
7. From the main lock screen, open Tools and confirm PIN Recovery still requires fingerprint/device credential.
8. Attempt screenshots on lock, customer, girvi, report, backup and restore screens; capture should be blocked/blank according to device behavior.
9. Open recent apps and confirm no readable customer/business data appears in the preview.
10. Confirm the floating Tools button does not hide important save/payment/release controls.
11. Return from Tools and verify the 30-second background auto-lock still works.
12. Report duplicate icons, exposed previews, screenshot capture, missing Tools access, data loss, overlay obstruction or crashes.

Known limitations:

- Tools is opened as an internal activity from a floating button rather than rendered as a native bottom-navigation page; this minimizes regression risk while removing the second launcher icon.
- OEM and accessibility capture behavior may differ, so privacy blocking requires physical-device confirmation.
- Google Drive automatic backup and the final encrypted transactional database remain pending.

## v0.9.0-alpha.9

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

Owner confirmation: testing completed successfully; development may continue from the customer-profile and custom-date baseline.

Build source:

- Commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- Android workflow run: `30480566880`
- Security Guard run: `30480566699`
- Artifact ID: `8735582885`
- Package: `com.girvikhata.app.testing`
- Version code/name: `9` / `0.9.0-testing`
- APK size: `20,025,854 bytes`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`

Verified and approved scope:

- Customer khata profile with saved mobile/address and financial totals.
- Customer name, mobile and address edits persist in encrypted storage.
- Customer-name changes propagate to linked girvi display names without changing accounting IDs.
- Duplicate normalized mobile numbers are rejected.
- Only unused customers can be deleted; all girvi history remains protected.
- Collection reports support exact Android From/To date pickers.
- Final selected date includes the full local day through 23:59:59.999.
- Existing reports, CSV/receipt sharing, backup, restore and PIN recovery remained functional in the owner's check.

## v0.8.0-alpha.8-pin-recovery

**Status: VERIFIED TESTING BUILD — FUNCTIONALLY INCLUDED IN OWNER-APPROVED ALPHA 9**

- Commit: `c289c31b94ed260d6a686c93ee45489f359ac628`
- Android workflow run: `30477959563`
- Security Guard run: `30477959615`
- Artifact ID: `8734522129`
- Package: `com.girvikhata.app.testing`
- Version code/name: `8` / `0.8.0-testing`
- APK SHA-256: `74965918d7a97c33f28faaad8df81486a0342788009d603c4de5057f984a3d96`
- Added authenticated data-preserving PIN recovery and strict portable `.gkb` restore with pre-restore safety backup and post-save verification.

## v0.7.0-alpha.7

**Status: SUPERSEDED — OWNER REPORTED PREVIOUS PIN NOT ACCEPTED**

- Commit: `e87ec9b252e0f2470eddbc7408abb694d48c0bb1`
- Android workflow run: `30426181772`
- Security Guard run: `30426181774`
- Artifact ID: `8713757434`
- Version code/name: `7` / `0.7.0-testing`
- APK SHA-256: `5f87cf94b8bf10331e153b1cfbf7a0e568498c96fde93bc83901d02a12581177`
- Existing previously configured PIN was not accepted on the physical device; superseded by Alpha 8 recovery.

## v0.4.0-alpha.4

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `6d39fbd6dec11204e340e40284659993faa612d5`
- Workflow run: `30412018790`
- Artifact ID: `8708824816`
- Package: `com.girvikhata.app.testing`
- Version code/name: `4` / `0.4.0-testing`
- APK SHA-256: `6bb19e1eefd2192fac811616db891f55cd8ac889e18937c1cbb9ac4012eb2306`
- Payment receive, allocation, reversal, release, encrypted schema v3, and dashboard payment/release totals owner-approved.

## v0.3.0-alpha.3

**Status: VERIFIED TESTING BUILD — FUNCTIONALLY SUPERSEDED BY OWNER-APPROVED ALPHA 4**

- Commit: `2fded9557a039714c07c01ccb058af0c9f3bcfed`
- Workflow run: `30387546934`
- Artifact ID: `8699655919`
- APK SHA-256: `f8d76b01c23de35fc10c1c8c0348fd9137f5eba9a227a8d16290fc07bb3042ce`

## v0.2.0-alpha.2-fixed

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `15c5ccbe096042a15a06116c22ecec6f8236281a`
- Workflow run: `30384904918`
- Artifact ID: `8698642645`
- Package: `com.girvikhata.app.testing`
- APK SHA-256: `ac923b42ffa2968ad3a95a60075814f57a26cde932ac7d549cbab3305748c63f`

## v0.2.0-alpha.2

**Status: SUPERSEDED — INSTALL CONFLICT FOUND**

Used the original package with a different ephemeral debug key. Replaced by `v0.2.0-alpha.2-fixed`.

## v0.1.0-alpha.1

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `a9037dbab10af729a6ed2c298a47fc74e250c09d`
- Workflow run: `30374188211`
- Artifact ID: `8694333535`
- APK SHA-256: `09b2ee05518e17654f9f5e81a75eadc76d43c1f0a6642573a572bc179c143c9c`
