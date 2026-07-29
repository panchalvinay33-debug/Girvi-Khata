# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations, and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records, and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling, and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

## v0.11.0-alpha.11

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

Build source:

- Commit: `ed866d3580e747108ed5a4a53a0b36798a3eba6a`
- Android workflow run: `30485337748`
- Security Guard run: `30485348627`
- Artifact ID: `8737489498`
- Package: `com.girvikhata.app.testing`
- Version code/name: `11` / `0.11.0-testing`
- APK size: `20,042,234 bytes`
- APK SHA-256: `a1f95823e34ca86cb762e2d545984ca28699a1c470089203bb03fce24f4f0741`

Verified scope:

- The encrypted local store no longer converts corruption into an empty/default khata.
- Normal saves validate schema, duplicate IDs/numbers, customer links, statuses, principal and timestamps before writing.
- Existing valid primary is copied into rotating encrypted pre-save safety storage; latest five copies are retained.
- New primary writes use a temporary file, filesystem sync, decrypt/decode comparison before replacement and final primary read-back verification.
- Envelope magic, format version, file size, IV length, ciphertext length and trailing bytes are validated before decryption/allocation.
- A damaged primary automatically tries newest safety copies and promotes the first verified copy.
- Damaged primary bytes are quarantined; latest two quarantine files are retained.
- If no valid local copy remains, Dashboard is blocked and `Data Recovery Required` is shown instead of empty records.
- Main recovery screen opens verified `.gkb` restore and supports retry without reinstalling.
- Tools blocks Reports and new-backup creation during corruption while Restore and PIN Recovery remain available.
- Restore can quarantine a fully corrupt primary and install a verified portable snapshot.
- Valid current data still receives an encrypted pre-restore safety backup; file bytes and restored record counts are checked.
- New pure tests cover envelope bounds, retention order, duplicate girvi numbers and missing customer links.
- Unit tests, Compose/Android build, stable signing, APK upload, Security Guard, artifact ZIP and APK integrity passed.

Owner physical-test checklist:

1. Install directly over Alpha 10 without uninstalling.
2. Confirm existing PIN, fingerprint, customers, girvi, categories, payments, reports, backup and restore remain.
3. Create or edit a dummy customer/girvi/payment and restart after each operation; data must persist.
4. Confirm no normal operation displays an unexpected empty khata.
5. Create and externally save a fresh `.gkb` backup before any fault simulation.
6. Do not damage files containing real records; use disposable dummy data/test device only.
7. With a deliberately damaged primary and a valid local safety copy, confirm automatic recovery.
8. If all local copies are deliberately damaged, confirm `Data Recovery Required` appears and Dashboard is unavailable.
9. Confirm Tools disables Reports and Backup but leaves Restore and PIN Recovery enabled.
10. Restore the external `.gkb`, return to main app and tap `Restore Ke Baad Dobara Check Karein`.
11. Confirm restored customers, girvi and ledger counts match the backup.
12. Confirm one launcher icon, Tools access, screenshot blocking and 30-second auto-lock still work.

Known limitations:

- The store is still an encrypted snapshot file, not the final transactional encrypted relational database.
- Local safety/quarantine copies are Android-Keystore/device bound and disappear on uninstall or device loss.
- External portable `.gkb` backup remains mandatory.
- Google Drive automatic upload/read-back verification remains pending.
- Deliberate fault injection must not be performed on real customer data.

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

Known limitations:

- Tools is opened as an internal activity from a floating button rather than rendered as a native bottom-navigation page.
- OEM and accessibility capture behavior may differ, so privacy blocking requires physical-device confirmation.
- Google Drive automatic backup and the final encrypted transactional database remain pending.

## v0.9.0-alpha.9

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

Owner confirmation: testing completed successfully; development may continue from the customer-profile and custom-date baseline.

- Commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- Android workflow run: `30480566880`
- Security Guard run: `30480566699`
- Artifact ID: `8735582885`
- Package: `com.girvikhata.app.testing`
- Version code/name: `9` / `0.9.0-testing`
- APK size: `20,025,854 bytes`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`
- Customer profile/edit/delete safety and exact custom collection dates owner-approved.

## v0.8.0-alpha.8-pin-recovery

**Status: VERIFIED TESTING BUILD — FUNCTIONALLY INCLUDED IN OWNER-APPROVED ALPHA 9**

- Commit: `c289c31b94ed260d6a686c93ee45489f359ac628`
- Android workflow run: `30477959563`
- Security Guard run: `30477959615`
- Artifact ID: `8734522129`
- APK SHA-256: `74965918d7a97c33f28faaad8df81486a0342788009d603c4de5057f984a3d96`
- Added authenticated data-preserving PIN recovery and strict portable `.gkb` restore.

## v0.7.0-alpha.7

**Status: SUPERSEDED — OWNER REPORTED PREVIOUS PIN NOT ACCEPTED**

- Commit: `e87ec9b252e0f2470eddbc7408abb694d48c0bb1`
- APK SHA-256: `5f87cf94b8bf10331e153b1cfbf7a0e568498c96fde93bc83901d02a12581177`

## v0.4.0-alpha.4

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `6d39fbd6dec11204e340e40284659993faa612d5`
- APK SHA-256: `6bb19e1eefd2192fac811616db891f55cd8ac889e18937c1cbb9ac4012eb2306`

## v0.3.0-alpha.3

**Status: VERIFIED TESTING BUILD — FUNCTIONALLY SUPERSEDED BY OWNER-APPROVED ALPHA 4**

- Commit: `2fded9557a039714c07c01ccb058af0c9f3bcfed`
- APK SHA-256: `f8d76b01c23de35fc10c1c8c0348fd9137f5eba9a227a8d16290fc07bb3042ce`

## v0.2.0-alpha.2-fixed

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `15c5ccbe096042a15a06116c22ecec6f8236281a`
- APK SHA-256: `ac923b42ffa2968ad3a95a60075814f57a26cde932ac7d549cbab3305748c63f`

## v0.2.0-alpha.2

**Status: SUPERSEDED — INSTALL CONFLICT FOUND**

Used the original package with a different ephemeral debug key. Replaced by `v0.2.0-alpha.2-fixed`.

## v0.1.0-alpha.1

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `a9037dbab10af729a6ed2c298a47fc74e250c09d`
- APK SHA-256: `09b2ee05518e17654f9f5e81a75eadc76d43c1f0a6642573a572bc179c143c9c`