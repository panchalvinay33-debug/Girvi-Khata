# Girvi Khata — Current Project State

Last updated: 2026-07-30

This file is the quickest authoritative summary. Detailed approval, rollback, milestone and release evidence is stored in the linked documents.

## Approved baseline and branch policy

- Owner-tested and explicitly approved base: `v0.21.0-alpha.21`.
- Exact tested APK source: `eec78e0aba6a8d168baeb09959fe93e2fd85733f`.
- Permanent rollback branch: `baseline/alpha21-owner-approved`.
- PR #1 is authorized for merge into `main` after the approval documentation is committed.
- Future work must start from the merged Alpha 21 main baseline on a new development branch.
- Future milestones still require CI, a separately versioned APK, physical testing and explicit owner approval before merge.
- Repository is public; never commit OAuth credentials, production signing material, real customer records, recovery phrases or `.gkb` files.

## Approved APK evidence

- Package: `com.girvikhata.app.testing`
- Version code/name: `21` / `0.21.0-testing`
- Android workflow: `30543127987`
- Security Guard: `30543128181`
- Artifact ID: `8759651865`
- APK size: `20,402,894 bytes`
- APK SHA-256: `65a5f56771c120b9f11e102e0eeaea7c086544c7a381a781879cf9c43ebead12`
- Artifact digest: `584432831011b866850bc6db62c65a1d2f884907a7cabeca16a2ccf0890c7157`
- Owner status: physically tested and approved as the new stable project base.

## Approved visible application scope

### Main business work

- Six-digit PIN, weak-PIN protection, progressive lockout, biometric unlock and authenticated PIN recovery.
- Configurable immediate/30-second/1-minute/5-minute background auto-lock.
- Customers, categories, duplicate-mobile safeguards and customer khata.
- Classic master-assisted and advanced multi-item girvi creation.
- Item, Unit, Interest Plan, Payment Mode and Locker masters.
- Interest preview, manual adjustment with mandatory reason, settlement and release.
- Immutable payment history, linked reversals and interest-first/principal-first/exact custom allocation.
- Customer/portfolio/collection reports, exact date filters and CSV/text sharing.
- Settlement and release receipts.

### Internal Tools

- Data Safety Status with encrypted hash-chained journal and backup-due state.
- Database Migration Status with incremental sync, dual-read verification, rollback simulation and benchmark diagnostics.
- Owner Settings and Business Masters.
- Advanced Multi-Item & Custom Split workflow plus classic fallback.
- Settlement & Release Center.
- Reports & Customer Khata.
- Portable encrypted `.gkb` backup containing business records and master catalog.
- Android Files/Drive direct save with same-URI byte, SHA, decrypt and payload verification.
- Strict restore preview, legacy-backup compatibility and combined pre-restore safety package.
- Data-preserving PIN recovery and explicit local corruption recovery.
- `MainActivity` is the only exported launcher activity.

## Approved storage and security architecture

- The authoritative store remains an app-private Android-Keystore AES-256-GCM encrypted snapshot, schema v3.
- Saves validate relationships, rotate encrypted safety copies, fsync temporary files and decrypt/read back before and after replacement.
- Latest five safety copies and latest two quarantine copies are retained.
- A separate AES-256-GCM encrypted journal keeps up to 500 SHA-256 hash-chained events.
- Portable backup uses AES-256-GCM and PBKDF2-HMAC-SHA256 at 310,000 iterations.
- Android automatic backup/device transfer remains disabled.
- App-wide `FLAG_SECURE` protection is applied where supported.
- No developer master key, central customer database, analytics or admin backdoor exists.

## Approved relational migration state

- SQLite shadow tables exist for customers, categories, girvis, items, payments and metadata.
- Foreign keys, uniqueness constraints, indexes and write-ahead logging are enabled.
- Sensitive text cells are individually AES-256-GCM encrypted with field-specific associated data.
- Snapshot commits trigger incremental relational synchronization; only affected customer/category/girvi subtrees are rewritten.
- Every sync is followed by full row-count and decrypted semantic-fingerprint verification.
- Device diagnostics can inject a pre-commit failure and prove transaction rollback without modifying the authoritative snapshot.
- Device benchmark and free-space headroom checks are available.
- Item/Unit/Locker master-link resolution foundation exists, but dedicated relational columns are not yet approved.
- The encrypted snapshot remains the sole source-of-truth. Relational read/write cutover is explicitly blocked pending a separate owner approval.

## Rollback contract

- Exact code rollback target: `baseline/alpha21-owner-approved`.
- Never delete or force-move this branch.
- Do not uninstall the app during recovery.
- Because Android blocks version-code downgrade, a future recovery APK must use the Alpha 21 source with a version code higher than the failing installed APK and the same stable testing signature.
- Protect data first with an externally verified `.gkb`; prefer a forward repair or revert PR instead of rewriting shared history.
- Full instructions: `docs/ROLLBACK_RUNBOOK.md`.
- Approval evidence: `docs/OWNER_APPROVAL_ALPHA21_BASELINE.md`.

## Known limitations retained at this base

- SQLite is a verified shadow, not the source-of-truth.
- SQLite is not whole-file SQLCipher encrypted; sensitive text cells are individually encrypted.
- Unit/Locker/Interest Plan/Payment Mode dedicated relational columns remain pending.
- Controlled completely-full-storage and process-kill tests remain pending.
- Same-URI document read-back cannot prove later remote cloud synchronization.
- Google Drive API automation, PDF/thermal printing, encrypted media vault, production signing and pilot rollout remain pending.

## Next priority order after the baseline merge

1. Create a fresh development branch from updated `main`.
2. Normalize dedicated relational master IDs with explicit schema migration and rollback tests.
3. Add relational business write APIs while keeping snapshot fallback.
4. Run longer dual-read observation and controlled low-storage/process-kill testing.
5. Seek separate owner approval before any relational read/write cutover.
6. Make the repository private before Drive OAuth or production-signing work.
7. Add PDF/thermal printing and encrypted media vault after storage cutover safety is proven.

## Permanent delivery rule

`approved main base → new development branch → automated checks → separately versioned APK → owner physical test → fixes/retest → explicit owner approval → merge to main`
