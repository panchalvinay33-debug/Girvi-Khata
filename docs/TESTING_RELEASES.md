# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

## v0.18.0-alpha.18

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `93772dab32df673abefd4edbee1eb4a6cf8c6823`
- Android workflow: `30531484305`
- Security Guard: `30531484326`
- Artifact ID: `8754962903`
- Package: `com.girvikhata.app.testing`
- Version code/name: `18` / `0.18.0-testing`
- APK size: `20,337,330 bytes`
- APK SHA-256: `abf3800536166380e1532cfc973c30170f467f16ea880beaf5058ed042699416`

Verified scope:

- New PIN-protected Advanced Multi-Item & Custom Split activity inside internal Tools.
- One girvi can contain 1–50 validated items.
- Each item supports category, saved/manual item name, saved unit metadata, quantity, gross weight, deduction, locker metadata and note.
- Duplicate category+item pairs are rejected case-insensitively inside the same transaction.
- Negative/oversized weights and deduction greater than gross weight are rejected.
- Added-item cart supports review and removal before save.
- Existing encrypted business snapshot remains the save authority and all items remain portable in `.gkb` backup/restore.
- Custom payment allocation supports exact principal, interest and charges components.
- Custom components must exactly equal the entered payment and cannot exceed their current due balances.
- Existing SettlementEngine remains authoritative for posting and overpayment rejection.
- Exact tamper-evident journal labels are added after successful multi-item creation and custom-split payment.
- Classic master-assisted workflow remains available as a fallback.
- Transaction-core tests, all previous accounting/backup/restore/security tests, Compose compilation, stable signing, Security Guard and APK upload passed.
- Artifact ZIP integrity and APK archive integrity passed.

Owner checklist:

1. Install over Alpha 17 without uninstalling.
2. Confirm existing PIN, fingerprint, customers, girvi, payments and Business Masters.
3. Open Tools → Advanced Multi-Item & Custom Split; wrong PIN must fail and correct PIN must unlock.
4. Add two different items to one disposable girvi and verify both appear in details/reports after save.
5. Try the same category+item with different letter case; duplicate must fail.
6. Try deduction greater than gross weight; item add must fail.
7. Add an item, remove it, and confirm only remaining items are saved.
8. Choose an active girvi and verify principal/interest/charges due preview.
9. Post a custom split whose components exactly match the payment amount and dues.
10. Try a mismatched split and a component greater than due; both must fail without saving.
11. Check Data Safety Status for exact multi-item/custom-split audit events and backup-due increment.
12. Create a verified external `.gkb` backup and preview it in Restore; multi-item records must remain intact.
13. Restart and regression-test classic entry, settlement, reports, backup, restore and auto-lock.

Known limitations:

- Unit, locker and interest-plan labels remain schema-compatible item-description metadata until relational database columns are added.
- Existing-customer search/reuse is richer in the classic master-assisted workflow; Alpha 18 advanced screen currently creates a new customer from entered details.
- Item removal is immediate inside the unsaved draft cart and does not yet use a confirmation dialog.
- Custom split uses current settlement-month due preview; owner must choose the correct month count.
- Physical-device scrolling, keyboard behavior and OEM lifecycle remain owner-test pending.
- Persistence remains the interim encrypted snapshot architecture rather than the final transactional relational database.

## v0.17.0-alpha.17

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `9845284c3eb39e251bdb66e4ab367046256c105f`
- Android workflow: `30528710554`
- Security Guard: `30528710551`
- Artifact ID: `8753862752`
- Package: `com.girvikhata.app.testing`
- Version code/name: `17` / `0.17.0-testing`
- APK size: `20,271,762 bytes`
- APK SHA-256: `f5d4bb65f439034f5ee0096c7ae62d80bb9033163038f28dcc3acf423f4c2aa4`

Verified scope:

- PIN-protected Master-Assisted Girvi & Payment workflow inside internal Tools.
- Active Item, Unit, Interest Plan and Locker masters are selectable during girvi creation.
- Existing-customer search/reuse and manual item/rate fallback remain available.
- Selected interest-plan basis points become the account monthly rate.
- Selected payment mode is written to the immutable payment ledger.
- Principal-first and interest-first payment allocation are available.
- Portable `.gkb` backup now contains both business snapshot and encrypted master catalog.
- New bundle restore replaces business and masters after preview/read-back verification.
- Legacy snapshot-only backups remain readable and preserve the current master catalog.
- Pre-restore safety backup now contains business records and masters together.
- Portable master codec rejects duplicate IDs, invalid kinds, invalid rates and oversized catalogs.
- Legacy top-level girvi items receive deterministic portable IDs derived from stable girvi IDs, making repeat backup bytes and SHA deterministic.
- Android CI runs on PR and development-branch pushes; full unit tests, Compose build, stable signing and artifact upload passed.
- Security Guard, artifact ZIP integrity and APK integrity passed.

Owner checklist:

1. Install over Alpha 16 without uninstalling.
2. Confirm existing PIN, fingerprint, customers, girvi, payments and masters.
3. Create/enable Item, Unit, Interest Plan, Payment Mode and Locker entries in Business Masters.
4. Open Master-Assisted Girvi & Payment; wrong PIN must fail and correct PIN must unlock.
5. Create a disposable girvi using saved item, unit, plan and locker.
6. Reopen details and verify item description metadata and exact interest rate.
7. Receive a payment using a saved custom payment mode; verify receipt and allocation.
8. Create an external `.gkb` backup and confirm success message includes master count.
9. Preview the new backup in Restore; verify business and master counts.
10. Verify a legacy Alpha 8–16 backup preview says current masters will be preserved; do not destructively restore real data without a safe copy.
11. Restart the app and regression-test Reports, Settlement, Safety Status, Owner Settings and corruption recovery.

Known limitations:

- Master-assisted girvi entry currently creates one item; the original main workflow still supports multiple items.
- Unit, locker and plan labels are stored in the schema-compatible item description until the relational database migration adds dedicated columns.
- Custom payment split remains in the original payment workflow; the master-assisted screen offers interest-first or principal-first.
- Physical-device update, scrolling, Files/Drive provider and restore behavior remain owner-test pending.
- Persistence remains the interim encrypted snapshot architecture rather than the final transactional relational database.

## v0.14.0-alpha.14

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `74d43dada5c3634365760f80ddd4a6e203a6c537`
- Android workflow: `30515811575`
- Security Guard: `30515811548`
- Artifact ID: `8748888169`
- Package: `com.girvikhata.app.testing`
- Version code/name: `14` / `0.14.0-testing`
- APK size: `20,124,214 bytes`
- APK SHA-256: `0479ee0e38ab070f77471709a534d01e061a2b097c0aa76bc06af672bb8703c2`

Verified scope:

- PIN-protected Owner Settings inside internal Tools.
- Auto-lock choices: immediate, 30 seconds, 1 minute and 5 minutes.
- Biometric unlock toggle while PIN remains available.
- PIN verifier health diagnostic without exposing secrets.
- Category rename with normalized whitespace and case-insensitive duplicate rejection.
- Rename propagation to legacy girvi category and linked item categories.
- Category up/down reorder with boundary no-op safety.
- MainActivity refreshes settings on return and applies selected timeout on the next background cycle.
- Category operation tests and all previous accounting/reporting/backup/restore/safety tests passed.
- Android/Compose compilation, stable signing, artifact upload, Security Guard, ZIP integrity and APK integrity passed.

Owner checklist:

1. Install over Alpha 13 without uninstalling.
2. Confirm old PIN, fingerprint and all business records.
3. Wrong PIN must not open Owner Settings.
4. Disable biometric; confirm fingerprint unlock disappears and PIN works.
5. Enable biometric; confirm fingerprint unlock returns.
6. Test all four auto-lock timeout options.
7. Rename a disposable category and confirm linked girvi/search/report names update.
8. Blank and duplicate names must fail.
9. Reorder categories and confirm persistence after restart.
10. Regression-test Reports, Backup, Restore, Safety and PIN Recovery.

Known limitations:

- Category activation/deactivation remains in the existing workflow; Alpha 14 adds rename/order only.
- Item/unit/interest-plan/payment-mode/locker/custom-field management remains pending.
- OEM lifecycle and biometric timing requires owner-device confirmation.
- Persistence remains the interim encrypted snapshot architecture.

## v0.13.0-alpha.13

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `e3a745c1e376f08f5d929fc8f42502dc96db70e9`
- Android workflow: `30514393001`
- Security Guard: `30514393005`
- Artifact ID: `8748366094`
- Version: `13` / `0.13.0-testing`
- APK size: `20,091,418 bytes`
- APK SHA-256: `7f6d42226dfe973f068f4027749b4a112fec823b8d6a31131ee613b13dda92ca`
- Direct Android document write and same-URI read-back verification; cancellation/failure does not reset backup status.

## v0.12.0-alpha.12

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `316db12c4710830687f264bc7e11242ba67a8842`
- Android workflow: `30512974288`
- Security Guard: `30512974297`
- Artifact ID: `8747878288`
- Version: `12` / `0.12.0-testing`
- APK SHA-256: `2fba24b1e971ed517f15bf9ea0996478ad8e369357d050fbb01178fd2e77461e`
- Added Data Safety Status, encrypted hash-chained journal and backup-due policy.

## v0.11.0-alpha.11

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `ed866d3580e747108ed5a4a53a0b36798a3eba6a`
- Android workflow: `30485337748`
- Artifact ID: `8737489498`
- APK SHA-256: `a1f95823e34ca86cb762e2d545984ca28699a1c470089203bb03fce24f4f0741`
- Strict encrypted-store verification, rotating safety copies, quarantine and explicit recovery screen.

## v0.10.0-alpha.10

**Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING**

- Commit: `fad678462cf801e682acf6565fccf4978248fde7`
- Android workflow: `30482752429`
- Artifact ID: `8736432865`
- APK SHA-256: `a50da4c6c678723f5d9b0284d8423d97af009823266564b0dbc11378e2f9ed60`
- Single launcher, internal Tools and app-wide secure-window policy.

## v0.9.0-alpha.9

**Status: VERIFIED TESTING BUILD — OWNER APPROVED**

- Commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`
- Customer profile/edit/delete safety and exact custom collection dates approved.

## Earlier retained milestones

- `v0.8.0-alpha.8-pin-recovery`: authenticated PIN recovery and strict portable restore; SHA `74965918d7a97c33f28faaad8df81486a0342788009d603c4de5057f984a3d96`.
- `v0.7.0-alpha.7`: superseded after owner reported previous PIN was not accepted.
- `v0.4.0-alpha.4`: owner-approved payment/release baseline; SHA `6bb19e1eefd2192fac811616db891f55cd8ac889e18937c1cbb9ac4012eb2306`.
- `v0.3.0-alpha.3`: functionally superseded by Alpha 4.
- `v0.2.0-alpha.2-fixed`: owner-approved stable-signature update baseline.
- `v0.2.0-alpha.2`: superseded install-conflict build.
- `v0.1.0-alpha.1`: owner-approved initial testing baseline.
