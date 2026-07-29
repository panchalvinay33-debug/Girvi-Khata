# Alpha 9 — Customer Profiles and Custom Collection Dates

## Status

Verified testing build; owner physical-device test pending. Development remains outside `main`.

## Build identity

- Source commit: `9d31f06c7a5517da3f3afbbb9b1f5435e2c6c9bd`
- Android workflow: `30480566880`
- Security Guard: `30480566699`
- Artifact ID: `8735582885`
- Version: `0.9.0-testing` (code 9)
- Package: `com.girvikhata.app.testing`
- APK size: `20,025,854 bytes`
- APK SHA-256: `698dda752567b1f4f3e28500d95c8c3e21f7e2bd61ed09bb818a15b7145eeb17`

## Visible work

### Customer profile and editing

- Customer Khata detail now displays saved mobile and address.
- Owner can edit customer name, mobile and address.
- Input is normalized by the existing pure-domain customer operation.
- Duplicate normalized mobile numbers are rejected.
- Name changes propagate to linked girvi display names without changing customer IDs or accounting links.
- Updated snapshots are written back through the encrypted local store.

### Safe customer deletion

- Delete is enabled only when the customer has no active or released girvi history.
- Any customer referenced by a girvi remains protected.
- Deletion requires an explicit confirmation dialog.

### Custom collection dates

- Collection Reports add an Android date picker for From and To dates.
- From uses device-local start-of-day.
- To includes the complete final selected day through 23:59:59.999.
- Today, seven-day, thirty-day and all-time presets remain available.
- Custom range output supports the existing reversal-aware totals, CSV share and receipt share.

## Accounting and backup impact

- No schema change; encrypted snapshot schema remains v3.
- Customer edits are included automatically in future portable backups.
- Linked girvi customer display names are updated to keep statements and reports consistent.
- Payment, reversal, release and allocation records are not modified by customer profile editing.

## Automated verification

- Existing customer-operation tests cover trimming/normalization, duplicate mobile rejection, linked-name propagation, history deletion protection and unused-customer deletion.
- Existing reporting tests cover effective collection rows and date boundaries.
- Full unit-test suite passed on the Alpha 9 source commit.
- Compose and Android compilation passed.
- Stable testing signing, artifact upload and Security Guard passed.
- Artifact ZIP and APK archive integrity passed after download.

## Physical test focus

- In-place upgrade and PIN/data retention.
- Alpha 8 PIN recovery and restore regression check.
- Customer edit persistence after closing and reopening.
- Duplicate-mobile rejection.
- Girvi-history delete protection and unused-customer deletion.
- Custom date boundaries, especially receipts on the final selected day.
- CSV output, scrolling, keyboard overlap and dialog sizing.

## Known limitations

- Main and Tools remain two launcher entries.
- App-wide screenshot/recent-app privacy blocking is pending.
- Final transactional encrypted database and explicit corrupt-store recovery are pending.
- Automatic Google Drive backup remains pending.
- No merge to `main` without explicit owner approval.
