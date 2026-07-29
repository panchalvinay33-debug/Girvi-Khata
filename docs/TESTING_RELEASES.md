# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations, and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records, and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling, and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

## v0.9.0-alpha.9

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

Build source:

- Commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- Android workflow run: `30480566880`
- Security Guard run: `30480566699`
- Artifact ID: `8735582885`
- Package: `com.girvikhata.app.testing`
- Version code/name: `9` / `0.9.0-testing`
- APK size: `20,025,854 bytes`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`

Verified scope:

- Customer khata now opens a full profile view with saved mobile and address.
- Customer name, mobile, and address can be edited and persist in the encrypted store.
- Customer-name edits propagate to linked girvi display names while IDs and accounting links remain unchanged.
- Duplicate normalized mobile numbers are rejected.
- Customer deletion is enabled only for customers with no active or released girvi history.
- Girvi-history customers remain protected from deletion.
- Collection reports add Android date pickers for exact custom From/To dates.
- Custom From dates begin at local midnight and To dates include the complete final day through 23:59:59.999.
- Today, 7-day, 30-day, all-time, CSV share, receipt share, reports, backup, restore, and PIN recovery remain present.

Owner physical-test checklist:

1. Install over Alpha 8 without uninstalling.
2. Confirm the PIN recovery/new PIN from Alpha 8 still works.
3. Confirm existing customers, girvi, payments, reports, backup and restore remain.
4. Open `Girvi Tools Test` → Reports → Khata.
5. Select a customer and edit name, mobile, and address.
6. Close and reopen Reports; edits must persist.
7. Confirm linked girvi rows show the updated customer name.
8. Try assigning a mobile number already used by another customer; save must be rejected.
9. Confirm a customer with any girvi history cannot be deleted.
10. Create an unused customer in the main app and confirm only that customer can be deleted from Khata.
11. In Collections choose Custom Date Range and select exact From/To dates.
12. Confirm receipts from the final selected day are included.
13. Select From after To and confirm the range is rejected/returns no destructive behavior.
14. Share the custom-range CSV and open it in Sheets/Excel.
15. Check scrolling, keyboard overlap, dialog clipping and app restart persistence.

Known limitations:

- Alpha 8 PIN recovery and verified restore still require owner physical confirmation.
- Main app and Tools remain two launcher entries inside one package.
- Customer create remains in the main girvi flow; Alpha 9 adds edit/delete to Reports Khata.
- Privacy screenshot/recent-app blocking is still pending app-wide.
- Google Drive automatic backup and the final transactional encrypted relational database remain pending.

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

Known limitations:

- Root cause of the owner's Alpha 7 stored-verifier failure cannot be proven remotely from source alone; Alpha 8 adds a safe recovery path and stronger verifier validation.
- Internal pre-restore safety copies are removed if the app is uninstalled; an external `.gkb` backup remains mandatory.
- Google Drive automatic upload, read-back verification, retention, and scheduled backup are pending.
- Main app and Tools remain two launcher entries inside the same package; final single-navigation refactor is pending.
- Persistence remains the interim encrypted snapshot store rather than the final transactional encrypted relational database.

## v0.7.0-alpha.7

**Status: SUPERSEDED — OWNER REPORTED PREVIOUS PIN NOT ACCEPTED**

- Commit: `e87ec9b252e0f2470eddbc7408abb694d48c0bb1`
- Android workflow run: `30426181772`
- Security Guard run: `30426181774`
- Artifact ID: `8713757434`
- Version code/name: `7` / `0.7.0-testing`
- APK SHA-256: `5f87cf94b8bf10331e153b1cfbf7a0e568498c96fde93bc83901d02a12581177`
- Existing previously configured PIN was not accepted on the physical device; superseded by Alpha 8.

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
