# Alpha 17 — Master-Assisted Workflow and Portable Masters

Status: implementation complete; final CI and APK verification in progress.

## Daily workflow scope

- PIN-protected `Master-Assisted Girvi & Payment` workflow inside internal Tools.
- New girvi entry reads active Item, Unit, Interest Plan and Locker entries from the Android-Keystore encrypted master catalog.
- Existing-customer search/reuse and duplicate matching remain active.
- Saved interest-plan basis points become the account monthly interest rate.
- Saved item name/category are used in the girvi item.
- Unit, locker and plan labels are persisted in the item description in a schema-compatible tagged form.
- Manual item/rate fallback remains available when a saved option is absent.
- Gross/deduction validation prevents negative values and deduction exceeding gross weight.
- Blank customer search no longer floods suggestions.
- Payment workflow reads active Payment Modes and writes the selected mode to the immutable payment ledger.
- Interest-first and principal-first allocation are available.
- Existing overpayment, invalid amount, released-girvi and receipt-number rules remain enforced by `GirviSettlementUseCase`.

## Portable backup and restore

- New `.gkb` payload is a versioned portable bundle containing:
  - complete business snapshot;
  - complete Item/Unit/Interest Plan/Payment Mode/Locker catalog.
- The combined payload is passphrase-encrypted by the existing portable backup crypto.
- Before the document picker opens, the app decrypts and compares both business and master data.
- After writing to Files/Drive, the same URI is read back, package SHA is verified, payload is decrypted and business/master counts are checked.
- Safety Status resets only after the full combined package passes verification.
- Restore preview displays whether portable masters are present and their count.
- New bundle restore writes and read-back verifies both encrypted stores.
- Legacy Alpha 8–16 snapshot-only `.gkb` files remain readable.
- Legacy restore replaces business records but preserves the current master catalog rather than resetting owner customization.
- Pre-restore app-private safety backup now contains both business records and masters.

## CI reliability

- Android Build and Security Guard retain pull-request triggers.
- Both also run directly on pushes to `agent/initial-foundation`, avoiding dependence on temporary pull-request merge-ref calculation.

## Compatibility decision

Alpha 17 does not force a business snapshot schema migration solely for master metadata. Unit, locker and plan display labels use the existing encrypted description field. The future transactional relational database migration will normalize them into dedicated foreign-key columns while preserving stable master IDs.

## Known remaining work

- Integrate saved choices directly into the original main-screen multi-item New Girvi dialog.
- Add multiple items to the dedicated master-assisted workflow; the original main workflow already supports multiple items.
- Add custom payment split to the master-assisted payment screen; the original payment screen retains custom allocation.
- Move unit, locker and plan references into dedicated relational columns.
- Add explicit master-catalog details to Data Safety Status and audit events.

## Owner physical-test focus

1. Install over Alpha 16 without uninstalling.
2. Verify existing PIN, customers, girvi, payments and Business Masters.
3. Open Master-Assisted Girvi & Payment with wrong and correct PIN.
4. Create a girvi using saved item, unit, plan and locker.
5. Test manual item/rate fallback.
6. Verify gross/deduction rejection and customer reuse.
7. Receive payment using a saved custom payment mode.
8. Disable a master and confirm it no longer appears.
9. Create a new `.gkb`; verify success message includes master count.
10. Restore-preview the new file and confirm master count.
11. On disposable data, restore and confirm business records plus masters return.
12. Preview a legacy Alpha 16 backup and confirm the UI says current masters will be preserved.
13. Regression-test Reports, Settlement, Safety Status, PIN Recovery and app restart persistence.
