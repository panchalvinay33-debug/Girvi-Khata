# Girvi Khata Testing Release Ledger

Every testing APK must be recorded here before sharing. A build is shareable only after unit tests, Android build, stable testing signing, Security Guard, artifact download, APK integrity verification, checksum recording, known limitations, and an owner test checklist.

## Mandatory owner test order

1. Install as an update; do not uninstall the existing `Girvi Khata Test` app.
2. Verify the existing PIN, fingerprint, customers, categories, girvi records, and payments remain.
3. Test every new workflow, invalid input, close/reopen persistence, scrolling, and crash behavior.
4. Code stays outside `main` until the owner explicitly approves the milestone.

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
