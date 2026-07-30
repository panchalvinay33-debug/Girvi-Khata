# Alpha 15 Verified Build Evidence

Status: VERIFIED TESTING BUILD — OWNER PHYSICAL TEST PENDING

- Source commit: `2f019c93ade9a70b657393722e6d9942353a0d36`
- Android workflow: `30517513379`
- Security Guard: `30517513383`
- Artifact ID: `8749518064`
- Package: `com.girvikhata.app.testing`
- Version: `15` / `0.15.0-testing`
- APK size: `20,157,014 bytes`
- APK SHA-256: `1b52f4825044bdbc398471a74d33d6e68baf9651b7403b91332b4b255d3ebaa1`

Verified scope:

- Internal PIN-protected Settlement & Release Center.
- Settlement preview for active and released girvi.
- Positive and negative manual interest adjustment.
- Non-zero amount, active-status and mandatory-reason validation.
- Cumulative adjustment stored in the encrypted business snapshot.
- Reason recorded as a separate encrypted, hash-chained audit event.
- Explicit audit event does not double-increment the backup-due counter.
- Shareable settlement/release receipt with item, payment, interest, adjustment and release metadata.
- Critical external-backup reminders in Settlement Center and Tools.
- New adjustment/receipt tests plus all existing accounting, reporting, backup, restore, recovery and journal tests passed.
- Android compilation, stable testing signing, APK upload, artifact ZIP integrity and APK integrity passed.

Owner test order:

1. Install directly over Alpha 14 without uninstalling.
2. Confirm existing PIN, fingerprint, records, Owner Settings, reports, backup and restore.
3. Open Tools → Settlement & Release Center; wrong PIN must fail and correct PIN must open.
4. Select disposable active girvi and test +₹100 adjustment with a reason.
5. Test -₹50 adjustment and confirm cumulative amount.
6. Confirm zero amount, short reason and released-girvi adjustment are rejected.
7. Open Data Safety Status; verify an interest-adjustment reason event and committed snapshot event.
8. Share an active settlement preview.
9. Share a released girvi receipt and verify release note/date.
10. Create a verified external `.gkb` after critical changes.
