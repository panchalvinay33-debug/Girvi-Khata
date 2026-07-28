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

**Decision:** Every interest result must have a reproducible period-wise breakup. Manual adjustments are separate ledger entries and never overwrite the automatic result.

**Reason:** The shopkeeper must be able to explain the calculation to the customer and audit it later.

## ADR-005 — Reversal instead of deletion

**Decision:** Posted payments and completed releases cannot be silently deleted. Corrections use linked reversal records.

**Reason:** Financial history must remain trustworthy.

## ADR-006 — Custom business masters

**Decision:** Categories, items, units, custom fields, payment modes, and interest plans are owner-configurable. No fixed gold/silver-only structure.

**Reason:** Different shopkeepers organize their business differently.

## ADR-007 — Documentation changes with the code

**Decision:** Every meaningful change updates `docs/PROGRESS.md`. New ideas or decisions update this file, and backup/security changes update their respective blueprints.

**Reason:** The repository itself must always show what is complete, pending, changed, and why.
