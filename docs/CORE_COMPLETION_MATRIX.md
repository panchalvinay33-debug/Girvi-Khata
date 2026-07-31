# Girvi Khata — Core Completion Matrix

Date: 2026-07-31
Branch: `agent/alpha24-production-hardening`
Status: Near-final core source; permanent signing identity is the release blocker.

## Complete code gates

- Customer, girvi, multi-item and payment workflows
- Atomic customer + girvi creation
- Verified custom payment split
- Settlement and reversal domain coverage
- Encrypted local snapshot with safety copies and quarantine
- PIN, biometric and local recovery foundation
- Portable encrypted business + master backup
- Restore preview and coordinated restore commit
- Restore storage headroom policy and tests
- Relational shadow database, incremental sync and dual-read verification
- Dedicated relational master links
- Persistent verified-write observation evidence
- Interrupted-write intent persisted before authoritative write
- Target fingerprint persisted before snapshot save
- Startup interrupted-write reconciliation
- Unknown interrupted state blocks new business writes
- Random/fallback signing disabled
- Unit tests run before strict certificate gate

## External release blocker

A permanent testing keystore must be provisioned through GitHub repository secrets. The private key must never be committed to the public repository.

Required secret names:

1. `GIRVI_TEST_KEYSTORE_BASE64`
2. `GIRVI_TEST_STORE_PASSWORD`
3. `GIRVI_TEST_KEY_PASSWORD`
4. `GIRVI_TEST_KEY_ALIAS`
5. `GIRVI_TEST_CERT_SHA256`

Until these are present and the expected certificate matches, CI must refuse to assemble or upload an APK.

## Final consolidated release gates

1. Permanent signing identity provisioned.
2. Final source unit tests and Security Guard green.
3. Signed APK assembly green.
4. APK package, version and certificate independently verified.
5. Artifact ZIP and APK archive integrity verified.
6. APK SHA-256 recorded.
7. One unified owner phone test.
8. Real-device verified-write observation period.
9. Explicit owner approval before merge.

## Current estimate

- Safe core source implementation: approximately 95% complete.
- Safe installable release readiness: approximately 86% complete.
- Full roadmap including PDF, thermal printing, media vault, Drive backup and relational cutover: approximately 76% complete.

## Deferred after first stable core release

- PDF receipts
- Bluetooth/thermal printer integration
- Encrypted photo/document vault
- Automatic Google Drive backup
- Relational source-of-truth cutover

## Governance

- `main` remains the owner-approved Alpha 21 base.
- `baseline/alpha21-owner-approved` remains the permanent emergency rollback source.
- Candidate branches remain immutable references.
- No secret, private key, customer data or `.gkb` file may enter the public repository.
- No merge occurs without explicit owner approval.
