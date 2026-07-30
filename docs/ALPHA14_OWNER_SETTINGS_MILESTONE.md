# Alpha 14 — Owner Settings Milestone

Status: CI-verified testing build; owner physical test pending.

## Delivered

- PIN-protected Owner Settings inside the internal Tools hub.
- Configurable auto-lock: immediately, 30 seconds, 1 minute, or 5 minutes.
- Owner-controlled biometric unlock toggle; PIN remains available.
- PIN verifier diagnostic state without exposing verifier bytes or secrets.
- Category rename with whitespace normalization and case-insensitive duplicate rejection.
- Category rename propagation to legacy girvi category fields and all linked girvi items.
- Category up/down ordering with safe boundary no-op behavior.
- MainActivity refreshes security preferences when returning from settings.
- Settings activity is internal and protected by the app-wide secure-window policy.

## Verification

- Source commit: `74d43dada5c3634365760f80ddd4a6e203a6c537`
- Android workflow: `30515811575`
- Security Guard: `30515811548`
- Artifact: `8748888169`
- Version: `14` / `0.14.0-testing`
- Package: `com.girvikhata.app.testing`
- APK size: `20,124,214 bytes`
- APK SHA-256: `0479ee0e38ab070f77471709a534d01e061a2b097c0aa76bc06af672bb8703c2`
- Unit tests, Android/Compose compilation, stable signing, artifact upload, ZIP integrity and APK integrity passed.

## Physical test focus

1. Install over Alpha 13 without uninstalling.
2. Confirm existing PIN, fingerprint and business records remain.
3. Wrong PIN must not open Owner Settings.
4. Toggle biometric off; return to lock screen and confirm fingerprint action is unavailable while PIN works.
5. Toggle biometric on and confirm fingerprint returns.
6. Test each auto-lock timeout using background/return behavior.
7. Rename a disposable category and confirm linked girvi details, search and reports use the new name.
8. Duplicate and blank category names must be rejected.
9. Move categories up/down, restart the app and confirm order persists.
10. Confirm backup, restore, reports, Data Safety and PIN recovery still work.

## Limitations

- Category activation/deactivation remains in the existing category workflow; Alpha 14 adds rename and order only.
- Item/unit/interest-plan/locker/custom-field management screens remain pending.
- Physical OEM biometric and lifecycle timing behavior requires owner-device confirmation.
- Storage is still the interim encrypted snapshot architecture, not the final transactional encrypted database.
