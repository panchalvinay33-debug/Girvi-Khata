# Girvi Khata — Project Completion Audit

Last updated: 2026-07-30 after Alpha 14.

Percentages are engineering estimates, not marketing claims. They separate daily-use functionality from production hardening.

## Overall estimates

- Core daily girvi workflow: **80% complete**
- Security, local recovery and portable backup: **78% complete**
- Reports, customer khata and exports: **75% complete**
- Owner customization and operational UX: **58% complete**
- Production storage, rollout and disaster recovery: **42% complete**
- Whole product toward a safe first stable release: **about 65% complete**

## Completed or substantially complete

- PIN enrollment/verification, weak-PIN rejection, lockout and authenticated PIN recovery.
- Strong biometric unlock, configurable biometric toggle and configurable auto-lock timeout.
- Android Keystore protected AES-GCM local records and secure-window policy.
- Customer, category and girvi persistence; multiple items, weights and descriptions.
- Customer profile editing, duplicate-mobile protection and history-safe deletion.
- Category create/activate/deactivate, rename and reorder with linked record propagation.
- Interest calculation foundations, settlement preview and payment allocation.
- Payment posting, receipt numbers, modes, immutable history and linked reversals.
- Release checks, owner override and release metadata.
- Search, customer khata, portfolio/collection reports, exact custom dates, CSV and text sharing.
- Portable encrypted `.gkb` package creation and strict restore/import.
- Same-document external write/read-back verification before backup status is reset.
- Rotating local safety copies, corrupt-store quarantine and explicit recovery-required state.
- PIN-protected Data Safety dashboard and encrypted hash-chained operational journal.
- Single launcher, internal Tools hub, stable testing signature, Android CI and Security Guard.

## Major work still required for first stable release

### Highest priority

1. Replace the encrypted whole-snapshot store with a transaction-safe encrypted relational database and tested migration.
2. Add exact transaction audit records generated inside database transactions rather than aggregate file-observer events.
3. Run owner physical tests for Alpha 10–14 and fix all device-specific regressions.
4. Make the repository private; enable branch protection and required checks.
5. Establish private production signing, release build verification and pilot rollout procedure.

### Important product completion

6. Saved interest-plan management and selection inside the girvi wizard.
7. Item/unit/payment-mode/locker/status/custom-field management screens.
8. Manual interest adjustment UI with mandatory reason and exact audit event.
9. Final settlement/release checklist and production receipt.
10. PDF receipt, customer statement and thermal-printer support.
11. Photo/document media vault with encrypted storage, limits and backup inclusion.
12. User-visible local safety-copy management and rollback.

### Backup and reliability

13. Owner-authorized Google Drive API integration after repository privacy change.
14. Upload/read-back/hash/manifest verification, retention and cloud backup discovery.
15. WorkManager scheduling, retry/backoff and backup notifications.
16. Low-storage, interrupted-write, large-data, clock-change, offline, uninstall/reinstall and device-loss test matrix.
17. Root/device-integrity warning and final security review.

### UX and accessibility

18. Consolidate Tools as native main-app navigation instead of a floating-button activity hop.
19. Accessibility labels, font scaling, keyboard behavior, loading states and broader screen-size testing.
20. Final Hindi/English wording review, empty states and onboarding/help.

## Explicitly outside the first stable release

- Multi-user staff accounts
- Live multi-device synchronization
- Web dashboard
- Central customer database
- iOS
- Ads or behavioral analytics
- Automated online lending

## Delivery gate

No testing milestone enters `main` until the owner installs it on the real device, verifies existing data and every new workflow, reports regressions, receives fixes/retest, and explicitly approves merge.
