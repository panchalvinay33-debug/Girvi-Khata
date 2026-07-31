# Alpha 24 Core Completion Matrix

Date: 2026-07-31
Branch: `agent/alpha24-production-hardening`
Base candidate: `candidate/alpha23-testing`
Approved rollback: Alpha 21

## Completed in code

- Classic new girvi and payment writes use `VerifiedBusinessWriteCoordinator`.
- Advanced Multi-Item customer + girvi write is atomic and verified.
- Advanced Custom Split payment uses isolated verified append.
- Restore business replacement uses verified coordinated write.
- Restore requires a verified pre-restore safety copy where a healthy primary exists.
- Restore enforces deterministic storage headroom.
- Relational shadow dual-read verification remains mandatory after coordinated writes.
- Persistent verified-write observation count and fingerprint proof exist.
- Pending/committed/failed write intent survives process death.
- Database Migration Status shows write observation proof and cutover blockers.
- Random/fallback APK signing is forbidden.
- Keystore and final APK certificate fingerprints are pinned by CI.

## Automated gates

- Accounting and interest calculations.
- Payment allocation, reversals and settlement.
- Multi-item and custom-split validation.
- Backup encryption, bundle codecs and restore preview.
- Record-store corruption and safety-copy policies.
- Relational delta, dual-read and stress logic.
- Restore storage policy boundaries.
- Interrupted-write intent state transitions.
- Verified-write observation and cutover policy.

## External release blockers

1. Provision permanent repository signing secrets.
2. Pin the selected certificate SHA-256.
3. Produce one signed consolidated APK.
4. Independently verify package, version, certificate and APK SHA-256.
5. Run one consolidated owner phone test.
6. Observe at least 25 successful coordinated writes before any relational cutover discussion.
7. Move production work to a private repository before adding OAuth or production credentials.

## Modules that may follow the first stable core release

- PDF receipts.
- Bluetooth/thermal printing.
- Encrypted media/document vault.
- Automatic Google Drive backup.
- Relational database source-of-truth cutover.

## Governance

- `main` remains owner-approved Alpha 21 until explicit later approval.
- Candidate and baseline branches are immutable references.
- No APK is promoted without exact certificate and source evidence.
- No secrets, private keys, passphrases or customer data enter the public repository.
