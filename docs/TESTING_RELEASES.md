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

### v0.2.0-alpha.2-fixed

Status: VERIFIED TESTING BUILD — OWNER TEST PENDING

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

Owner test focus:

1. Install beside the older `Girvi Khata` Alpha 1 app.
2. Confirm the new icon/name is `Girvi Khata Test`.
3. Create a PIN in the testing app.
4. Add a category and create a girvi with test-only values.
5. Confirm dashboard, customer search, and girvi list update.
6. Lock/unlock and fully close/reopen the testing app; verify data remains.
7. Keep this testing app installed so the next APK can be tested as a direct update.

Known limitations:

- The old Alpha 1 PIN/data cannot migrate automatically because it belongs to a different package/signing identity.
- Persistence is an encrypted snapshot store, not yet the final transaction-safe relational database.
- Multiple items per girvi are not yet available.
- Existing customers cannot yet be selected from a dedicated picker.
- Category edit/deactivate is pending.
- Payment posting and settlement are pending.
- Biometric unlock and lifecycle auto-lock are pending.
- Google Drive backup/restore is pending.
- PDF receipts and printing are pending.

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

Not yet included:

- Production local database.
- Real customer/girvi persistence.
- New-girvi wizard.
- Biometric unlock.
- Google Drive backup/restore.
- PDF receipts and printing.
