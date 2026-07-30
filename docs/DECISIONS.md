# Product and Architecture Decision Log

## ADR-001 — Single-owner, single-primary-device first release

**Decision:** The first stable release supports one owner and one primary Android device.

**Reason:** This keeps privacy, synchronization, conflict handling, recovery, and maintenance understandable and reliable.

## ADR-002 — No central business-record database

**Decision:** Customer, girvi, payment, item, and media records will not be stored in a company-controlled database.

**Reason:** The app's main market promise is owner-controlled data.

## ADR-003 — Google Drive is backup storage, not a live database

**Decision:** The app works from an offline local database. Drive stores versioned encrypted backup packages.

**Reason:** Using Drive as a live database would create conflict, corruption, performance, and authorization risks.

## ADR-004 — Transparent calculations

**Decision:** Every interest result must have a reproducible period-wise breakup. Manual adjustments remain separate and never overwrite the automatic result.

**Reason:** The shopkeeper must be able to explain and audit the calculation.

## ADR-005 — Reversal instead of deletion

**Decision:** Posted payments and completed releases cannot be silently deleted. Corrections use linked reversal records.

**Reason:** Financial history must remain trustworthy.

## ADR-006 — Custom business masters

**Decision:** Categories, items, units, custom fields, payment modes, and interest plans are owner-configurable. No fixed gold/silver-only structure.

**Reason:** Different shopkeepers organize their businesses differently.

## ADR-007 — Documentation changes with code

**Decision:** Every meaningful change updates the current-state and progress records. Product, security, storage, or backup decisions update their dedicated documents.

**Reason:** The repository must always show what is complete, pending, changed, tested, and why.

## ADR-008 — PIN is an unlock factor, not the backup encryption key

**Decision:** The six-digit PIN is salted and stretched for local verification. It never directly encrypts the database or portable/cloud backup.

**Reason:** Six digits do not provide enough entropy for long-term backup encryption.

## ADR-009 — Authenticated encryption only

**Decision:** Device and portable payloads use AES-256-GCM with unique random nonces/IVs and authenticated associated data where applicable.

**Reason:** Confidentiality without integrity is insufficient for financial records and recovery material.

## ADR-010 — Exact money arithmetic

**Decision:** All monetary and percentage calculations use integer paise or `BigDecimal`; floating-point money arithmetic is prohibited.

**Reason:** Binary floating point can create silent rounding errors.

## ADR-011 — Domain engine independent from UI and storage

**Decision:** Interest, payment allocation, validation, numbering, reporting, and financial invariants remain testable Kotlin modules independent from Compose and persistence.

**Reason:** The same rules must drive screens, receipts, reports, and restored records.

## ADR-012 — Backup success requires verification

**Decision:** A future cloud backup becomes `VERIFIED` only after creation, encryption, upload, read-back, authenticated decryption checks, hash checks, and manifest/count validation.

**Reason:** A successful upload request does not prove that recovery will work.

## ADR-013 — Restore is preview-first and fail-closed

**Decision:** Portable restore must decrypt and validate the complete package before touching current records. The owner sees counts/checksum metadata and must explicitly confirm replacement.

**Reason:** Wrong passphrases, corrupt files, incompatible schemas, or broken relationships must never partially overwrite a working ledger.

## ADR-014 — Pre-restore safety copy and post-save verification

**Decision:** Before a destructive restore, the current snapshot is encrypted into app-private safety storage. After saving imported records, the app reloads them and verifies customer, girvi, and immutable-ledger counts.

**Reason:** A successful write call alone is not sufficient proof that replacement completed correctly.

## ADR-015 — PIN recovery must preserve business data

**Decision:** When an old PIN is unusable, the owner may reset only the PIN verifier and lockout state after strong biometric or device-credential authentication. Business records are not cleared as part of PIN recovery.

**Reason:** Security-state failure must not force deletion of the owner’s financial history.

## ADR-016 — Physical regression overrides automated confidence

**Decision:** A CI-green APK is not owner-approved. Any physical-device regression, including Alpha 7’s old-PIN failure, supersedes the milestone until a corrected build is physically tested.

**Reason:** Automated tests cannot fully reproduce Android upgrade state, hardware authentication, OEM behavior, or real retained app data.

## ADR-017 — Interim snapshot storage is not production persistence

**Decision:** The current app-private encrypted snapshot file may support testing milestones, but stable production requires transaction-safe encrypted relational storage and explicit corrupt-store recovery.

**Reason:** A single snapshot file cannot provide the same transactional guarantees, query scalability, and granular recovery behavior as the final database design.
