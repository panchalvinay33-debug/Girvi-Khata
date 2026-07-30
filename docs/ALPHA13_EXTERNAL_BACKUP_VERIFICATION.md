# Alpha 13 — Externally Verified Backup Save

## Goal

Replace the Alpha 12 share-sheet-only backup completion signal with a direct Android document save whose written bytes are read back and cryptographically verified before the backup status is reset.

## Visible flow

1. Open internal Tools → Encrypted Backup.
2. Verify the existing six-digit app PIN.
3. Enter and confirm a valid recovery passphrase.
4. The app creates and internally decrypt/read-back verifies the portable `.gkb` package.
5. Android Files/Drive document picker opens with a timestamped filename.
6. The app writes the encrypted package to the selected document URI.
7. The app reads the same URI back with a bounded stream.
8. Written bytes must be byte-identical to the prepared package.
9. Read-back bytes must decrypt with the recovery phrase and preserve the expected schema and payload.
10. Only after all checks pass does Data Safety record the verified backup SHA/counts and reset `changes since backup`.

## Failure safety

The backup due state remains unchanged when:

- the picker is cancelled,
- the provider cannot open the destination,
- writing fails,
- the saved document is empty, truncated, oversized or changed,
- read-back fails,
- decrypt/authentication fails,
- schema or payload comparison fails.

The recovery phrase is held only in memory while the picker/save verification is pending and its character array is overwritten after success, failure, cancellation or activity destruction.

## Verification core

`ExternalBackupVerification` is a pure testable component. It verifies:

- non-empty prepared and written packages,
- exact package-byte equality,
- AES-GCM authenticated decrypt,
- expected schema version,
- exact snapshot payload equality,
- final package SHA-256 and size.

Unit tests cover success, changed byte, truncated write, wrong expected schema and wrong recovery phrase.

## Honest boundaries

- Same-document read-back proves that the selected Android document provider returned the exact bytes written by the app at verification time.
- It does not prove that a remote cloud provider later completed server synchronization, retention or cross-device availability.
- Google Drive app-data authorization, upload/read-back API verification, retention and restore discovery remain pending.
- Process death while the external picker is open may cancel the in-memory pending operation; no recovery phrase is persisted to resume it.
- Alpha 13 does not replace the interim encrypted snapshot store with the final transactional encrypted relational database.

## Release governance

Alpha 13 remains on `agent/initial-foundation`. It requires automated checks, signed testing artifact verification and owner physical-device testing before any approval or merge to `main`.
