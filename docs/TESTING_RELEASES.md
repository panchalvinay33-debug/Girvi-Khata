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
2. First launch shows PIN enrollment.
3. Weak PIN is rejected.
4. PIN confirmation mismatch is rejected.
5. Correct PIN unlocks the app.
6. Wrong PIN increments failed attempts.
7. Home, Customers, Girvi, Masters, and More tabs open.
8. Manual app lock returns to the PIN screen.
9. App relaunch asks for PIN.
10. No crash occurs while navigating all available screens.

## Testing releases

### v0.2.0-alpha.2

Status: VERIFIED TESTING BUILD — OWNER TEST PENDING

Build source:

- Commit: `91100e60bcbb23d3c61311a3dad43d902d7df1bc`
- Workflow run: `30375884064`
- Artifact ID: `8695024443`
- APK size: `19,664,465 bytes`
- APK SHA-256: `6007547cfc5bcaaba283ae57e5344604b046b1969350bca83685f9995704317f`

Verification completed:

- Android unit tests passed.
- Debug APK compilation passed.
- Artifact upload passed.
- Security Guard passed.
- Downloaded artifact contained exactly one APK.
- File identified as a valid Android package.

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

Owner test focus:

1. Install Alpha 2 over Alpha 1.
2. Existing PIN should continue working.
3. Add a new category.
4. Create a girvi with realistic test values only.
5. Confirm dashboard totals update.
6. Confirm customer appears in search.
7. Confirm girvi appears in Girvi list.
8. Lock the app, unlock again, and verify records remain.
9. Fully close the app, reopen, unlock, and verify records remain.
10. Create a second girvi for the same mobile number and confirm customer reuse behaves correctly.
11. Check blank, zero, negative-looking, very large, and decimal values for validation problems.
12. Confirm no crash occurs across all tabs.

Known limitations:

- Persistence is an encrypted snapshot store, not yet the final transaction-safe relational database.
- Multiple items per girvi are not yet available.
- Existing customers cannot yet be selected from a dedicated picker.
- Category edit/deactivate is pending.
- Payment posting and settlement are pending.
- Biometric unlock and lifecycle auto-lock are pending.
- Google Drive backup/restore is pending.
- PDF receipts and printing are pending.

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

Not yet included:

- Production local database.
- Real customer/girvi persistence.
- New-girvi wizard.
- Biometric unlock.
- Google Drive backup/restore.
- PDF receipts and printing.
