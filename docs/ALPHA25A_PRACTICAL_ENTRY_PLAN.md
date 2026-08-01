# Alpha 25A — Practical Bilingual Entry Upgrade

Status: In progress
Issue: #13
Base: `agent/alpha24-production-hardening`
Working branch: `agent/alpha25a-practical-entry`

## Product promise

A shopkeeper can create a complete pledge entry quickly in Hindi, English or mixed text, while retaining control over imported contact data, optional live photos, weight details and back-dated pledge dates.

## Locked decisions

- WhatsApp authentication, backup and recovery are excluded.
- Existing PIN, biometric, encrypted local storage and verified-write architecture remain authoritative.
- Contact import is a convenience only; imported customer name and mobile remain editable.
- Customer and pledged-item photos are optional and camera-only.
- Permission denial must never block manual data entry.
- Existing stored snapshots must remain readable.

## Increment plan

### 25A.1 Foundation

1. Add backward-compatible model fields for customer photo reference, item photo reference, weight unit and pledge date.
2. Add entry validation that accepts Unicode Hindi, English and mixed-script text.
3. Add Android contact and camera permission/capability boundaries.
4. Add tests for old-snapshot compatibility and bilingual values.

### 25A.2 Customer entry

1. Add contact picker next to customer name/mobile.
2. Populate both fields from the selected contact.
3. Keep both fields editable after import.
4. Add optional live customer photo with retake/remove actions.
5. Warn on likely duplicates without silently merging records.

### 25A.3 Item and weight entry

1. Add optional live item photo.
2. Add simple weight and unit controls.
3. Keep gross, deduction and calculated net weight under an expandable advanced section.
4. Support grams, milligrams, kilograms, tola, ratti, pieces and custom unit.

### 25A.4 Date and review

1. Open calendar directly from the date field.
2. Allow back-dated pledge entries.
3. Guard future dates with a clear message.
4. Show a final Hindi/English review card before verified save.

## Acceptance criteria

- Hindi, English and mixed-script values round-trip through encrypted storage.
- Selecting a contact fills name and mobile and both can be changed before save.
- Manual entry works without Contacts permission.
- Manual entry works without Camera permission.
- Photos do not appear in the public gallery.
- Back dates save correctly and future dates are rejected or explicitly guarded.
- Existing Alpha 24 customer, girvi, payment, settlement, backup and restore tests remain green.
- A signed testing APK is promoted only after CI and owner-phone checks pass.

## Later phases

- Alpha 25B: monthly percentage, flat monthly, per-day and compound interest engine.
- Alpha 25C: additional advances and two-column ledger.
- Alpha 25D: detailed settlement, interest-only and partial-payment redesign.
