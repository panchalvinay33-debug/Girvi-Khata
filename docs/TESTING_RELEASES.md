# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations, and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records, and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling, and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

## v0.8.0-alpha.8-pin-recovery

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

Build source:

- Commit: `c289c31b94ed260d6a686c93ee45489f359ac628`
- Android workflow run: `30477959563`
- Security Guard run: `30477959615`
- Artifact ID: `8734522129`
- Package: `com.girvikhata.app.testing`
- Version code/name: `8` / `0.8.0-testing`
- APK size: `19,993,086 bytes`
- APK SHA-256: `74965918d7a97c33f28faaad8df81486a0342788009d603c4de5057f984a3d96`

Owner-reported regression addressed:

- Alpha 7 did not accept the previously configured PIN on the owner's device.
- Alpha 2 through Alpha 7 retained the same preference namespace and verifier field names; no source-level package or key rename was found.
- A data-preserving recovery route is now available through `Girvi Tools Test` when the stored verifier is unusable or lockout prevents access.
- Recovery requires strong biometric authentication or the device screen credential, then replaces only the PIN verifier and clears lockout metadata.
- Customer, girvi, category, payment, reversal, release, report, and backup data are not deleted during PIN recovery.
- Stored verifier validation now checks hash length, salt length, and PBKDF2 iteration bounds; security-state writes use synchronous persistence.

Verified restore scope:

- PIN-protected `.gkb` file selection through Android's document picker.
- Recovery-passphrase decryption, authenticated tamper detection, and strict schema validation.
- Preview of customer, category, girvi, and immutable payment-ledger counts before replacement.
- Explicit destructive confirmation before current data is replaced.
- Automatic encrypted pre-restore safety backup, retaining the latest three internal copies.
- Strict validation for duplicate identifiers/numbers, missing customer links, payment allocation reconciliation, timestamps, statuses, item quantities, and supported schema.
- Encrypted local-store save followed by read-back count verification.
- Wrong passphrase, damaged file, unsupported schema, or malformed data fails before existing records are modified.

Owner physical-test checklist:

1. Install directly over Alpha 7 without uninstalling.
2. First try the old PIN in `Girvi Khata Test` and record the exact result.
3. Open `Girvi Tools Test` and choose `Purana PIN Kaam Nahi Kar Raha`.
4. Authenticate with fingerprint or the phone screen lock.
5. Set a new non-weak 6-digit PIN and confirm it.
6. Open the main app with the new PIN.
7. Verify all existing customers, girvi, categories, payments, reversals, released records, and report totals remain.
8. Create a fresh `.gkb` backup and save it outside the app.
9. Add one temporary test customer/girvi so the current data differs from the backup.
10. Open Restore, enter a wrong passphrase, and confirm current data remains unchanged.
11. Enter the correct passphrase and verify the preview counts/checksum.
12. Confirm restore only after checking the counts.
13. Reopen the main app and confirm the temporary post-backup record is gone while backed-up records are restored.
14. Verify PIN recovery does not change the backup recovery passphrase and restore does not change the app PIN.
15. Report any crash, clipped screen, keyboard overlap, incorrect count, authentication failure, or data mismatch screenshot.

Known limitations:

- Root cause of the owner's Alpha 7 stored-verifier failure cannot be proven remotely from source alone; Alpha 8 adds a safe recovery path and stronger verifier validation.
- Internal pre-restore safety copies are removed if the app is uninstalled; an external `.gkb` backup remains mandatory.
- Google Drive automatic upload, read-back verification, retention, and scheduled backup are pending.
- Main app and Tools remain two launcher entries inside the same package; final single-navigation refactor is pending.
- Persistence remains the interim encrypted snapshot store rather than the final transactional encrypted relational database.

## v0.7.0-alpha.7

**Status: SUPERSEDED — OWNER REPORTED PREVIOUS PIN NOT ACCEPTED**

Build source:

- Commit: `e87ec9b252e0f2470eddbc7408abb694d48c0bb1`
- Android workflow run: `30426181772`
- Security Guard run: `30426181774`
- Artifact ID: `8713757434`
- Package: `com.girvikhata.app.testing`
- Version code/name: `7` / `0.7.0-testing`
- APK size: `19,960,258 bytes`
- APK SHA-256: `5f87cf94b8bf10331e153b1cfbf7a0e568498c96fde93bc83901d02a12581177`

Verified build scope:

- Existing `Girvi Reports Test` launcher entry replaced by `Girvi Tools Test`.
- PIN-protected Tools hub with Reports and Encrypted Backup entries.
- Complete snapshot serializer includes customers, categories, girvi items, payments, reversals, release metadata, and adjustments.
- Visible recovery-passphrase backup creation and secure Android share flow.
- Portable AES-256-GCM package with PBKDF2-HMAC-SHA256 recovery key derivation.
- App-private temporary `.gkb` file with read-only FileProvider sharing.

Owner result:

- Existing previously configured PIN was not accepted on the physical device.
- Alpha 7 must not be used as the approved baseline.
- Superseded by Alpha 8 with data-preserving authenticated PIN recovery and verified restore.

## v0.4.0-alpha.4

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

Owner confirmation: installed and tested successfully; work may continue from this encrypted payment-ledger baseline.

Build source:

- Commit: `6d39fbd6dec11204e340e40284659993faa612d5`
- Workflow run: `30412018790`
- Artifact ID: `8708824816`
- Package: `com.girvikhata.app.testing`
- Version code/name: `4` / `0.4.0-testing`
- APK size: `19,844,926 bytes`
- APK SHA-256: `6bb19e1eefd2192fac811616db891f55cd8ac889e18937c1cbb9ac4012eb2306`

Verified scope:

- Direct upgrade with stable testing signature.
- Encrypted schema v3 with backward-compatible Alpha 2/3 defaults.
- Payment receive flow with interest-first, principal-first, and custom allocation.
- Cash, UPI, and bank modes; payment notes and automatic receipt numbering.
- Immutable payment history and linked reversal entries.
- Settlement totals and outstanding-release protection.
- Explicit owner override with mandatory release note.
- Dashboard payment/released totals.
- Modular `MainActivity` and `AppRoot` structure.

Known limitations at approval:

- Persistence is still the encrypted snapshot store, not the final transactional database.
- Reports/customer statements and shareable receipt files are not yet visible in the app.
- Google Drive backup/restore is not active.
- Production PDF/thermal printing is pending.

## v0.3.0-alpha.3

**Status: VERIFIED TESTING BUILD — FUNCTIONALLY SUPERSEDED BY OWNER-APPROVED ALPHA 4**

- Commit: `2fded9557a039714c07c01ccb058af0c9f3bcfed`
- Workflow run: `30387546934`
- Artifact ID: `8699655919`
- Version code/name: `3` / `0.3.0-testing`
- APK SHA-256: `f8d76b01c23de35fc10c1c8c0348fd9137f5eba9a227a8d16290fc07bb3042ce`
- Added existing-customer picker, multiple items, detailed interest view, category activation safety, fingerprint unlock, and 30-second background auto-lock.

## v0.2.0-alpha.2-fixed

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `15c5ccbe096042a15a06116c22ecec6f8236281a`
- Workflow run: `30384904918`
- Artifact ID: `8698642645`
- Package: `com.girvikhata.app.testing`
- APK SHA-256: `ac923b42ffa2968ad3a95a60075814f57a26cde932ac7d549cbab3305748c63f`
- Established the permanent testing package/signature and encrypted customer/category/girvi persistence baseline.

## v0.2.0-alpha.2

**Status: SUPERSEDED — INSTALL CONFLICT FOUND**

Used the original package with a different ephemeral debug key. Replaced by `v0.2.0-alpha.2-fixed`.

## v0.1.0-alpha.1

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `a9037dbab10af729a6ed2c298a47fc74e250c09d`
- Workflow run: `30374188211`
- Artifact ID: `8694333535`
- APK SHA-256: `09b2ee05518e17654f9f5e81a75eadc76d43c1f0a6642573a572bc179c143c9c`
- Added PIN enrollment/unlock, lockout, dashboard/navigation, and core calculation tests.
