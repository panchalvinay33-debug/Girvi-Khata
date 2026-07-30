# Alpha 16 — Encrypted Business Master Catalog

Status: implementation complete; CI and owner physical test required.

## Scope

- PIN-protected Business Masters entry inside the internal Tools hub.
- Separate Android-Keystore AES-GCM encrypted catalog file; no plaintext SharedPreferences master data.
- Verified temporary write and final read-back before catalog replacement.
- Item Master with optional category scoping.
- Unit Master.
- Monthly Interest Plan Master with validated rate basis points.
- Payment Mode Master.
- Locker / Storage Master.
- Add, rename, activate/deactivate and same-kind up/down ordering.
- Normalized whitespace and case-insensitive duplicate protection.
- Operational defaults for units, payment modes, one interest plan and main locker.
- Pure domain tests for duplicate handling, category scoping, rename/toggle identity, ordering boundaries and defaults.

## Security and migration boundary

The catalog is intentionally separate from the current business snapshot so Alpha 16 does not rewrite customer, girvi, payment or portable-backup schema. The catalog is device-encrypted and app-private. Existing `.gkb` backups continue to restore business records as before.

## Honest limitation

Alpha 16 creates and manages the encrypted catalog. The current New Girvi and Payment dialogs still use their existing manual/default controls. Full selection wiring, portable backup inclusion and restore migration for master catalog are the next milestone; they are not claimed complete in Alpha 16.

## Owner test

1. Install over Alpha 15 without uninstalling.
2. Open Tools → Business Masters and verify PIN gate.
3. Add item, unit, interest plan, payment mode and locker entries.
4. Confirm blank and duplicate names reject.
5. Disable/enable entries and restart the app.
6. Rename and reorder entries; restart and verify persistence.
7. Verify existing customers, girvi, payments, reports, backup and restore are unchanged.
