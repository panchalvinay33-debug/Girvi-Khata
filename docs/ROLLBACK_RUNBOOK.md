# Girvi Khata Rollback Runbook

Approved recovery base: `baseline/alpha21-owner-approved`
Exact tested source: `eec78e0aba6a8d168baeb09959fe93e2fd85733f`
Approved APK SHA-256: `65a5f56771c120b9f11e102e0eeaea7c086544c7a381a781879cf9c43ebead12`

## Purpose

Use this runbook when work after Alpha 21 causes a crash, failed update, corrupted workflow, migration mismatch or unacceptable regression. The first goal is to protect owner data; the second is to restore application code.

## Before changing code

1. Do not uninstall the installed app.
2. Create and externally verify a `.gkb` backup whenever the current app can still open.
3. Record the failing APK version, source commit, device model, Android version and reproduction steps.
4. Preserve logs without copying PINs, recovery phrases, real customer data or `.gkb` files into the public repository.

## Preferred recovery path: forward fix

Create a repair branch from the approved baseline or the latest known-good post-baseline commit. Fix the defect, run complete CI, build a higher version-code APK and install it as an update. This preserves Android app-private data and signing continuity.

## Exact code rollback

The immutable code reference is:

- Branch: `baseline/alpha21-owner-approved`
- Commit: `eec78e0aba6a8d168baeb09959fe93e2fd85733f`

Create a new recovery branch from that reference. Do not rewrite `main`, force-push the baseline branch or erase later history. Reverting later commits or opening a recovery PR is preferred over resetting shared branches.

## APK rollback limitation

Android normally blocks installation of a lower version code over a higher installed version. Therefore the original Alpha 21 APK may not install directly over Alpha 22+.

Safe rollback build procedure:

1. Start from the Alpha 21 baseline source.
2. Keep the same testing application ID and stable testing signing key.
3. Set a version code higher than the currently installed failing APK.
4. Change only the version metadata and any minimum compatibility repair required.
5. Run full tests, Security Guard, artifact integrity and checksum verification.
6. Install as an update without uninstalling.

This produces an Alpha-21-code recovery APK while preserving owner data.

## Data recovery order

1. Try normal app launch and automatic local safety-copy recovery.
2. Use the explicit Data Recovery Required screen if shown.
3. Restore the newest externally verified `.gkb` package.
4. Confirm customers, girvis, payments and masters before new entries.
5. Rebuild and verify the relational shadow from the authoritative encrypted snapshot.

The relational shadow database is rebuildable and must never be treated as the only recovery source at this baseline.

## Verification after rollback

- PIN and biometric unlock work.
- Existing customers, categories, girvis, items and payments remain.
- Reports and settlement totals match.
- Backup and restore preview work.
- Data Safety Status journal remains readable.
- Database Migration Status shows matching counts/fingerprint after rebuild.
- No extra launcher icon appears.
- Screenshot/recent-app privacy remains active where supported.

## Governance

Every recovery APK needs a unique version, exact source commit, CI evidence, SHA-256 and owner test record. Returning to the Alpha 21 code does not automatically authorize a production merge; the repair still follows the normal owner-approval gate.
