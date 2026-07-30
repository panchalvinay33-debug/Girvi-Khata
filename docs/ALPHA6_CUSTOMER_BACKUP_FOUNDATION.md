# Alpha 6 — Customer Account Safety and Portable Encrypted Backup Foundation

Status: VERIFIED TESTING BUILD — OWNER TEST PENDING

## Owner-approved baseline

- Alpha 4 is owner approved.
- Alpha 5 reports APK remains owner-test pending.
- Alpha 6 remains on the development/testing branch and must not enter `main` without a separate APK test and owner approval.

## Customer account foundation

- Customer profile aggregates all active/released girvi, effective collections, and outstanding balance.
- Customer edits normalize name, mobile, and address.
- Name changes propagate to historical girvi display names while preserving customer IDs and accounting links.
- Duplicate normalized mobile numbers are blocked.
- Customers with any girvi history cannot be deleted.
- Only unused customers may be removed.

## Reporting date correctness

- Exact device-local day boundaries are generated with `java.time`.
- Today/7-day/30-day/custom-date ranges include the full final local day.
- Invalid reverse date ranges are rejected.
- All-time range remains explicitly bounded from epoch to `Long.MAX_VALUE`.

## Portable encrypted backup container

- Backup encryption is independent from the Android Keystore so a backup can survive reinstall or device loss.
- Owner recovery passphrase must contain at least 12 characters with letters and digits.
- PBKDF2-HMAC-SHA256 derives a 256-bit backup key with 310,000 iterations and a random 16-byte salt.
- AES-256-GCM encrypts the payload with a random 12-byte nonce and authenticated associated data.
- Envelope records format version, app schema version, creation time, KDF settings, salt, nonce, and encrypted payload length.
- Decrypt rejects wrong passphrases, modified ciphertext, malformed lengths, unsupported versions, trailing bytes, empty payloads, and oversized packages.
- Restored payload receives a SHA-256 checksum for later manifest/read-back comparison.
- Encryption keys and passphrases are never written to repository files or logs.

## Automated tests

- Backup encrypt/decrypt round trip.
- Random salt/nonce create different packages for the same payload.
- Wrong recovery passphrase rejection.
- Ciphertext tamper rejection.
- Weak passphrase rejection.
- Trailing-data rejection.
- Customer name propagation to girvi history.
- Duplicate mobile protection.
- Customer-history deletion block.
- Unused-customer deletion.
- Customer profile totals.

## Verified build

- Commit: `8f3077ceff30ce6fa7c45651ecd3905d3a10bb69`
- Android workflow run: `30418144206`
- Security Guard run: `30418144204`
- Artifact ID: `8710947731`
- Artifact archive digest: `sha256:aaad14bf9548a7271c1afa00874864da657dc8bb81a7c1026614839a62609ce1`
- APK SHA-256: `b971b4fc50199f9c62a5b96b9b0fe7b67db0bd0a1895ac8363d51f4435004916`
- APK size: `19,927,438 bytes`
- Unit tests, Android compilation, stable testing signing, APK packaging, artifact upload, and Security Guard passed.
- Downloaded APK archive integrity check reported no errors.

## Version

- Version code: 6
- Version name: `0.6.0-testing`
- Testing package and stable testing signature remain unchanged.

## Owner test focus

1. Install directly over Alpha 5 without uninstalling.
2. Confirm existing PIN, customer, girvi, payment, release, and report records remain.
3. Confirm both current launcher entries still open correctly.
4. Recheck customer/report totals for regressions.
5. Confirm no crash during PIN, girvi, payment, release, receipt share, statement share, or CSV share.

## Honest limitations

- Customer edit/delete operations are domain-complete and tested but not yet wired into visible profile edit buttons.
- Portable backup crypto is implemented and tested, but the current encrypted snapshot serializer is not yet exposed as an exportable byte stream in the UI.
- Google Drive upload, read-back verification, retention, and restore screens remain pending.
- Alpha 5 reports currently use a second launcher entry until the main navigation refactor is completed.
- Persistence remains the interim encrypted snapshot store rather than the final transaction-safe encrypted relational database.
