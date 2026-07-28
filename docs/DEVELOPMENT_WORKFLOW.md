# Girvi Khata Development and Testing Workflow

This workflow is mandatory for every future feature, fix, security change, migration, backup change, and release.

## Golden rule

No new feature work is merged into `main` until the current testing APK has been installed and checked by the owner, reported issues are fixed, automated checks pass, and the owner confirms that the milestone is acceptable.

## Permanent cycle

1. Start from the latest approved `main` branch.
2. Create one focused feature or milestone branch.
3. Implement the planned work only on that branch.
4. Update all affected governance documents in the same branch:
   - `docs/ROADMAP.md`
   - `docs/BACKUP_BLUEPRINT.md` when storage, encryption, recovery, data format, or backup behavior changes
   - `docs/PROGRESS.md`
   - `docs/DECISIONS.md` for every new product or architecture decision
   - `docs/TESTING_RELEASES.md` for every testing APK
5. Run automated unit tests, Android build, and Security Guard.
6. Produce a versioned testing APK.
7. Record the APK version, commit SHA, workflow run, checksum, scope, and known limitations.
8. Owner installs and tests that milestone separately.
9. Record every reported bug, usability issue, missing behavior, and new idea.
10. Fix the current milestone before starting unrelated new work.
11. Rebuild and retest when fixes materially change behavior.
12. Merge into `main` only after owner approval and all required checks pass.
13. Tag or otherwise record the approved milestone baseline.
14. Create the next feature branch from the newly updated `main` branch.

## Branch discipline

- `main` is the last owner-approved and tested baseline.
- Development branches contain unfinished or unapproved work.
- One branch should represent one clear milestone whenever practical.
- Do not stack multiple untested milestones into `main`.
- Do not continue major unrelated work on top of a milestone that the owner has not tested.

## Testing APK rule

Each testing APK must represent a clear checkpoint. It must include:

- Version name and version code.
- Source commit SHA.
- Passed automated checks.
- Exact features to test.
- Known missing features and limitations.
- Upgrade or clean-install instruction when relevant.
- APK checksum.

## Merge gate

A milestone can be merged only when all are true:

- Android unit tests pass.
- Debug APK build passes.
- Security Guard passes.
- APK artifact is verified.
- Owner has tested the milestone.
- Reported blocker and critical issues are fixed.
- Roadmap, progress, decision, backup, and testing ledgers are current.
- No secret, production customer data, signing key, OAuth credential, database, photo, or backup file is committed.
- Owner explicitly approves moving the tested milestone into `main`.

## Blueprint and backup update rule

Any change affecting data structure, local storage, encryption, PIN or biometric security, file format, Google account handling, backup scheduling, backup retention, restore, migration, exports, or recovery must update `docs/BACKUP_BLUEPRINT.md` in the same branch and must include a compatibility note.

## New idea rule

Every useful new idea is first recorded in `docs/DECISIONS.md` or the relevant roadmap phase. It is not silently mixed into the current milestone if doing so would make testing unclear. Large ideas become a future milestone; small safe improvements may be included only when their test case is added to the current testing checklist.

## Failure handling

- A failed test is not hidden or marked complete.
- A failed APK build blocks release.
- Data-loss, incorrect-interest, incorrect-payment, recovery, encryption, or authentication bugs are release blockers.
- Fixes must preserve an audit trail in commits and progress notes.

## Current baseline note

The first approved testing checkpoint is `v0.1.0-alpha.1`. Work after that checkpoint remains on a development branch until its separate APK is tested and approved.
