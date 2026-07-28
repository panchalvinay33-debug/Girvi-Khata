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

### v0.1.0-alpha.1

Status: VERIFIED TESTING BUILD

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
