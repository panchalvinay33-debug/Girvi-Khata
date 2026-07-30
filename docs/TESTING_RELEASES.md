# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations, and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records, and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling, and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

## v0.13.0-alpha.13

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

Build source:

- Commit: `e3a745c1e376f08f5d929fc8f42502dc96db70e9`
- Android workflow run: `30514393001`
- Security Guard run: `30514393005`
- Artifact ID: `8748366094`
- Package: `com.girvikhata.app.testing`
- Version code/name: `13` / `0.13.0-testing`
- APK size: `20,091,418 bytes`
- APK SHA-256: `7f6d42226dfe973f068f4027749b4a112fec823b8d6a31131ee613b13dda92ca`

Verified scope:

- Backup uses Android `CreateDocument` instead of treating an opened share chooser as completion.
- The encrypted package is internally decrypt/read-back verified before the document picker opens.
- The selected Files/Drive document URI is written directly by the app.
- The same URI is read back with a 128 MB allocation bound.
- Written bytes must be exactly identical to the prepared package.
- Read-back bytes must authenticate/decrypt with the recovery phrase and preserve the expected schema and complete snapshot payload.
- Only successful same-URI read-back verification records the package SHA/counts and resets `changes since backup`.
- Picker cancellation, provider open/write/read failure, empty/truncated/changed files, decrypt failure or schema/payload mismatch leave backup-due status unchanged.
- Recovery phrase characters are kept only in memory while the picker/save operation is pending and are overwritten on success, failure, cancellation or activity destruction.
- `ExternalBackupVerification` tests cover success, changed bytes, truncation, wrong expected schema and wrong recovery phrase.
- Existing accounting, reporting, backup crypto, restore, corruption recovery and safety-journal tests remained green.
- Android compilation, stable signing, APK packaging/upload, Security Guard, artifact ZIP and APK integrity passed.

Owner physical-test checklist:

1. Install directly over Alpha 12 without uninstalling.
2. Confirm existing PIN, fingerprint, customers, girvi, payments, reports, restore, journal and one launcher icon remain.
3. Open Tools → Data Safety Status and note the current backup-due/change count.
4. Open Encrypted Backup, verify PIN and enter a strong recovery phrase twice.
5. Cancel the Files picker once; Safety Status must remain due/unchanged.
6. Repeat and save to phone Downloads/Files; wait for the explicit `External backup verified` message.
7. Refresh Safety Status; timestamp/SHA should update and changes-since-backup should be zero.
8. Confirm the `.gkb` file exists and has non-zero size in the selected folder.
9. Repeat save to Google Drive through the Android picker and confirm same-URI verification succeeds.
10. Temporarily remove network access and observe provider behavior honestly; failure must not reset backup status.
11. Use the saved `.gkb` in Restore preview with the same phrase and verify counts/checksum before cancelling or using disposable test data.
12. Report picker cancellation bugs, false success, zero/truncated files, provider incompatibility, crashes, data loss or wrong Safety Status.

Known limitations:

- Same-URI read-back proves the Android document provider returned the exact bytes written at verification time; it cannot prove that a remote cloud provider later completed server synchronization or cross-device retention.
- Process death while the picker is open cancels the in-memory pending operation; the recovery phrase is deliberately not persisted.
- Business journal entries remain aggregate committed-state entries until the final transactional database layer provides exact transaction metadata.
- Local journal/safety copies remain device-bound and disappear on uninstall/device loss.
- Automatic Google Drive API authorization, upload/read-back verification, retention and restore discovery remain pending.
- Persistence is still the interim encrypted snapshot store rather than the final transactional encrypted relational database.

## v0.12.0-alpha.12

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

Build source:

- Commit: `316db12c4710830687f264bc7e11242ba67a8842`
- Android workflow run: `30512974288`
- Security Guard run: `30512974297`
- Artifact ID: `8747878288`
- Package: `com.girvikhata.app.testing`
- Version code/name: `12` / `0.12.0-testing`
- APK size: `20,075,034 bytes`
- APK SHA-256: `2fba24b1e971ed517f15bf9ea0996478ad8e369357d050fbb01178fd2e77461e`

Verified scope:

- PIN-protected `Data Safety Status` inside the internal Tools hub.
- Separate Android-Keystore AES-256-GCM encrypted journal with SHA-256 hash chaining.
- Business-store verification/recovery state, journal validity, last verified package timestamp/SHA and changes-since-backup are visible.
- Backup becomes due when none was verified, five committed changes occurred, or seven days elapsed.
- Process-lifetime `FileObserver` records aggregate events only after the committed primary encrypted business file changes; temporary/safety/quarantine/journal files are ignored.
- Duplicate file-system callbacks are deduplicated by encrypted-file SHA and timing.
- Portable backup creation performs in-memory decrypt/read-back and schema comparison before Android share opens.
- Verified package SHA/counts reset changes-since-backup to zero.
- Authenticated PIN recovery appends a separate journal event.
- Safety activity is internal and requires the existing app PIN.
- Raw PIN, recovery phrase and plaintext business/backup contents are not written to the journal.
- Journal retains the latest 500 entries; retained segments verify from their preserved previous-hash anchor so pruning does not cause a false chain failure.
- New SHA and backup-due tests passed alongside all existing accounting, reporting, backup, restore and recovery tests.
- Android compilation, stable signing, APK packaging/upload, Security Guard, artifact ZIP and APK integrity passed.

Known limitations:

- Alpha 12 opened a share chooser and could not prove that the user completed an external save; Alpha 13 supersedes this completion signal with direct document write/read-back verification.
- Journal and local business safety copies remain device-bound and disappear on uninstall/device loss.
- Persistence is still an encrypted snapshot file rather than the final transactional encrypted relational database.
- Automatic Google Drive upload/read-back verification and retention remain pending.

## v0.11.0-alpha.11

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `ed866d3580e747108ed5a4a53a0b36798a3eba6a`
- Android workflow run: `30485337748`
- Security Guard run: `30485348627`
- Artifact ID: `8737489498`
- Package: `com.girvikhata.app.testing`
- Version code/name: `11` / `0.11.0-testing`
- APK SHA-256: `a1f95823e34ca86cb762e2d545984ca28699a1c470089203bb03fce24f4f0741`
- Strict encrypted-store verification, rotating safety copies, quarantine and explicit recovery screen added.

## v0.10.0-alpha.10

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `fad678462cf801e682acf6565fccf4978248fde7`
- Android workflow run: `30482752429`
- Security Guard run: `30482752531`
- Artifact ID: `8736432865`
- Package: `com.girvikhata.app.testing`
- Version code/name: `10` / `0.10.0-testing`
- APK SHA-256: `a50da4c6c678723f5d9b0284d8423d97af009823266564b0dbc11378e2f9ed60`
- Single launcher, internal Tools and app-wide FLAG_SECURE added.

## v0.9.0-alpha.9

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`
- Customer profile/edit/delete safety and exact custom collection dates owner-approved.

## v0.8.0-alpha.8-pin-recovery

**Status: VERIFIED TESTING BUILD — FUNCTIONALLY INCLUDED IN OWNER-APPROVED ALPHA 9**

- Commit: `c289c31b94ed260d6a686c93ee45489f359ac628`
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
