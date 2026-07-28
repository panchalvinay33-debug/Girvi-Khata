# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before it is shared.

## Release requirements

A testing APK is shareable only when:

- Android unit tests pass.
- Debug APK build succeeds.
- Security Guard workflow passes.
- APK artifact is downloaded and its archive/file structure is verified.
- Commit SHA and workflow run are recorded.
- Known limitations are listed honestly.
- No OAuth secrets, signing keys, production data, customer photos, databases, or backups are bundled.

## Test checklist for the owner

For every APK, test in this order:

1. Install/upgrade succeeds.
2. Existing PIN and encrypted records remain.
3. Correct PIN and fingerprint unlock work.
4. Wrong PIN increments failed attempts.
5. Home, Customers, Girvi, Masters, and More tabs open.
6. Manual and automatic app lock work.
7. New records remain after full close/reopen.
8. No crash occurs while navigating all available screens.

## Testing releases

### v0.3.0-alpha.3

Status: VERIFIED TESTING BUILD — OWNER TEST PENDING

Build source:

- Commit: `2fded9557a039714c07c01ccb058af0c9f3bcfed`
- Workflow run: `30387546934`
- Artifact ID: `8699655919`
- Testing package: `com.girvikhata.app.testing`
- Version code: `3`
- Version name: `0.3.0-testing`
- APK size: `19,795,774 bytes`
- APK SHA-256: `f8d76b01c23de35fc10c1c8c0348fd9137f5eba9a227a8d16290fc07bb3042ce`

Verification completed:

- Android unit tests passed.
- Compose and Android source compilation passed.
- Stable testing keystore was restored and validated.
- Signed debug APK build passed.
- Artifact upload passed.
- Security Guard passed on the same code milestone and again on the documentation head.
- Downloaded artifact contained exactly one APK.
- APK archive integrity check reported no errors.
- File identified as an Android package.

Included scope:

- Direct update over the owner-approved Alpha 2 testing package.
- Encrypted schema v1-to-v2 compatibility for existing Alpha 2 records.
- Existing-customer search/picker with mobile/address fill.
- Multiple girvi items with add/remove.
- Category, item name, quantity, gross weight, deduction, net preview, and description per item.
- Multiple-item encrypted persistence and dashboard totals.
- Girvi details with all items and weight breakup.
- Adjustable month-wise simple-interest calculation details.
- Category activate/deactivate UI with active-girvi protection.
- Strong-biometric/fingerprint prompt when the device supports and has it enrolled.
- Thirty-second background auto-lock.
- Improved customer search by name, mobile, and address.
- Improved date-wise girvi number sequencing.

Owner test focus:

1. Install this APK directly over `Girvi Khata Test` Alpha 2; do not uninstall.
2. Confirm the existing PIN still works.
3. Confirm previously saved category, customer, and girvi records remain.
4. Lock the app and test fingerprint unlock.
5. Put the app in the background for more than 30 seconds and confirm it locks.
6. Create a girvi by selecting an existing customer.
7. Create another girvi with two or more items.
8. Test gross weight, deduction, and net-weight preview.
9. Close/reopen and confirm all item rows remain.
10. Open a girvi and change the calculation month count.
11. Try deactivating a category used by an active girvi; it must be blocked.
12. Deactivate and reactivate an unused category.
13. Check all tabs for crash, overlap, or scrolling problems.

Known limitations:

- Persistence remains the encrypted snapshot store, not yet the final transaction-safe relational database.
- Category rename is not yet available.
- Customer edit/profile ledger is not yet complete.
- Payment posting, part-payment, settlement, and release are pending.
- Interest calculation UI currently exposes the simple monthly preview; all configured engine modes are not yet selectable here.
- Google Drive backup/restore is pending.
- PDF receipts and printing are pending.

### v0.2.0-alpha.2-fixed

Status: VERIFIED TESTING BUILD — OWNER APPROVED

Owner approval:

- Installed successfully as `Girvi Khata Test`.
- PIN setup/unlock passed.
- Category creation passed.
- Girvi creation and encrypted saving passed.
- Dashboard totals, customer search, and girvi listing passed.
- Manual lock/unlock passed.
- Full close/reopen persistence passed.
- Approved as the permanent testing-package baseline for future in-place upgrades.

Build source:

- Commit: `15c5ccbe096042a15a06116c22ecec6f8236281a`
- Workflow run: `30384904918`
- Artifact ID: `8698642645`
- Testing package: `com.girvikhata.app.testing`
- App label: `Girvi Khata Test`
- APK size: `19,664,493 bytes`
- APK SHA-256: `ac923b42ffa2968ad3a95a60075814f57a26cde932ac7d549cbab3305748c63f`

Verification completed:

- Android unit tests passed.
- Stable testing keystore restored/generated and validated before build.
- Debug APK compilation and signing passed.
- Artifact upload passed.
- Security Guard passed.
- Downloaded artifact contained exactly one APK.
- APK archive integrity test reported no errors.

Signing/update policy:

- This is the first permanent `Girvi Khata Test` package baseline.
- It intentionally installs beside the old Alpha 1 package and does not conflict with it.
- Future testing APKs must keep this package ID, signing identity, and increasing version code so they install as updates and preserve testing data.
- Production signing and production package identity remain separate.

Included scope:

- Everything from Alpha 1.
- App-private AES-256-GCM encrypted business snapshot storage.
- Android Keystore protected encryption key.
- Persistent customer records.
- Persistent custom categories.
- Persistent girvi records.
- Working new-girvi form with customer, mobile, address, category, item, weight, principal, and monthly interest rate.
- Basic matching/reuse of an existing customer.
- Automatic girvi number generation.
- Dashboard totals from saved records.
- Saved customer search and saved girvi listing.
- One-month and six-month interest preview.

### v0.2.0-alpha.2

Status: SUPERSEDED — INSTALL CONFLICT FOUND

- This build was technically valid, but used the original package ID with a different ephemeral debug signing key.
- Android correctly rejected installation over Alpha 1.
- Replaced by `v0.2.0-alpha.2-fixed` with a dedicated testing package and stable testing signing process.

### v0.1.0-alpha.1

Status: VERIFIED TESTING BUILD — OWNER APPROVED

Build source:

- Commit: `a9037dbab10af729a6ed2c298a47fc74e250c09d`
- Workflow run: `30374188211`
- Artifact ID: `8694333535`
- APK size: `19,631,697 bytes`
- APK SHA-256: `09b2ee05518e17654f9f5e81a75eadc76d43c1f0a6642573a572bc179c143c9c`

Verification completed:

- Android unit tests passed.
- Debug APK compilation passed.
- Artifact upload passed.
- Security Guard passed.
- Downloaded artifact contained one APK.
- APK ZIP structure integrity test reported no errors.
- File identified as a valid Android package.

Included scope:

- PIN enrollment and verification.
- Progressive failed-attempt lockout.
- Dashboard and bottom navigation.
- Customer list/search prototype.
- Girvi list prototype.
- Custom master overview.
- Security/settings screen.
- Pure Kotlin interest, payment allocation, validation, and PIN tests.
