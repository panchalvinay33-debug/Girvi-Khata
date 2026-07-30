# Alpha 17 — Master-Assisted Daily Workflow

Status: implementation and CI verification in progress.

## Scope

- PIN-protected `Master-Assisted Girvi & Payment` workflow inside internal Tools.
- New girvi entry reads active Item, Unit, Interest Plan and Locker entries from the Android-Keystore encrypted master catalog.
- Existing customer search/reuse and duplicate matching remain active.
- Saved interest-plan basis points become the account monthly interest rate.
- Saved item name/category are used in the girvi item.
- Unit, locker and plan labels are persisted in the item description in a schema-compatible tagged form.
- Manual item/rate fallback remains available when an active saved option is absent.
- Payment workflow reads active saved Payment Modes and writes the selected mode to the immutable payment ledger.
- Interest-first and principal-first allocation are available.
- Existing overpayment, invalid amount, inactive/released girvi and receipt-number rules remain enforced through `GirviSettlementUseCase`.

## Compatibility decision

Alpha 17 does not force a business snapshot schema migration solely for master metadata. Unit, locker and plan display labels are stored in the existing encrypted description field. The future transactional relational database migration will normalize them into dedicated foreign-key columns.

## Known remaining work

- Integrate saved choices directly into the original main-screen `New Girvi` dialog instead of using the internal master-assisted workflow activity.
- Add multiple items per transaction to the master-assisted workflow; the original main workflow already supports multiple items.
- Include the separate encrypted master catalog in portable `.gkb` backup and restore.
- Preserve stable master IDs as dedicated columns during relational database migration.
- Add custom payment split in the master-assisted payment screen; original payment screen retains custom allocation.

## Owner physical-test focus

1. Install over Alpha 16 without uninstalling.
2. Verify existing PIN, customers, girvi and payments.
3. Add/enable masters in Business Masters.
4. Open Master-Assisted Girvi & Payment with correct and wrong PIN.
5. Create a girvi using saved item, unit, plan and locker.
6. Reopen the girvi and verify tagged metadata and exact interest rate.
7. Receive payment using a saved custom payment mode.
8. Verify receipt number, allocation, reports, Safety Status and persistence after restart.
9. Disable a master and confirm it no longer appears in the workflow.
10. Create an external verified backup after testing.
